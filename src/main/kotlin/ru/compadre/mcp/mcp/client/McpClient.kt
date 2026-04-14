package ru.compadre.mcp.mcp.client

import ru.compadre.mcp.mcp.client.common.model.McpConnectionSnapshot
import ru.compadre.mcp.mcp.client.common.toolcall.model.McpToolCallRequest
import ru.compadre.mcp.mcp.client.common.toolcall.model.McpToolCallResult

/**
 * Проектный контракт доступа к MCP.
 */
interface McpClient {
    /**
     * Подключается к MCP server и возвращает снимок базовой информации.
     */
    suspend fun connect(endpoint: String): McpConnectionSnapshot

    /**
     * Вызывает MCP-инструмент и возвращает его проектный результат.
     */
    suspend fun callTool(endpoint: String, request: McpToolCallRequest): McpToolCallResult
}
