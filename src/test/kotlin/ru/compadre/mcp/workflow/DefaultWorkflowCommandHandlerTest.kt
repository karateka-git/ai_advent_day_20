package ru.compadre.mcp.workflow

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import ru.compadre.mcp.agent.Agent
import ru.compadre.mcp.agent.AgentRequest
import ru.compadre.mcp.agent.AgentResponse
import ru.compadre.mcp.mcp.client.model.McpServerInfo
import ru.compadre.mcp.mcp.client.model.McpToolDescriptor
import ru.compadre.mcp.mcp.toolcall.models.McpToolCallResult
import ru.compadre.mcp.workflow.command.ConnectCommand
import ru.compadre.mcp.workflow.command.ToolPostCommand
import ru.compadre.mcp.workflow.result.ConnectResult
import ru.compadre.mcp.workflow.result.ToolCallResult
import ru.compadre.mcp.workflow.service.DefaultWorkflowCommandHandler

class DefaultWorkflowCommandHandlerTest {
    @Test
    fun connectCommandReturnsSuccessfulConnectResult() = runBlocking {
        val handler = DefaultWorkflowCommandHandler(
            agent = object : Agent {
                override suspend fun handle(request: AgentRequest): AgentResponse {
                    val connectRequest = request as AgentRequest.Connect
                    return AgentResponse.ConnectSuccess(
                        endpoint = connectRequest.endpoint,
                        serverInfo = McpServerInfo(
                            name = "local_mcp_server",
                            version = "0.1.0",
                            title = "Local MCP Server",
                            instructions = "Локальный MCP server для sandbox-проекта.",
                        ),
                        tools = listOf(
                            McpToolDescriptor(name = "ping", title = "Ping"),
                            McpToolDescriptor(name = "echo", title = "Echo"),
                        ),
                    )
                }
            },
        )

        val result = handler.handle(
            ConnectCommand(endpointOverride = "http://127.0.0.1:3000/mcp"),
        )

        assertIs<ConnectResult>(result)
        assertEquals(true, result.connected)
        assertEquals("local_mcp_server", result.serverName)
        assertEquals(2, result.tools.size)
    }

    @Test
    fun connectCommandReturnsFailureResultWhenAgentFails() = runBlocking {
        val handler = DefaultWorkflowCommandHandler(
            agent = object : Agent {
                override suspend fun handle(request: AgentRequest): AgentResponse =
                    AgentResponse.Failure("agent failure")
            },
        )

        val result = handler.handle(
            ConnectCommand(endpointOverride = "http://127.0.0.1:3000/mcp"),
        )

        assertIs<ConnectResult>(result)
        assertEquals(false, result.connected)
        assertEquals("agent failure", result.errorMessage)
        assertEquals("http://127.0.0.1:3000/mcp", result.endpoint)
    }

    @Test
    fun toolPostCommandReturnsSuccessfulToolCallResult() = runBlocking {
        val handler = DefaultWorkflowCommandHandler(
            agent = object : Agent {
                override suspend fun handle(request: AgentRequest): AgentResponse {
                    val toolRequest = request as AgentRequest.CallTool
                    assertEquals("fetch_post", toolRequest.toolCallRequest.toolName)
                    assertEquals(1, toolRequest.toolCallRequest.arguments["postId"])

                    return AgentResponse.ToolCallSuccess(
                        endpoint = toolRequest.endpoint,
                        result = McpToolCallResult(
                            toolName = "fetch_post",
                            isError = false,
                            content = listOf("Публикация #1", "Автор: 1"),
                        ),
                    )
                }
            },
        )

        val result = handler.handle(
            ToolPostCommand(
                endpointOverride = "http://127.0.0.1:3000/mcp",
                postId = 1,
            ),
        )

        assertIs<ToolCallResult>(result)
        assertEquals(true, result.successful)
        assertEquals("fetch_post", result.toolName)
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

        val result = handler.handle(
            ToolPostCommand(
                endpointOverride = "http://127.0.0.1:3000/mcp",
                postId = 1,
            ),
        )

        assertIs<ToolCallResult>(result)
        assertEquals(false, result.successful)
        assertEquals("tool agent failure", result.errorMessage)
        assertEquals("fetch_post", result.toolName)
        assertEquals("http://127.0.0.1:3000/mcp", result.endpoint)
    }
}
