package ru.compadre.mcp.workflow.service

import ru.compadre.mcp.agent.Agent
import ru.compadre.mcp.agent.AgentRequest
import ru.compadre.mcp.agent.AgentResponse
import ru.compadre.mcp.agent.bootstrap.models.AgentCommandId
import ru.compadre.mcp.agent.bootstrap.models.PreparedMcpServer
import ru.compadre.mcp.config.McpProjectConfig
import ru.compadre.mcp.workflow.command.Command
import ru.compadre.mcp.workflow.command.PrepareAgentCommand
import ru.compadre.mcp.workflow.command.ToolPostCommand
import ru.compadre.mcp.workflow.command.ToolPostsCommand
import ru.compadre.mcp.workflow.command.ToolStartRandomPostsCommand
import ru.compadre.mcp.workflow.command.ToolSummariesCommand
import ru.compadre.mcp.workflow.command.ToolSummaryPostsCommand
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
        is ToolStartRandomPostsCommand -> handleToolStartRandomPosts(command)
        ToolSummariesCommand -> handleToolSummaries()
        is ToolSummaryPostsCommand -> handleToolSummaryPosts(command)
    }

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
                warnings = response.snapshot.servers
                    .filterNot { it.prepared }
                    .map(::preparationWarningFor),
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

    private suspend fun handleToolPost(command: ToolPostCommand): ToolCallResult =
        handleAvailableCommand(
            commandText = "tool post ${command.postId}",
            commandId = AgentCommandId.TOOL_POST,
            arguments = mapOf("postId" to command.postId),
        )

    private suspend fun handleToolPosts(): ToolCallResult =
        handleAvailableCommand(
            commandText = "tool posts",
            commandId = AgentCommandId.TOOL_POSTS,
        )

    private suspend fun handleToolStartRandomPosts(command: ToolStartRandomPostsCommand): ToolCallResult {
        val commandText = buildString {
            append("tool start-random-posts")
            command.intervalMinutes?.let { append(" $it") }
        }
        val arguments = command.intervalMinutes?.let { mapOf("intervalMinutes" to it) } ?: emptyMap()

        return handleAvailableCommand(
            commandText = commandText,
            commandId = AgentCommandId.TOOL_START_RANDOM_POSTS,
            arguments = arguments,
        )
    }

    private suspend fun handleToolSummaryPosts(command: ToolSummaryPostsCommand): ToolCallResult =
        when (
            val response = agent.handle(
                AgentRequest.RunSummaryPipeline(
                    count = command.count,
                    strategy = command.strategy,
                ),
            )
        ) {
            is AgentResponse.ToolCallSuccess -> ToolCallResult(
                commandText = "tool summary posts ${command.count} ${command.strategy}",
                successful = !response.result.isError,
                content = response.result.content,
                errorMessage = response.result.content.takeIf { response.result.isError }
                    ?.joinToString(separator = System.lineSeparator()),
            )

            is AgentResponse.Failure -> ToolCallResult(
                commandText = "tool summary posts ${command.count} ${command.strategy}",
                successful = false,
                errorMessage = response.message,
            )

            is AgentResponse.ConnectSuccess -> ToolCallResult(
                commandText = "tool summary posts ${command.count} ${command.strategy}",
                successful = false,
                errorMessage = "Агент вернул результат подключения вместо pipeline-результата.",
            )

            is AgentResponse.PreparationSuccess -> ToolCallResult(
                commandText = "tool summary posts ${command.count} ${command.strategy}",
                successful = false,
                errorMessage = "Агент вернул результат подготовки вместо pipeline-результата.",
            )
        }

    private suspend fun handleToolSummaries(): ToolCallResult =
        handleAvailableCommand(
            commandText = "tool summaries",
            commandId = AgentCommandId.TOOL_SUMMARIES,
        )

    private suspend fun handleAvailableCommand(
        commandText: String,
        commandId: AgentCommandId,
        arguments: Map<String, Any?> = emptyMap(),
    ): ToolCallResult = when (
        val response = agent.handle(
            AgentRequest.CallAvailableCommand(
                commandId = commandId,
                arguments = arguments,
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

    private fun preparationWarningFor(server: PreparedMcpServer): String {
        val reason = when {
            server.errorMessage.isNullOrBlank() -> "причина недоступности не указана"
            "connection refused" in server.errorMessage.lowercase() -> "соединение отклонено"
            else -> server.errorMessage
        }

        return "Предупреждение: MCP-сервер `${server.serverId.value}` по адресу `${server.endpoint}` недоступен. Команды этого контура скрыты. Причина: $reason."
    }
}
