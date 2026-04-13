package ru.compadre.mcp.workflow.service

import ru.compadre.mcp.agent.Agent
import ru.compadre.mcp.agent.AgentRequest
import ru.compadre.mcp.agent.AgentResponse
import ru.compadre.mcp.mcp.toolcall.models.McpToolCallRequest
import ru.compadre.mcp.workflow.command.Command
import ru.compadre.mcp.workflow.command.ConnectCommand
import ru.compadre.mcp.workflow.command.ToolPostCommand
import ru.compadre.mcp.workflow.result.CommandResult
import ru.compadre.mcp.workflow.result.ConnectResult
import ru.compadre.mcp.workflow.result.ConnectToolResult
import ru.compadre.mcp.workflow.result.ToolCallResult

/**
 * Стандартная реализация обработчика workflow-команд.
 */
class DefaultWorkflowCommandHandler(
    private val agent: Agent,
) : WorkflowCommandHandler {
    override suspend fun handle(command: Command): CommandResult = when (command) {
        is ConnectCommand -> handleConnect(command)
        is ToolPostCommand -> handleToolPost(command)
    }

    private suspend fun handleConnect(command: ConnectCommand): ConnectResult {
        val endpoint = command.endpointOverride
            ?: error("Для ConnectCommand требуется endpoint на этапе до внедрения presentation-слоя.")

        return when (val response = agent.handle(AgentRequest.Connect(endpoint))) {
            is AgentResponse.ConnectSuccess -> ConnectResult(
                endpoint = response.endpoint,
                connected = true,
                serverName = response.serverInfo.name,
                serverVersion = response.serverInfo.version,
                serverTitle = response.serverInfo.title,
                serverInstructions = response.serverInfo.instructions,
                tools = response.tools.map { tool ->
                    ConnectToolResult(
                        name = tool.name,
                        title = tool.title,
                        description = tool.description,
                    )
                },
            )
            is AgentResponse.Failure -> ConnectResult(
                endpoint = endpoint,
                connected = false,
                errorMessage = response.message,
            )
            is AgentResponse.ToolCallSuccess -> ConnectResult(
                endpoint = endpoint,
                connected = false,
                errorMessage = "Агент вернул результат вызова инструмента для сценария connect.",
            )
        }
    }

    private suspend fun handleToolPost(command: ToolPostCommand): ToolCallResult {
        val endpoint = command.endpointOverride
            ?: error("Для ToolPostCommand требуется endpoint на этапе до внедрения presentation-слоя.")

        return when (
            val response = agent.handle(
                AgentRequest.CallTool(
                    endpoint = endpoint,
                    toolCallRequest = McpToolCallRequest(
                        toolName = "fetch_post",
                        arguments = mapOf("postId" to command.postId),
                    ),
                ),
            )
        ) {
            is AgentResponse.ToolCallSuccess -> ToolCallResult(
                endpoint = response.endpoint,
                toolName = response.result.toolName,
                successful = !response.result.isError,
                content = response.result.content,
                errorMessage = response.result.content.takeIf { response.result.isError }
                    ?.joinToString(separator = System.lineSeparator()),
            )
            is AgentResponse.Failure -> ToolCallResult(
                endpoint = endpoint,
                toolName = "fetch_post",
                successful = false,
                errorMessage = response.message,
            )
            is AgentResponse.ConnectSuccess -> ToolCallResult(
                endpoint = endpoint,
                toolName = "fetch_post",
                successful = false,
                errorMessage = "Агент вернул результат подключения для сценария вызова инструмента.",
            )
        }
    }
}
