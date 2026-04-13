package ru.compadre.mcp.agent

import ru.compadre.mcp.mcp.client.model.McpServerInfo
import ru.compadre.mcp.mcp.client.model.McpToolDescriptor
import ru.compadre.mcp.mcp.toolcall.models.McpToolCallResult

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
