package com.smartcal.app.config

/**
 * Lightweight configuration for the in-app LLM client.
 * Pure KMP: values are mutable and can be set by the host app at startup.
 * Do NOT ship production secrets; prefer using a proxy/gateway issuing short-lived tokens.
 */
object AppLlmConfig {
    // OpenAI-compatible endpoint (can be a proxy or gateway). Must NOT end with trailing slash.
    var baseUrl: String = "https://api.openai.com/v1"

    // API key for local/dev. In production use a gateway or token provider.
    // Set this at runtime - DO NOT hardcode secrets here
    var apiKey: String? = null

    // Default model
    var model: String = "gpt-4o-mini"

    // Request timeout in milliseconds
    var requestTimeoutMs: Long = 30_000L
}
