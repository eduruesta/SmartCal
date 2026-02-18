### Koog Calendar Agent — Frontend (Kotlin + Compose Multiplatform) Guidelines

This document describes how to move the Koog calendar agent from a server/API call pattern to a fully in‑app agent embedded in a Kotlin + Compose Multiplatform (KMP) application. It mirrors the behavior you currently have in:
- `post("/messages")` (in `BackendMain.kt`)
- `UserSessionManager` (session + OAuth + MCP lifecycle)
- `initializeUserAgent` and `createSimpleCalendarStrategy` (agent setup + loop + tools)

Where MCP can run in-app: Desktop (Compose for Desktop, JVM). For Android/iOS/Web, implement tools via direct Google REST, or connect to a remote MCP over WebSocket.

---

### High-level Goals
- Eliminate the server `/messages` hop for chat → lower latency and fewer timeouts.
- Keep the same system rules (time/tool-first), tool naming, and safe calendar operations.
- Preserve per-user context across messages on the client.

---

### Architecture Options (KMP)
1) Fully In‑App Agent (Recommended where feasible)
- Agent loop in shared Kotlin (`commonMain`) using Ktor Client to call your LLM provider.
- Tools in shared Kotlin: reuse `TimeTools.kt`; implement Google Calendar REST via Ktor and OAuth PKCE tokens.
- Works best on Android and Desktop; iOS/Web are supported with platform-specific OAuth details.

2) In‑App Agent + Minimal Edge Token Proxy (Best practice for secrets)
- Agent still runs in the app.
- A tiny edge endpoint mints short‑lived, scoped tokens for LLM calls (no provider secrets in app).
- Google OAuth remains client‑side.

3) Optional MCP Reuse
- Desktop: bundle and spawn the Node MCP server (stdio/WS) and connect locally.
- Mobile/Web: connect to a remote MCP over WebSocket if you must keep MCP; otherwise prefer direct REST for lowest latency.

---

### Mapping Server Components → In‑App Equivalents
- Server `post("/messages")` → `FrontendMessageService.send()` (in-app):
  1) Validate session (local session manager)
  2) Initialize/reuse per-user agent context (persist across calls)
  3) (Optional) Consume one credit
  4) Save user message (local history)
  5) Run agent (LLM + tools)
  6) Save assistant message (local history)
  7) Return `MessageResponse` (same DTO shape)

- `UserSessionManager` → `FrontendSessionManager`:
  - PKCE login per platform, secure token storage, access token refresh
  - Desktop-only: spawn/stop MCP process

- `initializeUserAgent` → `AgentFactory.initializeForUser`:
  - Build system prompt with user variables (name, email, plan, timezone)
  - Register tools (TimeTools + Calendar tools)
  - Create agent loop with same iteration/guardrails

- `createSimpleCalendarStrategy` → `FrontendAgent` loop with the same 5-tool-call guardrail messages and single-turn response by default.

---

### System Prompt (copy from BackendMain.kt)
Use the same text block (BackendMain.kt ~3245–3265) and inject runtime values:
- `userName`, `userEmail`, `timezone` (e.g., `TimeZone.currentSystemDefault().id`), `subscriptionPlan`.

Key rules to keep verbatim:
- TOOL-FIRST FOR TIME: Never guess dates/times; call time tools first (`getEventTimes`, named ranges, `getDateDayOfWeek`, etc.).
- Listing: prefer named ranges (today/week/next week) → then `list-events` with `timeMin/timeMax`.
- Cross-calendar search: use `query` without `calendarId`.
- Deletions: list → confirm with user → `delete-event`.

---

### Security & Auth
- LLM provider secrets: Do not ship in the app. Use an edge token proxy issuing short‑lived tokens or a gateway that enforces model/param scopes + RPS limits.
- Google OAuth 2.0 PKCE per platform:
  - Android: AppAuth + EncryptedSharedPreferences/Keystore
  - iOS: ASWebAuthenticationSession + Keychain
  - Desktop: system browser + localhost redirect, encrypted local vault or OS keychain
  - Web/WASM: OAuth 2.1 PKCE via JS interop; keep tokens in memory or IndexedDB
- Scopes: `https://www.googleapis.com/auth/calendar` (and `contacts.readonly` if needed later)

---

### Tool Layer (Shared Kotlin)
1) Time tools
- Reuse `TimeTools.kt`. Ensure only `kotlinx-datetime`/KMP-friendly APIs are used.
- Preferred helper: `getEventTimes(hour, minute, dayOffset, durationMinutes)`

2) Google Calendar tools (REST)
- Implement a `CalendarTools` interface (list/create/update/delete/listCalendars) using Ktor with the user’s Google access token.
- Enforce the same behavioral rules as in the prompt (named ranges, cross-calendar search by `query`, safe deletions).

3) MCP (Desktop only)
- Spawn Node MCP (`mcp-google-calendar`) and connect over stdio/WS using your existing `McpToolRegistryProvider` adapter.
- Elsewhere (mobile/web), use the REST tools instead.

---

### Agent Loop (Shared Kotlin)
- Recreate the `SimpleCalendarStrategy` behavior:
  - LLM → optional tool calls → apply tool results → loop
  - After >5 tool calls: inject the same guidance messages (ask for specificity, never guess dates) and stop escalating.
- Provide streaming and cancellation using Kotlin coroutines (`Flow`, `Job.cancel()`).

---

### In‑App Replacement for `/messages`
Data models (mirror server DTOs):
```kotlin
@Serializable
data class MessageRequest(
    val message: String,
    val conversationId: String? = null
)

@Serializable
data class UserBasicInfo(
    val email: String? = null,
    val firstName: String? = null,
    val fullName: String? = null,
)

@Serializable
data class MessageResponse(
    val message: String,
    val conversationId: String? = null,
    val timestamp: String = kotlinx.datetime.Clock.System.now().toString(),
    val userInfo: UserBasicInfo? = null,
    val success: Boolean = true,
    val creditsRemaining: Int? = null,
    val subscriptionPlan: String? = null,
)
```

Session manager (frontend):
```kotlin
class FrontendSessionManager(
    private val secureStore: SecureStore,       // expect/actual per platform
    private val oauth: GoogleOAuthClient,       // expect/actual per platform
    private val desktopMcp: DesktopMcpController? = null // Desktop only
) {
    private val session = MutableStateFlow<AppSession?>(null)

    suspend fun login(email: String, redirectUri: String): Result<AppSession> {
        val tokens = oauth.login(email, redirectUri).getOrElse { return Result.failure(it) }
        secureStore.saveTokens(email, tokens)
        val s = AppSession(
            sessionId = randomId(),
            sessionToken = randomToken(),
            email = email,
            createdAt = Clock.System.now(),
            expiresAt = Clock.System.now().plus(DateTimePeriod(days = 30), TimeZone.UTC),
            isActive = true
        )
        session.value = s
        desktopMcp?.startForUser(s, tokens) // Desktop only
        return Result.success(s)
    }

    fun currentSession(): AppSession? = session.value

    suspend fun getGoogleAccessToken(): String? {
        val email = session.value?.email ?: return null
        val tokens = secureStore.getTokens(email) ?: return null
        val valid = oauth.ensureValidAccessToken(tokens)
        secureStore.saveTokens(email, valid)
        return valid.accessToken
    }

    fun logout() {
        session.value?.let { s -> desktopMcp?.stopForSession(s.sessionId) }
        session.value?.email?.let { secureStore.deleteTokens(it) }
        session.value = null
    }
}
```

Calendar tools via REST (Ktor):
```kotlin
interface CalendarTools {
    suspend fun listEvents(calendarId: String? = null, timeMin: String? = null, timeMax: String? = null, query: String? = null): EventsList
    suspend fun createEvent(req: CreateEventRequest): GoogleEvent
    suspend fun updateEvent(eventId: String, req: UpdateEventRequest): GoogleEvent
    suspend fun deleteEvent(calendarId: String, eventId: String): Boolean
    suspend fun listCalendars(): CalendarList
}

class GoogleRestCalendarTools(
    private val ktor: io.ktor.client.HttpClient,
    private val accessTokenProvider: suspend () -> String?
) : CalendarTools {
    private suspend fun token() = accessTokenProvider() ?: error("No Google token")

    override suspend fun listEvents(calendarId: String?, timeMin: String?, timeMax: String?, query: String?): EventsList {
        val calId = calendarId ?: "primary"
        val params = ParametersBuilder().apply {
            append("singleEvents", "true")
            append("orderBy", "startTime")
            timeMin?.let { append("timeMin", it) }
            timeMax?.let { append("timeMax", it) }
            query?.let { append("q", it) }
        }.build()
        val url = "https://www.googleapis.com/calendar/v3/calendars/${'$'}{encode(calId)}/events?${'$'}params"
        return ktor.get(url) { header(HttpHeaders.Authorization, "Bearer ${'$'}{token()}") }.body()
    }

    override suspend fun createEvent(req: CreateEventRequest): GoogleEvent {
        val url = "https://www.googleapis.com/calendar/v3/calendars/${'$'}{encode(req.calendarId)}/events"
        return ktor.post(url) {
            header(HttpHeaders.Authorization, "Bearer ${'$'}{token()}")
            contentType(ContentType.Application.Json)
            setBody(req.toGoogleBody())
        }.body()
    }

    override suspend fun updateEvent(eventId: String, req: UpdateEventRequest): GoogleEvent {
        val url = "https://www.googleapis.com/calendar/v3/calendars/${'$'}{encode(req.calendarId)}/events/${'$'}{encode(eventId)}"
        return ktor.request(url) {
            method = HttpMethod.Patch
            header(HttpHeaders.Authorization, "Bearer ${'$'}{token()}")
            contentType(ContentType.Application.Json)
            setBody(req.toGoogleBody())
        }.body()
    }

    override suspend fun deleteEvent(calendarId: String, eventId: String): Boolean {
        val url = "https://www.googleapis.com/calendar/v3/calendars/${'$'}{encode(calendarId)}/events/${'$'}{encode(eventId)}"
        val res = ktor.delete(url) { header(HttpHeaders.Authorization, "Bearer ${'$'}{token()}") }
        return res.status.isSuccess()
    }

    override suspend fun listCalendars(): CalendarList {
        val url = "https://www.googleapis.com/calendar/v3/users/me/calendarList"
        return ktor.get(url) { header(HttpHeaders.Authorization, "Bearer ${'$'}{token()}") }.body()
    }
}
```

Agent loop (mirrors `SimpleCalendarStrategy`):
```kotlin
data class LlmToolCall(val name: String, val args: JsonObject)

data class LlmStep(val content: String, val toolCalls: List<LlmToolCall> = emptyList())

interface LlmClient { suspend fun complete(messages: List<Message>): LlmStep }

class FrontendAgent(
    private val llm: LlmClient,
    private val timeTools: TimeTools,
    private val calendar: CalendarTools,
    private val maxToolCalls: Int = 5,
) {
    suspend fun run(userPrompt: String, systemPrompt: String): String {
        val msgs = mutableListOf(
            Message.system(systemPrompt),
            Message.user(userPrompt)
        )
        var toolCount = 0
        while (true) {
            val step = llm.complete(msgs)
            if (step.toolCalls.isEmpty()) return step.content

            val call = step.toolCalls.first()
            toolCount++
            if (toolCount > maxToolCalls) {
                msgs += Message.system("You are using too many steps. Ask the user for a more specific date/time range or event name (e.g., 'vacations').\n" +
                        "💡 Tip: Use list-events WITHOUT calendarId and WITH 'query' to search across all calendars.\n" +
                        "🚫 Remember: Always use time tools instead of guessing dates.\n" +
                        "🚨 CRITICAL: Even when at tool limit, NEVER guess dates manually.\n" +
                        "🚨 If you can't use more tools, ask the user to be more specific instead of guessing.")
                continue
            }

            val toolResult = when (call.name) {
                "getEventTimes" -> runCatching { Json.parseToJsonElement(timeTools.getEventTimes(/* parse args */)) }
                "list-events"  -> runCatching { /* calendar.listEvents(...) -> Json */ }
                "create-event" -> runCatching { /* calendar.createEvent(...) -> Json */ }
                "update-event" -> runCatching { /* calendar.updateEvent(...) -> Json */ }
                "delete-event" -> runCatching { /* calendar.deleteEvent(...) -> Json */ }
                else -> Result.failure(IllegalArgumentException("Unknown tool ${'$'}{call.name}"))
            }

            msgs += Message.assistantToolCall(call)
            msgs += Message.toolResult(call.name, toolResult.getOrElse { JsonNull })
        }
    }
}
```

In‑app service that replaces `/messages`:
```kotlin
class FrontendMessageService(
    private val sessions: FrontendSessionManager,
    private val history: ConversationHistoryStore,
    private val agentFactory: AgentFactory,
    private val accountProvider: AccountProvider
) {
    private val agents = mutableMapOf<String, AgentContext>() // sessionToken -> agent

    suspend fun send(req: MessageRequest): MessageResponse {
        val s = sessions.currentSession()
            ?: return MessageResponse(message = "❌ Invalid or expired session. Please login again.", success = false)

        val userEmail = s.email
        val account = accountProvider.getAccount(userEmail)
        if (account?.credits == 0) {
            return MessageResponse(
                message = "❌ No tienes suficientes créditos. Necesitas hacer upgrade a tu plan para continuar.",
                success = false,
                creditsRemaining = account.credits,
                subscriptionPlan = account.subscriptionPlanDisplay
            )
        }

        val agentCtx = agents.getOrPut(s.sessionToken) {
            agentFactory.initializeForUser(
                userEmail = userEmail,
                subscriptionPlan = account?.subscriptionPlanDisplay ?: "FREE",
                timezone = TimeZone.currentSystemDefault().id,
                session = s
            )
        }

        history.saveUserMessage(userEmail, req.message)
        val responseText = agentCtx.processMessage(req.message)
        history.saveAssistantMessage(
            userEmail, responseText,
            metadata = mapOf(
                "model" to agentCtx.modelName,
                "endpoint" to "in-app/messages",
                "conversationId" to (req.conversationId ?: "default")
            )
        )

        return MessageResponse(
            message = responseText,
            conversationId = req.conversationId,
            userInfo = UserBasicInfo(email = userEmail, firstName = account?.firstName, fullName = account?.fullName),
            success = true,
            creditsRemaining = account?.credits,
            subscriptionPlan = account?.subscriptionPlanDisplay
        )
    }
}
```

Desktop-only MCP launcher (optional):
```kotlin
class DesktopMcpController(
    private val mcpServerPath: String,
    private val nodePath: String = "node"
) {
    private val processes = mutableMapOf<String, Process>()

    fun startForUser(session: AppSession, tokens: GoogleTokens) {
        val env = mapOf(
            "USER_EMAIL" to session.email,
            "USER_SESSION" to session.sessionId,
            "GOOGLE_ACCESS_TOKEN" to tokens.accessToken,
            "GOOGLE_REFRESH_TOKEN" to (tokens.refreshToken ?: ""),
            "GOOGLE_TOKEN_EXPIRY" to (tokens.expiresAt ?: "")
        )
        val nodeIndex = File(mcpServerPath, "build/index.js")
        val pb = ProcessBuilder(nodePath, nodeIndex.absolutePath)
            .directory(File(mcpServerPath))
            .redirectError(ProcessBuilder.Redirect.INHERIT)
        env.forEach { (k,v) -> pb.environment()[k] = v }
        val p = pb.start()
        processes[session.sessionId] = p
    }

    fun stopForSession(sessionId: String) {
        processes.remove(sessionId)?.let { p ->
            p.destroy(); if (!p.waitFor(5, TimeUnit.SECONDS)) p.destroyForcibly()
        }
    }
}
```

---

### Streaming UX (Compose)
- Expose a `Flow<String>`/`StateFlow` from the agent and render incrementally with `collectAsState()`.
- Provide a Cancel button to `job.cancel()` the running completion.
- Show a short preface (e.g., "🤖 Procesando…") before first token.

---

### Persistence & Memory
- Conversation history: `SQLDelight` or `multiplatform-settings` with `kotlinx.serialization` for messages.
- Agent memory: small KV store (default calendar, last range). Support an "ephemeral mode" that never persists.
- Privacy: Never persist provider tokens in plaintext; use keystore/keychain.

---

### Telemetry & Guardrails
- Optional Langfuse or custom metrics: collect time-to-first-token, total latency, tool counts, error classes.
- Backoff on 429/5xx (LLM/Google) with jittered exponential backoff.
- Error taxonomy for UX: Auth (re-login), Permission (missing scopes), Not Found (bad calendar ID), Transient (retry).

---

### Migration Plan (App-side)
1) Feature flag: "Run agent locally"; keep server `/messages` as fallback.
2) Phase 1
   - Move/confirm `TimeTools.kt` is KMP-ready in `commonMain`.
   - Implement `CalendarTools` (REST) and `LlmClient` with streaming + cancellation.
   - Implement `FrontendAgent` loop + 5-call guardrail.
3) Phase 2
   - OAuth PKCE per platform; secure storage for refresh tokens.
   - Compose UI for chat, streaming, cancel, deletion confirmation.
4) Phase 3
   - Beta rollout (5–10%); measure latency/success; watch calendar write errors.
5) Phase 4
   - Remove `/messages` usage for most users; keep fallback.
6) Phase 5 (optional)
   - Desktop: add MCP spawn mode. Mobile/Web: consider remote MCP over WebSocket if needed.

---

### What to Remove When Fully Migrated
- Server chat endpoints/DTOs used solely for agent mediation (`ChatRequest/Response`, `MessageRequest/Response`).
- Server-side MCP stdio transport wiring in `BackendMain.kt` (if not needed elsewhere).
- Per-user `AIAgentService` chat orchestration for normal flows.

Keep any unrelated endpoints you still need (auth bootstrap, subscription/credits if enforced server-side).

---

### Checklist (Copy into Tickets)
- Agent Core (shared)
  - [ ] `Agent.kt` loop with 5-tool guardrail copied from `SimpleCalendarStrategy.kt` messages
  - [ ] `LlmClient.kt` with streaming, cancellation, provider token hookup
  - [ ] `ToolRegistry.kt` (wiring `TimeTools` + Calendar tools)
- Prompts
  - [ ] System prompt from `BackendMain.kt` (~3245–3265) with placeholders for name/email/timezone/plan
- Tools
  - [ ] Ensure `TimeTools.kt` compiles in `commonMain`
  - [ ] `CalendarTools` (REST) with named-range helpers and cross-calendar `query` search
- Auth
  - [ ] OAuth PKCE per platform; secure token storage
- UI/UX
  - [ ] Compose chat screen with streaming and cancel; deletion confirmation dialog
- Telemetry
  - [ ] Metrics: time-to-first-token, total latency, tool count, error classes
- Rollout
  - [ ] Feature flag and A/B vs server `/messages`

---

### Notes on MCP "in the app"
- Desktop (JVM): feasible to bundle Node + spawn MCP locally and connect via stdio/WS.
- Android/iOS/Web: not feasible to host Node MCP in-app. Prefer REST tools or remote MCP over WebSocket.

---

### Summary
- Run the Koog agent in the KMP app, keeping your server semantics and tool-first rules.
- Use `TimeTools` + Google REST tools cross‑platform; optionally use MCP locally on Desktop.
- Protect secrets via an edge token proxy; use OAuth PKCE for Google.
- Replace `/messages` with `FrontendMessageService` that mirrors the original flow entirely on-device.
