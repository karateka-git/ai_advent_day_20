package ru.compadre.mcp.agent.bootstrap.models

import ru.compadre.mcp.mcp.client.common.model.McpServerInfo
import ru.compadre.mcp.mcp.client.common.model.McpToolDescriptor

/**
 * Результат подготовки одного MCP-сервера внутри capability-модели агента.
 */
data class PreparedMcpServer(
    val serverId: McpServerId,
    val endpoint: String,
    val prepared: Boolean,
    val serverInfo: McpServerInfo? = null,
    val tools: List<McpToolDescriptor> = emptyList(),
    val errorMessage: String? = null,
)
