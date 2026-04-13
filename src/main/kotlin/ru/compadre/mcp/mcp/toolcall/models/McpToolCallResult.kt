package ru.compadre.mcp.mcp.toolcall.models

/**
 * Проектная модель результата вызова MCP-инструмента.
 *
 * @property toolName имя вызванного инструмента
 * @property isError признак ошибки вызова инструмента
 * @property content читаемое содержимое результата
 */
data class McpToolCallResult(
    val toolName: String,
    val isError: Boolean,
    val content: List<String>,
)
