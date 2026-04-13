package ru.compadre.mcp.agent

import ru.compadre.mcp.mcp.model.McpToolCallRequest

/**
 * Базовый контракт запроса к агенту.
 */
sealed interface AgentRequest {
    /**
     * Запрос агенту на подключение к MCP server и получение базовой информации.
     */
    data class Connect(
        val endpoint: String,
    ) : AgentRequest

    /**
     * Запрос агенту на вызов MCP-инструмента.
     */
    data class CallTool(
        val endpoint: String,
        val toolCallRequest: McpToolCallRequest,
    ) : AgentRequest
}
