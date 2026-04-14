package ru.compadre.mcp.mcp.client.common.session

/**
 * Снимок активной stateful MCP-сессии клиента.
 */
data class McpClientSession(
    val endpoint: String,
    val sessionId: String,
    val protocolVersion: String?,
)
