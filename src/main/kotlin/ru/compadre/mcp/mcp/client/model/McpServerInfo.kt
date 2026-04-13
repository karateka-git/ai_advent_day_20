package ru.compadre.mcp.mcp.client.model

/**
 * Проектное описание server info, полученного от MCP.
 */
data class McpServerInfo(
    val name: String,
    val version: String,
    val title: String? = null,
    val instructions: String? = null,
)
