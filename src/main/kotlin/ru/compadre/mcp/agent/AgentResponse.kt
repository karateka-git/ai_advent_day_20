package ru.compadre.mcp.agent

import ru.compadre.mcp.mcp.model.McpServerInfo
import ru.compadre.mcp.mcp.model.McpToolCallResult
import ru.compadre.mcp.mcp.model.McpToolDescriptor

/**
 * Базовый контракт ответа агента.
 */
sealed interface AgentResponse {
    /**
     * Успешный результат подключения и чтения базовой MCP-информации.
     */
    data class ConnectSuccess(
        val endpoint: String,
        val serverInfo: McpServerInfo,
        val tools: List<McpToolDescriptor>,
    ) : AgentResponse

    /**
     * Успешный результат вызова MCP-инструмента.
     */
    data class ToolCallSuccess(
        val endpoint: String,
        val result: McpToolCallResult,
    ) : AgentResponse

    /**
     * Ошибка при выполнении агентного запроса.
     */
    data class Failure(
        val message: String,
    ) : AgentResponse
}
