package com.smartcal.app.config

/**
 * Agent runtime configuration for tool integration.
 *
 * Note: Keep values mutable so the host app can override at startup.
 */
object AppAgentConfig {
    /**
     * Select which tool mode the in-app agent should use.
     */
    var agentToolMode: AgentToolMode = AgentToolMode.MCP_GOOGLE_CALENDAR

    /**
     * SSE URL for an MCP server exposing Google Calendar tools.
     *
     * For local development:
     * - iOS Simulator: "http://localhost:3001"
     * - Android Emulator: "http://10.0.2.2:3001" (10.0.2.2 is host loopback)
     * - Physical device: Use your machine's local IP (e.g., "http://192.168.1.100:3001")
     *
     * For production: Your hosted MCP server URL on Render
     *
     * Start local MCP server with: cd mcp-google-calendar && npm run start:sse:proxy
     */
    var mcpSseUrl: String? = "https://mcp-google.onrender.com"  // Production URL
}

enum class AgentToolMode {
    /** No tools registered. Useful for local/dev smoke tests. */
    LLM_ONLY,

    /** Use MCP Google Calendar server over SSE transport. */
    MCP_GOOGLE_CALENDAR,
}
