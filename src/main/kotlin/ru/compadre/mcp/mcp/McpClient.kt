package ru.compadre.mcp.mcp

import ru.compadre.mcp.mcp.model.McpConnectionSnapshot
import ru.compadre.mcp.mcp.model.McpToolCallRequest
import ru.compadre.mcp.mcp.model.McpToolCallResult

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
