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
import ru.compadre.mcp.mcp.toolcall.models.McpToolCallResult
import ru.compadre.mcp.workflow.command.PrepareAgentCommand
import ru.compadre.mcp.workflow.command.ToolPostCommand
import ru.compadre.mcp.workflow.command.ToolPostsCommand
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
}
