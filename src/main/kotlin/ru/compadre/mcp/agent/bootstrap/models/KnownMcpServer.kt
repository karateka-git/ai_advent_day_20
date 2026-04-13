package ru.compadre.mcp.agent.bootstrap.models

/**
 * Известный агенту MCP-сервер, который должен участвовать в стартовой подготовке.
 */
data class KnownMcpServer(
    val serverId: McpServerId,
    val endpoint: String,
)
