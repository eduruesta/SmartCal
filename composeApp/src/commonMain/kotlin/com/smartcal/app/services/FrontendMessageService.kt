package com.smartcal.app.services

import com.smartcal.app.agents.SmartCalAgentProvider
import com.smartcal.app.config.AppLlmConfig
import com.smartcal.app.models.MessageRequest
import com.smartcal.app.models.MessageResponse
import com.smartcal.app.models.UserBasicInfo
import kotlinx.datetime.TimeZone

/**
 * In-app replacement for `/messages` that runs the Koog agent locally in the app.
 * MVP: LLM-only agent loop (no tools yet). Tools will be added following the frontend guidelines.
 */
class FrontendMessageService(
    private val agentProvider: SmartCalAgentProvider = SmartCalAgentProvider()
) {
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

        val tz = TimeZone.currentSystemDefault().id

        val assistantText = runCatching {
            val agent = agentProvider.provideAgent(
                userEmail = userEmail,
                timezoneId = tz,
                sessionToken = sessionToken,
                onAssistantMessage = { it },
                onErrorEvent = { /* no-op propagate via exception below */ }
            )
            agent.run(req.message)
        }.getOrElse { e ->
            val msg = "❌ Error ejecutando el agente: ${'$'}{e.message ?: e::class.simpleName}"
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
            userInfo = UserBasicInfo(email = userEmail),
            success = true,
            creditsRemaining = null,
            subscriptionPlan = null
        )
    }
}
