package ru.compadre.mcp.agent

import ru.compadre.mcp.mcp.McpClient

/**
 * Стандартная реализация агентного слоя поверх проектного MCP-клиента.
 */
class DefaultAgent(
    private val mcpClient: McpClient,
) : Agent {
    override suspend fun handle(request: AgentRequest): AgentResponse = when (request) {
        is AgentRequest.Connect -> handleConnect(request)
        is AgentRequest.CallTool -> handleCallTool(request)
    }

    private suspend fun handleConnect(request: AgentRequest.Connect): AgentResponse =
        runCatching {
            val snapshot = mcpClient.connect(request.endpoint)

            AgentResponse.ConnectSuccess(
                endpoint = snapshot.endpoint,
                serverInfo = snapshot.serverInfo,
                tools = snapshot.tools,
            )
        }.getOrElse { error ->
            AgentResponse.Failure(
                message = error.message ?: "Не удалось выполнить агентный запрос connect.",
            )
        }

    private suspend fun handleCallTool(request: AgentRequest.CallTool): AgentResponse =
        runCatching {
            AgentResponse.ToolCallSuccess(
                endpoint = request.endpoint,
                result = mcpClient.callTool(
                    endpoint = request.endpoint,
                    request = request.toolCallRequest,
                ),
            )
        }.getOrElse { error ->
            AgentResponse.Failure(
                message = error.message ?: "Не удалось выполнить агентный запрос callTool.",
            )
        }
}
