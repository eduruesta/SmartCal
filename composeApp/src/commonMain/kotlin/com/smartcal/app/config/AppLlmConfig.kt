package com.smartcal.app.config

/**
 * Lightweight configuration for the in-app LLM client.
 * Pure KMP: values are mutable and can be set by the host app at startup.
 * Do NOT ship production secrets; prefer using a proxy/gateway issuing short-lived tokens.
 */
object AppLlmConfig {
    // OpenAI-compatible endpoint (can be a proxy or gateway). Must NOT end with trailing slash.
    var baseUrl: String = "https://api.openai.com/v1"

    // API key loaded from local Secrets.kt (gitignored)
    var apiKey: String? = Secrets.OPENAI_API_KEY

    // Default model
    var model: String = "gpt-4o-mini"

    // Request timeout in milliseconds
    var requestTimeoutMs: Long = 30_000L
}
