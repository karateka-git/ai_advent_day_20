package ru.compadre.mcp.mcp.toolcall.models

/**
 * Проектная модель запроса на вызов MCP-инструмента.
 *
 * @property toolName имя вызываемого инструмента
 * @property arguments аргументы вызова инструмента
 */
data class McpToolCallRequest(
    val toolName: String,
    val arguments: Map<String, Any?>,
)
