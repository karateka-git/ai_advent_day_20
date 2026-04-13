package ru.compadre.mcp.agent

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import ru.compadre.mcp.mcp.McpClient
import ru.compadre.mcp.mcp.model.McpConnectionSnapshot
import ru.compadre.mcp.mcp.model.McpServerInfo
import ru.compadre.mcp.mcp.model.McpToolCallRequest
import ru.compadre.mcp.mcp.model.McpToolCallResult
import ru.compadre.mcp.mcp.model.McpToolDescriptor

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
}
