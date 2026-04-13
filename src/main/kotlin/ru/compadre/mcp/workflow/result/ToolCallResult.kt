package ru.compadre.mcp.workflow.result

/**
 * Результат выполнения workflow-команды вызова MCP-инструмента.
 */
data class ToolCallResult(
    val commandText: String,
    val successful: Boolean,
    val content: List<String> = emptyList(),
    val errorMessage: String? = null,
) : CommandResult
