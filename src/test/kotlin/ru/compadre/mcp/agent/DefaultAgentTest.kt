package ru.compadre.mcp.agent

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import ru.compadre.mcp.mcp.client.McpClient
import ru.compadre.mcp.mcp.client.model.McpConnectionSnapshot
import ru.compadre.mcp.mcp.client.model.McpServerInfo
import ru.compadre.mcp.mcp.client.model.McpToolDescriptor
import ru.compadre.mcp.mcp.toolcall.models.McpToolCallRequest
import ru.compadre.mcp.mcp.toolcall.models.McpToolCallResult

class DefaultAgentTest {
    @Test
    fun connectRequestReturnsNormalizedSuccessResponse() = runBlocking {
        val agent = DefaultAgent(
            mcpClient = object : McpClient {
                override suspend fun connect(endpoint: String): McpConnectionSnapshot = McpConnectionSnapshot(
                    endpoint = endpoint,
                    serverInfo = McpServerInfo(
                        name = "local_mcp_server",
                        version = "0.1.0",
                        title = "Local MCP Server",
                    ),
                    tools = listOf(
                        McpToolDescriptor(name = "ping", title = "Ping"),
                        McpToolDescriptor(name = "echo", title = "Echo"),
                    ),
                )

                override suspend fun callTool(endpoint: String, request: McpToolCallRequest): McpToolCallResult {
                    error("Сценарий tools/call не должен использоваться в этом тесте.")
                }
            },
        )

        val response = agent.handle(AgentRequest.Connect("http://127.0.0.1:3000/mcp"))

        assertIs<AgentResponse.ConnectSuccess>(response)
        assertEquals("http://127.0.0.1:3000/mcp", response.endpoint)
        assertEquals("local_mcp_server", response.serverInfo.name)
        assertEquals(2, response.tools.size)
    }

    @Test
    fun connectRequestReturnsFailureWhenMcpClientThrows() = runBlocking {
        val agent = DefaultAgent(
            mcpClient = object : McpClient {
                override suspend fun connect(endpoint: String): McpConnectionSnapshot {
                    error("boom")
                }

                override suspend fun callTool(endpoint: String, request: McpToolCallRequest): McpToolCallResult {
                    error("Сценарий tools/call не должен использоваться в этом тесте.")
                }
            },
        )

        val response = agent.handle(AgentRequest.Connect("http://127.0.0.1:3000/mcp"))

        assertIs<AgentResponse.Failure>(response)
        assertEquals("boom", response.message)
    }

    @Test
    fun toolCallRequestReturnsNormalizedSuccessResponse() = runBlocking {
        val agent = DefaultAgent(
            mcpClient = object : McpClient {
                override suspend fun connect(endpoint: String): McpConnectionSnapshot {
                    error("Сценарий connect не должен использоваться в этом тесте.")
                }

                override suspend fun callTool(endpoint: String, request: McpToolCallRequest): McpToolCallResult =
                    McpToolCallResult(
                        toolName = request.toolName,
                        isError = false,
                        content = listOf("Публикация #1", "Автор: 1"),
                    )
            },
        )

        val response = agent.handle(
            AgentRequest.CallTool(
                endpoint = "http://127.0.0.1:3000/mcp",
                toolCallRequest = McpToolCallRequest(
                    toolName = "fetch_post",
                    arguments = mapOf("postId" to 1),
                ),
            ),
        )

        assertIs<AgentResponse.ToolCallSuccess>(response)
        assertEquals("http://127.0.0.1:3000/mcp", response.endpoint)
        assertEquals("fetch_post", response.result.toolName)
        assertEquals(listOf("Публикация #1", "Автор: 1"), response.result.content)
    }

    @Test
    fun toolCallRequestReturnsFailureWhenMcpClientThrows() = runBlocking {
        val agent = DefaultAgent(
            mcpClient = object : McpClient {
                override suspend fun connect(endpoint: String): McpConnectionSnapshot {
                    error("Сценарий connect не должен использоваться в этом тесте.")
                }

                override suspend fun callTool(endpoint: String, request: McpToolCallRequest): McpToolCallResult {
                    error("tool failure")
                }
            },
        )

        val response = agent.handle(
            AgentRequest.CallTool(
                endpoint = "http://127.0.0.1:3000/mcp",
                toolCallRequest = McpToolCallRequest(
                    toolName = "fetch_post",
                    arguments = mapOf("postId" to 1),
                ),
            ),
        )

        assertIs<AgentResponse.Failure>(response)
        assertEquals("tool failure", response.message)
    }
}
