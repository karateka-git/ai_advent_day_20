package ru.compadre.mcp.agent

import ru.compadre.mcp.agent.bootstrap.models.AgentCommandId
import ru.compadre.mcp.agent.bootstrap.models.KnownMcpServer
import ru.compadre.mcp.mcp.toolcall.models.McpToolCallRequest

/**
 * Базовый контракт запроса к агенту.
 */
sealed interface AgentRequest {
    /**
     * Запрос агенту на стартовую подготовку и discovery известных MCP-серверов.
     */
    data class Prepare(
        val servers: List<KnownMcpServer>,
    ) : AgentRequest

    /**
     * Запрос агенту на подключение к MCP server и получение базовой информации.
     */
    data class Connect(
        val endpoint: String,
    ) : AgentRequest

    /**
     * Запрос агенту на вызов прикладной команды через capability-реестр.
     */
    data class CallAvailableCommand(
        val commandId: AgentCommandId,
        val arguments: Map<String, Any?> = emptyMap(),
    ) : AgentRequest

    /**
     * Запрос агенту на вызов MCP-инструмента.
     */
    data class CallTool(
        val endpoint: String,
        val toolCallRequest: McpToolCallRequest,
    ) : AgentRequest
}
