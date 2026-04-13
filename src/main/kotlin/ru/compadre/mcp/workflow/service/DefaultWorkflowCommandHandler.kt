package ru.compadre.mcp.workflow.service

import ru.compadre.mcp.agent.Agent
import ru.compadre.mcp.agent.AgentRequest
import ru.compadre.mcp.agent.AgentResponse
import ru.compadre.mcp.agent.bootstrap.models.AgentCommandId
import ru.compadre.mcp.config.McpProjectConfig
import ru.compadre.mcp.workflow.command.Command
import ru.compadre.mcp.workflow.command.PrepareAgentCommand
import ru.compadre.mcp.workflow.command.ToolPostCommand
import ru.compadre.mcp.workflow.command.ToolPostsCommand
import ru.compadre.mcp.workflow.result.AgentPreparationResult
import ru.compadre.mcp.workflow.result.AvailableCliCommandResult
import ru.compadre.mcp.workflow.result.CommandResult
import ru.compadre.mcp.workflow.result.ToolCallResult

/**
 * Стандартная реализация обработчика workflow-команд.
 */
class DefaultWorkflowCommandHandler(
    private val agent: Agent,
) : WorkflowCommandHandler {
    override suspend fun handle(command: Command): CommandResult = when (command) {
        PrepareAgentCommand -> handlePrepareAgent()
        is ToolPostCommand -> handleToolPost(command)
        ToolPostsCommand -> handleToolPosts()
    }

    /**
     * Запрашивает у агента актуальный snapshot возможностей и переводит его в presentation-friendly результат.
     */
    private suspend fun handlePrepareAgent(): AgentPreparationResult =
        when (val response = agent.handle(AgentRequest.Prepare(McpProjectConfig.knownMcpServers()))) {
            is AgentResponse.PreparationSuccess -> AgentPreparationResult(
                prepared = true,
                availableCommands = response.snapshot.availableCommands.map { command ->
                    AvailableCliCommandResult(
                        pattern = command.cliPattern,
                        description = command.description,
                    )
                },
            )

            is AgentResponse.Failure -> AgentPreparationResult(
                prepared = false,
                errorMessage = response.message,
            )

            is AgentResponse.ConnectSuccess -> AgentPreparationResult(
                prepared = false,
                errorMessage = "Агент вернул результат подключения вместо результата подготовки.",
            )

            is AgentResponse.ToolCallSuccess -> AgentPreparationResult(
                prepared = false,
                errorMessage = "Агент вернул результат вызова инструмента вместо результата подготовки.",
            )
        }

    /**
     * Делегирует вызов пользовательской команды публикации агенту по стабильному commandId.
     */
    private suspend fun handleToolPost(command: ToolPostCommand): ToolCallResult {
        val commandText = "tool post ${command.postId}"

        return when (
            val response = agent.handle(
                AgentRequest.CallAvailableCommand(
                    commandId = AgentCommandId.TOOL_POST,
                    arguments = mapOf("postId" to command.postId),
                ),
            )
        ) {
            is AgentResponse.ToolCallSuccess -> ToolCallResult(
                commandText = commandText,
                successful = !response.result.isError,
                content = response.result.content,
                errorMessage = response.result.content.takeIf { response.result.isError }
                    ?.joinToString(separator = System.lineSeparator()),
            )

            is AgentResponse.Failure -> ToolCallResult(
                commandText = commandText,
                successful = false,
                errorMessage = response.message,
            )

            is AgentResponse.ConnectSuccess -> ToolCallResult(
                commandText = commandText,
                successful = false,
                errorMessage = "Агент вернул результат подключения для сценария вызова пользовательской команды.",
            )

            is AgentResponse.PreparationSuccess -> ToolCallResult(
                commandText = commandText,
                successful = false,
                errorMessage = "Агент вернул результат подготовки вместо сценария вызова пользовательской команды.",
            )
        }
    }

    /**
     * Делегирует вызов пользовательской команды списка публикаций агенту по стабильному commandId.
     */
    private suspend fun handleToolPosts(): ToolCallResult {
        val commandText = "tool posts"

        return when (
            val response = agent.handle(
                AgentRequest.CallAvailableCommand(
                    commandId = AgentCommandId.TOOL_POSTS,
                ),
            )
        ) {
            is AgentResponse.ToolCallSuccess -> ToolCallResult(
                commandText = commandText,
                successful = !response.result.isError,
                content = response.result.content,
                errorMessage = response.result.content.takeIf { response.result.isError }
                    ?.joinToString(separator = System.lineSeparator()),
            )

            is AgentResponse.Failure -> ToolCallResult(
                commandText = commandText,
                successful = false,
                errorMessage = response.message,
            )

            is AgentResponse.ConnectSuccess -> ToolCallResult(
                commandText = commandText,
                successful = false,
                errorMessage = "Агент вернул результат подключения для сценария вызова пользовательской команды.",
            )

            is AgentResponse.PreparationSuccess -> ToolCallResult(
                commandText = commandText,
                successful = false,
                errorMessage = "Агент вернул результат подготовки вместо сценария вызова пользовательской команды.",
            )
        }
    }
}
