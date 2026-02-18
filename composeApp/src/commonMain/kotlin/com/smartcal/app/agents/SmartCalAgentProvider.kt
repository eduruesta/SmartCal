package com.smartcal.app.agents

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
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
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import com.smartcal.app.config.AppAgentConfig
import com.smartcal.app.config.AppLlmConfig
import com.smartcal.app.tools.TimeTools
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * In-app Koog agent provider for SmartCal.
 *
 * This implementation provides an agent with TimeTools for date/time operations
 * and optionally MCP Google Calendar tools for calendar operations.
 */
class SmartCalAgentProvider {
    suspend fun provideAgent(
        userEmail: String? = null,
        timezoneId: String? = null,
        sessionToken: String? = null,
        onAssistantMessage: suspend (String) -> String = { it },
        onErrorEvent: suspend (String) -> Unit = {},
    ): AIAgent<String, String> {
        val apiKey = AppLlmConfig.apiKey ?: ""
        require(apiKey.isNotBlank()) { "LLM apiKey not configured. Set AppLlmConfig.apiKey before use." }

        val executor = simpleOpenAIExecutor(apiKey)

        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val systemPromptText = buildString {
            appendLine("Eres un asistente de calendario conciso y útil.")
            appendLine("Fecha/hora local actual: $now")
            appendLine("Responde siempre en el idioma del usuario.")
            appendLine()
            appendLine("REGLAS IMPORTANTES:")
            appendLine("1. NUNCA adivines fechas u horas. Siempre usa las herramientas de tiempo disponibles.")
            appendLine("2. Para crear eventos, usa getEventTimes para obtener los tiempos RFC3339 correctos.")
            appendLine("3. Para consultas sobre fechas, usa getCurrentDateTime, calculateDate, o findNextDayOfWeek según corresponda.")
            appendLine("4. Para listar eventos, usa list-events con timeMin y timeMax.")
            appendLine("5. Para crear eventos, usa create-event con los tiempos RFC3339 obtenidos de getEventTimes.")
        }

        // Strategy with tool support: LLM -> (tool call -> execute -> send result -> LLM)* -> finish
        val agentStrategy = strategy<String, String>("SmartCal Agent") {
            // Core nodes
            val nodeCallLLM by nodeLLMRequest()
            val nodeExecuteTool by nodeExecuteTool()
            val nodeSendToolResult by nodeLLMSendToolResult()

            // Node to handle assistant response
            val nodeHandleResponse by node<String, String> { response ->
                onAssistantMessage(response)
                response
            }

            // Start -> LLM
            edge(nodeStart forwardTo nodeCallLLM)

            // LLM -> Handle response (when assistant message)
            edge(nodeCallLLM forwardTo nodeHandleResponse onAssistantMessage { true })

            // LLM -> Execute tool (when tool call)
            edge(nodeCallLLM forwardTo nodeExecuteTool onToolCall { true })

            // Execute tool -> Send result to LLM
            edge(nodeExecuteTool forwardTo nodeSendToolResult)

            // After sending tool result:
            // - If LLM responds with assistant message -> handle response
            edge(nodeSendToolResult forwardTo nodeHandleResponse onAssistantMessage { true })
            // - If LLM wants another tool call -> execute it
            edge(nodeSendToolResult forwardTo nodeExecuteTool onToolCall { true })

            // Handle response -> Finish
            edge(nodeHandleResponse forwardTo nodeFinish)
        }

        val agentConfig = AIAgentConfig(
            prompt = prompt("smartcal") {
                system(systemPromptText)
            },
            model = OpenAIModels.Chat.GPT4o,
            maxAgentIterations = 20
        )

        // Build TimeTools registry
        val timeToolsRegistry = ToolRegistry {
            TimeTools.getAllTools().forEach { tool(it) }
        }

        // Build tool registry with TimeTools + MCP
        val baseUrl = AppAgentConfig.mcpSseUrl
        val toolRegistry = if (baseUrl.isNullOrBlank() || sessionToken.isNullOrBlank()) {
            if (baseUrl.isNullOrBlank()) {
                println("⚠️ mcpSseUrl is not configured. Using TimeTools only.")
            } else {
                println("⚠️ sessionToken is not provided. Using TimeTools only.")
            }
            timeToolsRegistry
        } else {
            try {
                // Build MCP URL with session token in path
                // Koog's defaultSseTransport expects the base URL, it will append /sse and /message
                val mcpUrl = "${baseUrl.trimEnd('/')}/$sessionToken"
                println("🔗 Connecting to MCP server at: $mcpUrl")
                val mcpTransport = McpToolRegistryProvider.defaultSseTransport(mcpUrl)
                val mcpToolRegistry = McpToolRegistryProvider.fromTransport(
                    transport = mcpTransport,
                    name = "smartcal-mcp-client-${userEmail ?: "local"}",
                    version = "1.0.0"
                )
                println("✅ MCP tools loaded successfully")
                // Combine TimeTools with MCP tools
                timeToolsRegistry + mcpToolRegistry
            } catch (e: Exception) {
                println("❌ Failed to connect to MCP server: ${e.message}")
                println("   Exception type: ${e::class.simpleName}")
                e.printStackTrace()
                onErrorEvent("Failed to connect to calendar service: ${e.message}")
                // Fallback to TimeTools only
                timeToolsRegistry
            }
        }

        return AIAgent(
            promptExecutor = executor,
            strategy = agentStrategy,
            agentConfig = agentConfig,
            toolRegistry = toolRegistry,
        ) {
            handleEvents {
                onAgentExecutionFailed { ctx ->
                    onErrorEvent(ctx.throwable.message ?: "Unknown agent error")
                }
                onAgentCompleted { _ -> }
            }
        }
    }
}
