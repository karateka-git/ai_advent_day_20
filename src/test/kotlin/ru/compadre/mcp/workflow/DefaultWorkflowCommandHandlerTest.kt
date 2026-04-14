package ru.compadre.mcp.workflow

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import ru.compadre.mcp.agent.Agent
import ru.compadre.mcp.agent.AgentRequest
import ru.compadre.mcp.agent.AgentResponse
import ru.compadre.mcp.agent.bootstrap.models.AgentCapabilitySnapshot
import ru.compadre.mcp.agent.bootstrap.models.AgentCommandId
import ru.compadre.mcp.agent.bootstrap.models.AvailableAgentCommand
import ru.compadre.mcp.agent.bootstrap.models.McpServerId
import ru.compadre.mcp.mcp.client.common.toolcall.model.McpToolCallResult
import ru.compadre.mcp.workflow.command.PrepareAgentCommand
import ru.compadre.mcp.workflow.command.ToolPostCommand
import ru.compadre.mcp.workflow.command.ToolPostsCommand
import ru.compadre.mcp.workflow.command.ToolStartRandomPostsCommand
import ru.compadre.mcp.workflow.command.ToolSummariesCommand
import ru.compadre.mcp.workflow.command.ToolSummaryPostsCommand
import ru.compadre.mcp.workflow.command.ToolSummarySavedCommand
import ru.compadre.mcp.workflow.result.AgentPreparationResult
import ru.compadre.mcp.workflow.result.ToolCallResult
import ru.compadre.mcp.workflow.service.DefaultWorkflowCommandHandler

class DefaultWorkflowCommandHandlerTest {
    @Test
    fun prepareAgentCommandReturnsSuccessfulPreparationResult() = runBlocking {
        val handler = DefaultWorkflowCommandHandler(
            agent = object : Agent {
                override suspend fun handle(request: AgentRequest): AgentResponse {
                    assertIs<AgentRequest.Prepare>(request)

                    return AgentResponse.PreparationSuccess(
                        snapshot = AgentCapabilitySnapshot(
                            availableCommands = listOf(
                                AvailableAgentCommand(
                                    commandId = AgentCommandId.TOOL_POSTS,
                                    cliPattern = "tool posts",
                                    description = "Показать первые 10 публикаций.",
                                    toolName = "list_posts",
                                    serverId = McpServerId.LOCAL_MCP_SERVER,
                                    endpoint = "http://127.0.0.1:3000/mcp",
                                ),
                            ),
                        ),
                    )
                }
            },
        )

        val result = handler.handle(PrepareAgentCommand)

        assertIs<AgentPreparationResult>(result)
        assertEquals(true, result.prepared)
        assertEquals(1, result.availableCommands.size)
        assertEquals("tool posts", result.availableCommands.single().pattern)
        assertEquals(emptyList(), result.warnings)
    }

    @Test
    fun prepareAgentCommandReturnsFailureResultWhenAgentFails() = runBlocking {
        val handler = DefaultWorkflowCommandHandler(
            agent = object : Agent {
                override suspend fun handle(request: AgentRequest): AgentResponse =
                    AgentResponse.Failure("agent failure")
            },
        )

        val result = handler.handle(PrepareAgentCommand)

        assertIs<AgentPreparationResult>(result)
        assertEquals(false, result.prepared)
        assertEquals("agent failure", result.errorMessage)
    }

    @Test
    fun prepareAgentCommandExposesWarningsForUnavailableServers() = runBlocking {
        val handler = DefaultWorkflowCommandHandler(
            agent = object : Agent {
                override suspend fun handle(request: AgentRequest): AgentResponse {
                    assertIs<AgentRequest.Prepare>(request)

                    return AgentResponse.PreparationSuccess(
                        snapshot = AgentCapabilitySnapshot(
                            servers = listOf(
                                ru.compadre.mcp.agent.bootstrap.models.PreparedMcpServer(
                                    serverId = McpServerId.LOCAL_STATEFUL_MCP_SERVER,
                                    endpoint = "http://127.0.0.1:3001/mcp",
                                    prepared = false,
                                    errorMessage = "Connection refused: getsockopt",
                                ),
                            ),
                        ),
                    )
                }
            },
        )

        val result = handler.handle(PrepareAgentCommand)

        assertIs<AgentPreparationResult>(result)
        assertEquals(true, result.prepared)
        assertEquals(
            listOf(
                "Предупреждение: MCP-сервер `local_stateful_mcp_server` по адресу `http://127.0.0.1:3001/mcp` недоступен. Команды этого контура скрыты. Причина: соединение отклонено.",
            ),
            result.warnings,
        )
    }

    @Test
    fun toolPostCommandUsesAvailableCommandRouting() = runBlocking {
        val handler = DefaultWorkflowCommandHandler(
            agent = object : Agent {
                override suspend fun handle(request: AgentRequest): AgentResponse {
                    val toolRequest = request as AgentRequest.CallAvailableCommand
                    assertEquals(AgentCommandId.TOOL_POST, toolRequest.commandId)
                    assertEquals(1, toolRequest.arguments["postId"])

                    return AgentResponse.ToolCallSuccess(
                        endpoint = "http://127.0.0.1:3000/mcp",
                        result = McpToolCallResult(
                            toolName = "fetch_post",
                            isError = false,
                            content = listOf("Публикация #1", "Автор: 1"),
                        ),
                    )
                }
            },
        )

        val result = handler.handle(ToolPostCommand(postId = 1))

        assertIs<ToolCallResult>(result)
        assertEquals(true, result.successful)
        assertEquals("tool post 1", result.commandText)
        assertEquals(listOf("Публикация #1", "Автор: 1"), result.content)
    }

    @Test
    fun toolPostCommandReturnsFailureResultWhenAgentFails() = runBlocking {
        val handler = DefaultWorkflowCommandHandler(
            agent = object : Agent {
                override suspend fun handle(request: AgentRequest): AgentResponse =
                    AgentResponse.Failure("tool agent failure")
            },
        )

        val result = handler.handle(ToolPostCommand(postId = 1))

        assertIs<ToolCallResult>(result)
        assertEquals(false, result.successful)
        assertEquals("tool agent failure", result.errorMessage)
        assertEquals("tool post 1", result.commandText)
    }

    @Test
    fun toolPostsCommandUsesAvailableCommandRouting() = runBlocking {
        val handler = DefaultWorkflowCommandHandler(
            agent = object : Agent {
                override suspend fun handle(request: AgentRequest): AgentResponse {
                    val toolRequest = request as AgentRequest.CallAvailableCommand
                    assertEquals(AgentCommandId.TOOL_POSTS, toolRequest.commandId)
                    assertEquals(emptyMap(), toolRequest.arguments)

                    return AgentResponse.ToolCallSuccess(
                        endpoint = "http://127.0.0.1:3000/mcp",
                        result = McpToolCallResult(
                            toolName = "list_posts",
                            isError = false,
                            content = listOf("Первые 10 публикаций:", "1. sunt aut facere"),
                        ),
                    )
                }
            },
        )

        val result = handler.handle(ToolPostsCommand)

        assertIs<ToolCallResult>(result)
        assertEquals(true, result.successful)
        assertEquals("tool posts", result.commandText)
        assertEquals(listOf("Первые 10 публикаций:", "1. sunt aut facere"), result.content)
    }

    @Test
    fun toolStartRandomPostsCommandUsesAvailableCommandRouting() = runBlocking {
        val handler = DefaultWorkflowCommandHandler(
            agent = object : Agent {
                override suspend fun handle(request: AgentRequest): AgentResponse {
                    val toolRequest = request as AgentRequest.CallAvailableCommand
                    assertEquals(AgentCommandId.TOOL_START_RANDOM_POSTS, toolRequest.commandId)
                    assertEquals(7, toolRequest.arguments["intervalMinutes"])

                    return AgentResponse.ToolCallSuccess(
                        endpoint = "http://127.0.0.1:3001/mcp",
                        result = McpToolCallResult(
                            toolName = "start_random_posts",
                            isError = false,
                            content = listOf("Random posts push активирован для текущей сессии. Интервал: 7 мин."),
                        ),
                    )
                }
            },
        )

        val result = handler.handle(ToolStartRandomPostsCommand(intervalMinutes = 7))

        assertIs<ToolCallResult>(result)
        assertEquals(true, result.successful)
        assertEquals("tool start-random-posts 7", result.commandText)
    }

    @Test
    fun toolStartRandomPostsCommandUsesEmptyArgumentsWhenIntervalIsOmitted() = runBlocking {
        val handler = DefaultWorkflowCommandHandler(
            agent = object : Agent {
                override suspend fun handle(request: AgentRequest): AgentResponse {
                    val toolRequest = request as AgentRequest.CallAvailableCommand
                    assertEquals(AgentCommandId.TOOL_START_RANDOM_POSTS, toolRequest.commandId)
                    assertEquals(emptyMap(), toolRequest.arguments)

                    return AgentResponse.ToolCallSuccess(
                        endpoint = "http://127.0.0.1:3001/mcp",
                        result = McpToolCallResult(
                            toolName = "start_random_posts",
                            isError = false,
                            content = listOf("Random posts push активирован для текущей сессии. Интервал: 5 мин."),
                        ),
                    )
                }
            },
        )

        val result = handler.handle(ToolStartRandomPostsCommand(intervalMinutes = null))

        assertIs<ToolCallResult>(result)
        assertEquals(true, result.successful)
        assertEquals("tool start-random-posts", result.commandText)
    }

    @Test
    fun toolSummaryPostsCommandRunsPipelineThroughAgent() = runBlocking {
        val handler = DefaultWorkflowCommandHandler(
            agent = object : Agent {
                override suspend fun handle(request: AgentRequest): AgentResponse {
                    val pipelineRequest = request as AgentRequest.RunSummaryPipeline
                    assertEquals(10, pipelineRequest.count)
                    assertEquals("short", pipelineRequest.strategy)

                    return AgentResponse.ToolCallSuccess(
                        endpoint = "http://127.0.0.1:3001/mcp",
                        result = McpToolCallResult(
                            toolName = "summary_pipeline",
                            isError = false,
                            content = listOf("Summary pipeline выполнен успешно.", "Сохранён summary: summary-1"),
                        ),
                    )
                }
            },
        )

        val result = handler.handle(ToolSummaryPostsCommand(count = 10, strategy = "short"))

        assertIs<ToolCallResult>(result)
        assertEquals(true, result.successful)
        assertEquals("tool summary posts 10 short", result.commandText)
        assertEquals(true, result.content.any { it.contains("summary-1") })
    }

    @Test
    fun toolSummarySavedCommandUsesAvailableCommandRouting() = runBlocking {
        val handler = DefaultWorkflowCommandHandler(
            agent = object : Agent {
                override suspend fun handle(request: AgentRequest): AgentResponse {
                    val toolRequest = request as AgentRequest.CallAvailableCommand
                    assertEquals(AgentCommandId.TOOL_SUMMARY_SAVED, toolRequest.commandId)
                    assertEquals("summary-2", toolRequest.arguments["summaryId"])

                    return AgentResponse.ToolCallSuccess(
                        endpoint = "http://127.0.0.1:3001/mcp",
                        result = McpToolCallResult(
                            toolName = "get_saved_summary",
                            isError = false,
                            content = listOf("Короткие публикации: title [summary-2]", "Стратегия: short"),
                        ),
                    )
                }
            },
        )

        val result = handler.handle(ToolSummarySavedCommand(summaryId = "summary-2"))

        assertIs<ToolCallResult>(result)
        assertEquals(true, result.successful)
        assertEquals("tool summary saved summary-2", result.commandText)
    }

    @Test
    fun toolSummariesCommandUsesAvailableCommandRouting() = runBlocking {
        val handler = DefaultWorkflowCommandHandler(
            agent = object : Agent {
                override suspend fun handle(request: AgentRequest): AgentResponse {
                    val toolRequest = request as AgentRequest.CallAvailableCommand
                    assertEquals(AgentCommandId.TOOL_SUMMARIES, toolRequest.commandId)

                    return AgentResponse.ToolCallSuccess(
                        endpoint = "http://127.0.0.1:3001/mcp",
                        result = McpToolCallResult(
                            toolName = "list_saved_summaries",
                            isError = false,
                            content = listOf("Сохранённые summary: 1", "1. [summary-1] Короткие публикации: title | short | posts: 1, 2, 3"),
                        ),
                    )
                }
            },
        )

        val result = handler.handle(ToolSummariesCommand)

        assertIs<ToolCallResult>(result)
        assertEquals(true, result.successful)
        assertEquals("tool summaries", result.commandText)
    }
}
