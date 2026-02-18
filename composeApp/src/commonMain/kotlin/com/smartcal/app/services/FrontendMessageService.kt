package com.smartcal.app.services

import ai.koog.agents.core.agent.AIAgentService
import com.smartcal.app.agents.SmartCalAgentProvider
import com.smartcal.app.config.AppLlmConfig
import com.smartcal.app.models.MessageRequest
import com.smartcal.app.models.MessageResponse
import com.smartcal.app.models.UserBasicInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.seconds

/**
 * In-app replacement for `/messages` that runs the Koog agent locally in the app.
 *
 * Uses a single agent.run per session with chat loop: message history is conserved
 * across turns. Each session has userInputChannel/responseChannel; the agent waits
 * for the next message between responses.
 *
 * Consumes 1 credit via backend POST /credits/consume before processing each message.
 */
private data class ChatSession(
    val userChannel: Channel<String>,
    val responseChannel: Channel<String>,
    val agentJob: kotlinx.coroutines.Job
)

class FrontendMessageService(
    private val calendarService: CalendarService = CalendarService(),
    private val agentProvider: SmartCalAgentProvider = SmartCalAgentProvider(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val sessions = mutableMapOf<String, ChatSession>()
    private val serviceMutex = Mutex()

    suspend fun send(req: MessageRequest, userEmail: String? = null, sessionToken: String? = null): MessageResponse {
        // Guard: require API key configured for local/dev usage
        if (AppLlmConfig.apiKey.isNullOrBlank()) {
            val hint = "❌ LLM no configurado para el modo local.\n" +
                "Configura AppLlmConfig.apiKey (por ejemplo, en el arranque de la app) " +
                "o usa un gateway compatible con OpenAI."
            return MessageResponse(
                message = hint,
                conversationId = req.conversationId ?: "local-default",
                userInfo = UserBasicInfo(email = userEmail),
                success = false
            )
        }

        // Consume 1 credit before processing (requires session token)
        var consumeResponse: MessageResponse? = null
        if (sessionToken != null && sessionToken.isNotBlank()) {
            val consumeResult = calendarService.consumeCredits(sessionToken)
            consumeResult.fold(
                onSuccess = { consumeResponse = it },
                onFailure = { e ->
                    when (e) {
                        is UnauthorizedException -> return MessageResponse(
                            message = "❌ Session inválida o expirada. Por favor inicia sesión de nuevo.",
                            conversationId = req.conversationId ?: "local-default",
                            userInfo = UserBasicInfo(email = userEmail),
                            success = false
                        )
                        is InsufficientCreditsException -> return MessageResponse(
                            message = e.message ?: "❌ No tienes suficientes créditos. Necesitas hacer upgrade a tu plan para continuar.",
                            conversationId = req.conversationId ?: "local-default",
                            userInfo = UserBasicInfo(email = userEmail),
                            success = false,
                            creditsRemaining = e.creditsRemaining,
                            subscriptionPlan = e.subscriptionPlan,
                            showCreditsExhausted = true
                        )
                        else -> return MessageResponse(
                            message = "❌ Error al consumir créditos: ${e.message}",
                            conversationId = req.conversationId ?: "local-default",
                            userInfo = UserBasicInfo(email = userEmail),
                            success = false
                        )
                    }
                }
            )
        }

        val tz = TimeZone.currentSystemDefault().id
        val cacheKey = userEmail?.takeIf { it.isNotBlank() }
            ?: sessionToken?.takeIf { it.isNotBlank() }
            ?: "default"
        val stableAgentId = "calendar-agent-$cacheKey"

        val responseTimeout = 90.seconds

        val assistantText = runCatching {
            val session = serviceMutex.withLock { sessions[cacheKey] }

            if (session != null) {
                // Existing session: send message to agent, receive response
                session.userChannel.send(req.message)
                withTimeoutOrNull(responseTimeout) { session.responseChannel.receive() }
                    ?: throw IllegalStateException("Timeout esperando respuesta del agente")
            } else {
                // New session: create channels, agent with chat loop, launch
                val userCh = Channel<String>(Channel.UNLIMITED)
                val responseCh = Channel<String>(Channel.UNLIMITED)

                val agentService = agentProvider.provideAgentService(
                    userEmail = userEmail,
                    timezoneId = tz,
                    sessionToken = sessionToken,
                    onAssistantMessage = { it },
                    onErrorEvent = { /* propagate via exception */ },
                    userInputChannel = userCh,
                    responseChannel = responseCh
                )

                val agentJob = scope.launch {
                    runCatching {
                        @Suppress("UNCHECKED_CAST")
                        agentService.createAgentAndRun(
                            agentInput = req.message,
                            id = stableAgentId
                        )
                    }.onFailure { e ->
                        responseCh.close(e)
                    }
                }
                agentJob.invokeOnCompletion {
                    scope.launch {
                        serviceMutex.withLock { sessions.remove(cacheKey) }
                    }
                }

                serviceMutex.withLock { sessions[cacheKey] = ChatSession(userCh, responseCh, agentJob) }

                withTimeoutOrNull(responseTimeout) { responseCh.receive() }
                    ?: throw IllegalStateException("Timeout esperando respuesta del agente")
            }
        }.getOrElse { e ->
            val errMsg = e.message?.lowercase() ?: ""
            val isConnectionError = errMsg.contains("sse") ||
                errMsg.contains("connection") ||
                errMsg.contains("transport") ||
                errMsg.contains("timeout") ||
                errMsg.contains("mcp error") ||
                errMsg.contains("no active")
            val isResponseTimeout = errMsg.contains("timeout esperando")
            if (isConnectionError || isResponseTimeout) {
                scope.launch {
                    serviceMutex.withLock {
                        sessions[cacheKey]?.agentJob?.cancel()
                        sessions.remove(cacheKey)
                    }
                }
            }
            if (isConnectionError) {
                return MessageResponse(
                    message = "❌ Se perdió la conexión con el calendario. Por favor, intentá de nuevo.",
                    conversationId = req.conversationId ?: "local-default",
                    userInfo = UserBasicInfo(email = userEmail),
                    success = false
                )
            }
            if (isResponseTimeout) {
                return MessageResponse(
                    message = "❌ El agente tardó demasiado en responder. Intentá de nuevo.",
                    conversationId = req.conversationId ?: "local-default",
                    userInfo = UserBasicInfo(email = userEmail),
                    success = false
                )
            }
            val msg = "❌ Error ejecutando el agente: ${e.message ?: e::class.simpleName}"
            return MessageResponse(
                message = msg,
                conversationId = req.conversationId ?: "local-default",
                userInfo = UserBasicInfo(email = userEmail),
                success = false
            )
        }

        return MessageResponse(
            message = assistantText,
            conversationId = req.conversationId ?: "local-default",
            userInfo = consumeResponse?.userInfo ?: UserBasicInfo(email = userEmail),
            success = true,
            creditsRemaining = consumeResponse?.creditsRemaining,
            subscriptionPlan = consumeResponse?.subscriptionPlan
        )
    }

    /**
     * Clears the chat session. Call on logout.
     * Pass userEmail or sessionToken to clear the correct session.
     */
    fun clearAgentCache(userEmail: String? = null, sessionToken: String? = null) {
        val key = userEmail?.takeIf { it.isNotBlank() }
            ?: sessionToken?.takeIf { it.isNotBlank() }
            ?: "default"
        scope.launch {
            serviceMutex.withLock {
                sessions[key]?.agentJob?.cancel()
                sessions.remove(key)
            }
        }
    }
}
