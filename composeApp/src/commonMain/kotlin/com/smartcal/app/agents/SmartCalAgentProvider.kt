package com.smartcal.app.agents

import ai.koog.agents.core.agent.AIAgentService
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.context.RollbackStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteTool
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.nodeLLMSendToolResult
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onToolCall
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.agents.memory.feature.AgentMemory
import ai.koog.agents.memory.providers.NoMemory
import ai.koog.agents.snapshot.feature.Persistence
import ai.koog.agents.snapshot.providers.InMemoryPersistenceStorageProvider
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import com.smartcal.app.config.AppAgentConfig
import com.smartcal.app.config.AppLlmConfig
import com.smartcal.app.tools.TimeTools
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.minutes

/**
 * In-app Koog agent provider for SmartCal.
 *
 * Returns AIAgentService with Persistence for conversation continuity and AgentMemory for fact storage.
 * Uses createAgentAndRun with stable agentId for checkpoint restore.
 * AgentMemory uses NoMemory (KMP-compatible); switch to LocalFileMemoryProvider when file storage is available.
 */
class SmartCalAgentProvider {
    suspend fun provideAgentService(
        userEmail: String? = null,
        timezoneId: String? = null,
        sessionToken: String? = null,
        onAssistantMessage: suspend (String) -> String = { it },
        onErrorEvent: suspend (String) -> Unit = {},
        userInputChannel: Channel<String>? = null,
        responseChannel: Channel<String>? = null,
    ): AIAgentService<String, String, *> {
        val apiKey = AppLlmConfig.apiKey ?: ""
        require(apiKey.isNotBlank()) { "LLM apiKey not configured. Set AppLlmConfig.apiKey before use." }

        val executor = simpleOpenAIExecutor(apiKey)

        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val systemPromptBase = buildString {
            appendLine("Eres un asistente de calendario. SOLO respondes preguntas sobre el calendario del usuario.")
            appendLine("Si la pregunta NO es sobre calendario/eventos (ej: historia, ciencia, trivia), responde: 'Solo puedo ayudarte con tu calendario: ver eventos, crear, editar o eliminar. ¿En qué puedo ayudarte?'")
            appendLine("EXCEPCIÓN IMPORTANTE: Si TÚ acabas de preguntar al usuario si quiere confirmar una acción (ej. '¿Querés que lo elimine?', '¿Elimino el evento X?') y el usuario responde 'Sí', 'Sí por favor', 'Elimínalo', 'Dale', 'Ok', etc., trata esto como CONFIRMACIÓN y ejecuta la acción (delete-event con los datos del contexto). NUNCA respondas con el mensaje genérico en ese caso.")
            appendLine()
            appendLine("Fecha/hora local actual: $now")
            appendLine("Responde siempre en el idioma del usuario.")
            appendLine()
            appendLine("HERRAMIENTAS - Dos tipos:")
            appendLine("- TimeTools (getNamedRange, findNextDayOfWeek, getDateRange): SOLO para obtener timeMin/timeMax. No acceden al calendario.")
            appendLine("- MCP (list-calendars, list-events, delete-event, create-event, update-event): acceden al calendario real. SIEMPRE úsalas para listar, eliminar o crear.")
            appendLine()
            appendLine("CALENDARIO POR NOMBRE: Si el usuario pide crear/editar/eliminar en un calendario que NO es 'primary' (ej: 'ChulayBebi', 'Trabajo', 'Familia'), PRIMERO llama list-calendars. Lista devuelve 'Nombre (calendarId)' por línea. Busca el nombre (ignorando mayúsculas) y usa el calendarId entre paréntesis para create-event, list-events, etc. NUNCA uses el nombre como calendarId directo.")
            appendLine()
            appendLine("FLUJO OBLIGATORIO para ver/eliminar eventos:")
            appendLine("1) TimeTool: getNamedRange('tomorrow'|'today'|'this_friday'|'next_friday'|'this_week'|'next_week') o getDateRange -> timeMin, timeMax")
            appendLine("2) MCP list-events(timeMin, timeMax, query=nombre?) -> eventos con EventId y CalendarId")
            appendLine("3) Para eliminar: delete-event(calendarId, eventId) con los valores del paso 2")
            appendLine()
            appendLine("FLUJO para crear evento en calendario por nombre:")
            appendLine("1) list-calendars -> obtiene 'Nombre (calendarId)' por cada calendario")
            appendLine("2) Busca el nombre del usuario (ej. 'ChulayBebi') en la lista, extrae el calendarId")
            appendLine("3) getNamedRange o getDateRange -> timeMin, timeMax para start/end")
            appendLine("4) create-event(calendarId=<del paso 2>, summary, start, end)")
            appendLine()
            appendLine("CONTEXTO: Usa la conversación anterior. Si mostraste eventos (ej. 'tienes CrossFit mañana') y el usuario pide 'elimina CrossFit' o 'elimina ese evento', busca en ESA fecha: getNamedRange('tomorrow') + list-events(timeMin, timeMax, query='CrossFit'). NO preguntes cuándo está.")
            appendLine("Si pide eliminar por nombre sin fecha: busca en los próximos 7 días con query. Si no hay contexto, usa getNamedRange('this_week') + list-events.")
            appendLine()
            appendLine("NUNCA respondas sin haber llamado a list-events. NUNCA inventes eventId o calendarId (ej. '12345' es inválido).")
            appendLine("Si el usuario confirma 'Sí' para eliminar pero NO tienes eventId/calendarId del contexto (o delete-event falló con 404): llama get_named_range + list-events para obtener los datos reales, luego delete-event. NO des por perdido el evento.")
            appendLine("Para viernes: getNamedRange('this_friday') = este viernes, getNamedRange('next_friday') = próximo viernes. NO uses findNextDayOfWeek para viernes.")
        }

        // Helper: user wants to continue chat (not exit/bye/quit)
        fun userWantsToContinue(input: String): Boolean {
            val n = input.lowercase().trim()
            return !n.contains("exit") && !n.contains("bye") && !n.contains("goodbye") &&
                !n.contains("quit") && n.isNotBlank()
        }

        val hasChatLoop = userInputChannel != null && responseChannel != null

        // Strategy following CalculatorAgentProvider pattern: nodeAssistantMessage -> nodeCallLLM (direct loop).
        // When channels provided: assistant message -> send to responseChannel, receive next user input -> LLM.
        val agentStrategy = strategy<String, String>("SmartCal Agent") {
            val nodeCallLLM by nodeLLMRequest()
            val nodeExecuteTool by nodeExecuteTool()
            val nodeSendToolResult by nodeLLMSendToolResult()

            // Single node for assistant response + next user input (like Calculator's nodeAssistantMessage).
            // Returns next user message to feed back to LLM, or "exit" to finish.
            val nodeAssistantMessage by node<String, String> { message ->
                onAssistantMessage(message)
                if (hasChatLoop && userInputChannel != null) {
                    responseChannel?.let { runBlocking { it.send(message) } }
                    runBlocking {
                        withTimeoutOrNull(2.minutes) { userInputChannel.receive() } ?: "exit"
                    }
                } else "exit"
            }

            edge(nodeStart forwardTo nodeCallLLM)

            edge(nodeCallLLM forwardTo nodeAssistantMessage onAssistantMessage { true })
            edge(nodeCallLLM forwardTo nodeExecuteTool onToolCall { true })

            edge(nodeExecuteTool forwardTo nodeSendToolResult)

            edge(nodeSendToolResult forwardTo nodeAssistantMessage onAssistantMessage { true })
            edge(nodeSendToolResult forwardTo nodeExecuteTool onToolCall { true })

            // Chat loop: nodeAssistantMessage -> nodeCallLLM (like Calculator)
            if (hasChatLoop) {
                edge(nodeAssistantMessage forwardTo nodeCallLLM onCondition { userWantsToContinue(it) })
                edge(nodeAssistantMessage forwardTo nodeFinish onCondition { !userWantsToContinue(it) })
            } else {
                edge(nodeAssistantMessage forwardTo nodeFinish)
            }
        }

        // Build TimeTools registry
        val timeToolsRegistry = ToolRegistry {
            TimeTools.asTools().forEach { tool(it) }
        }

        // Build tool registry with TimeTools + MCP over SSE.
        // MCP server expects GET /:sessionToken/sse and POST /:sessionToken/message.
        // We MUST include sessionToken in the URL so the server routes to the correct user's calendar.
        val baseUrl = AppAgentConfig.mcpSseUrl?.trimEnd('/')
        val toolRegistry = if (baseUrl.isNullOrBlank()) {
            println("⚠️ MCP mode is default but AppAgentConfig.mcpSseUrl is null/blank. Using TimeTools only.")
            timeToolsRegistry
        } else {
            // Session token is required for MCP routing. Use "default" for single-user/local mode.
            val effectiveSession = sessionToken?.takeIf { it.isNotBlank() } ?: "default"
            val sessionBaseUrl = "$baseUrl/$effectiveSession"
            // MCP server: GET /:sessionToken/sse, POST /:sessionToken/message
            // Try /sse first - Koog uses base URL + /sse for the GET connection
            val candidates = listOf(
                "$sessionBaseUrl/sse",   // Correct path for this MCP server
                sessionBaseUrl,
                "$sessionBaseUrl/mcp/sse"
            )
            var combined: ToolRegistry? = null
            var lastError: Throwable? = null
            for (url in candidates) {
                try {
                    println("🔗 Trying MCP SSE at: $url (session: ${effectiveSession.take(12)}...)")
                    val transport = McpToolRegistryProvider.defaultSseTransport(url)
                    val mcpToolRegistry = McpToolRegistryProvider.fromTransport(
                        transport = transport,
                        name = "smartcal-mcp-client-${userEmail ?: "local"}",
                        version = "1.0.0"
                    )
                    println("✅ MCP tools loaded from $url")
                    combined = timeToolsRegistry + mcpToolRegistry
                    break
                } catch (e: Exception) {
                    lastError = e
                    println("❌ Failed MCP SSE at $url: ${e.message} (${e::class.simpleName})")
                }
            }
            if (combined != null) combined else {
                lastError?.let {
                    onErrorEvent("Failed to connect to calendar service: ${it.message}")
                    it.printStackTrace()
                }
                println("↩️ Falling back to TimeTools only - list-events, delete-event, create-event NO disponibles")
                timeToolsRegistry
            }
        }

        val systemPromptFinal = if (!baseUrl.isNullOrBlank() && toolRegistry == timeToolsRegistry) {
            systemPromptBase + "\n\nIMPORTANTE: Las herramientas list-events, delete-event, create-event NO están disponibles. Responde: 'No puedo acceder a tu calendario en este momento. Verifica que el servicio esté conectado.'"
        } else systemPromptBase

        val agentConfig = AIAgentConfig(
            prompt = prompt("smartcal") {
                system(systemPromptFinal)
            },
            model = OpenAIModels.Chat.GPT4o,
            maxAgentIterations = 45  // Increased from 20 to handle complex queries (e.g. "what do I have tomorrow")
        )

        return AIAgentService(
            promptExecutor = executor,
            agentConfig = agentConfig,
            strategy = agentStrategy,
            toolRegistry = toolRegistry,
        ) {
            install(Persistence) {
                storage = InMemoryPersistenceStorageProvider()
                enableAutomaticPersistence = true
                rollbackStrategy = RollbackStrategy.MessageHistoryOnly
            }
            install(AgentMemory) {
                memoryProvider = NoMemory
                agentName = "smartcal-calendar"
                featureName = "calendar-assistant"
                organizationName = "smartcal"
                productName = "smartcal"
            }
            handleEvents {
                onAgentExecutionFailed { ctx ->
                    onErrorEvent(ctx.throwable.message ?: "Unknown agent error")
                }
                onAgentCompleted { _ -> }
            }
        }
    }
}
