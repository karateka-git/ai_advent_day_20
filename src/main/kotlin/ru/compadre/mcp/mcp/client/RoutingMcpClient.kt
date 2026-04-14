package ru.compadre.mcp.mcp.client

import kotlinx.coroutines.flow.SharedFlow
import ru.compadre.mcp.config.McpProjectConfig
import ru.compadre.mcp.mcp.client.common.model.McpConnectionSnapshot
import ru.compadre.mcp.mcp.client.common.notifications.RandomPostMessage
import ru.compadre.mcp.mcp.client.common.toolcall.model.McpToolCallRequest
import ru.compadre.mcp.mcp.client.common.toolcall.model.McpToolCallResult
import ru.compadre.mcp.mcp.client.stateful.StatefulMcpClient
import ru.compadre.mcp.mcp.client.stateless.StatelessMcpClient

/**
 * Общий MCP-клиент приложения, маршрутизирующий запросы между stateless и stateful runtime-контурами.
 */
class RoutingMcpClient(
    private val statelessClient: StatelessMcpClient = StatelessMcpClient(),
    private val statefulClient: StatefulMcpClient = StatefulMcpClient(),
) : McpClient {
    override suspend fun connect(endpoint: String): McpConnectionSnapshot =
        when (endpoint) {
            McpProjectConfig.statefulEndpoint() -> statefulClient.connect(endpoint)
            else -> statelessClient.connect(endpoint)
        }

    override suspend fun callTool(endpoint: String, request: McpToolCallRequest): McpToolCallResult =
        when (endpoint) {
            McpProjectConfig.statefulEndpoint() -> statefulClient.callTool(endpoint, request)
            else -> statelessClient.callTool(endpoint, request)
        }

    fun randomPostNotifications(): SharedFlow<RandomPostMessage> = statefulClient.randomPostNotifications()

    suspend fun close() {
        statefulClient.close()
    }
}
