package ru.compadre.mcp.agent

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import ru.compadre.mcp.agent.bootstrap.AgentCapabilityRegistry
import ru.compadre.mcp.agent.bootstrap.models.AgentCommandId
import ru.compadre.mcp.agent.bootstrap.models.KnownMcpServer
import ru.compadre.mcp.agent.bootstrap.models.McpServerId
import ru.compadre.mcp.mcp.client.McpClient
import ru.compadre.mcp.mcp.client.model.McpConnectionSnapshot
import ru.compadre.mcp.mcp.client.model.McpServerInfo
import ru.compadre.mcp.mcp.client.model.McpToolDescriptor
import ru.compadre.mcp.mcp.toolcall.models.McpToolCallRequest
import ru.compadre.mcp.mcp.toolcall.models.McpToolCallResult

class DefaultAgentTest {
    @Test
    fun prepareRequestCollectsSuccessfulServersAndStoresSnapshot() = runBlocking {
        val registry = AgentCapabilityRegistry()
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
                        McpToolDescriptor(name = "fetch_post", title = "Fetch Post"),
                        McpToolDescriptor(name = "list_posts", title = "List Posts"),
                    ),
                )

                override suspend fun callTool(endpoint: String, request: McpToolCallRequest): McpToolCallResult {
                    error("Сценарий tools/call не должен использоваться в этом тесте.")
                }
            },
            capabilityRegistry = registry,
        )

        val response = agent.handle(
            AgentRequest.Prepare(
                servers = listOf(
                    KnownMcpServer(
                        serverId = McpServerId.LOCAL_MCP_SERVER,
                        endpoint = "http://127.0.0.1:3000/mcp",
                    ),
                ),
            ),
        )

        assertIs<AgentResponse.PreparationSuccess>(response)
        assertEquals(1, response.snapshot.servers.size)
        assertEquals(true, response.snapshot.servers.single().prepared)
        assertEquals(2, response.snapshot.servers.single().tools.size)
        assertEquals(2, response.snapshot.availableCommands.size)
        assertEquals(response.snapshot, registry.snapshot())
    }

    @Test
    fun prepareRequestKeepsUnavailableServerInsideSnapshot() = runBlocking {
        val registry = AgentCapabilityRegistry()
        val agent = DefaultAgent(
            mcpClient = object : McpClient {
                override suspend fun connect(endpoint: String): McpConnectionSnapshot =
                    if (endpoint.contains("broken")) {
                        error("server unavailable")
                    } else {
                        McpConnectionSnapshot(
                            endpoint = endpoint,
                            serverInfo = McpServerInfo(
                                name = "local_mcp_server",
                                version = "0.1.0",
                                title = "Local MCP Server",
                            ),
                            tools = listOf(
                                McpToolDescriptor(name = "fetch_post", title = "Fetch Post"),
                            ),
                        )
                    }

                override suspend fun callTool(endpoint: String, request: McpToolCallRequest): McpToolCallResult {
                    error("Сценарий tools/call не должен использоваться в этом тесте.")
                }
            },
            capabilityRegistry = registry,
        )

        val response = agent.handle(
            AgentRequest.Prepare(
                servers = listOf(
                    KnownMcpServer(
                        serverId = McpServerId.LOCAL_MCP_SERVER,
                        endpoint = "http://127.0.0.1:3000/mcp",
                    ),
                    KnownMcpServer(
                        serverId = McpServerId("broken"),
                        endpoint = "http://127.0.0.1:3999/mcp-broken",
                    ),
                ),
            ),
        )

        assertIs<AgentResponse.PreparationSuccess>(response)
        assertEquals(2, response.snapshot.servers.size)
        assertEquals(true, response.snapshot.servers.first().prepared)
        assertEquals(false, response.snapshot.servers.last().prepared)
        assertEquals("server unavailable", response.snapshot.servers.last().errorMessage)
        assertEquals(1, response.snapshot.availableCommands.size)
        assertEquals(response.snapshot, registry.snapshot())
    }

    @Test
    fun availableCommandRequestRoutesToServerAndToolFromRegistry() = runBlocking {
        val registry = AgentCapabilityRegistry()
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
                        McpToolDescriptor(name = "fetch_post", title = "Fetch Post"),
                    ),
                )

                override suspend fun callTool(endpoint: String, request: McpToolCallRequest): McpToolCallResult {
                    assertEquals("http://127.0.0.1:3000/mcp", endpoint)
                    assertEquals("fetch_post", request.toolName)
                    assertEquals(7, request.arguments["postId"])

                    return McpToolCallResult(
                        toolName = request.toolName,
                        isError = false,
                        content = listOf("Публикация #7"),
                    )
                }
            },
            capabilityRegistry = registry,
        )

        agent.handle(
            AgentRequest.Prepare(
                servers = listOf(
                    KnownMcpServer(
                        serverId = McpServerId.LOCAL_MCP_SERVER,
                        endpoint = "http://127.0.0.1:3000/mcp",
                    ),
                ),
            ),
        )

        val response = agent.handle(
            AgentRequest.CallAvailableCommand(
                commandId = AgentCommandId.TOOL_POST,
                arguments = mapOf("postId" to 7),
            ),
        )

        assertIs<AgentResponse.ToolCallSuccess>(response)
        assertEquals("http://127.0.0.1:3000/mcp", response.endpoint)
        assertEquals("fetch_post", response.result.toolName)
        assertEquals(listOf("Публикация #7"), response.result.content)
    }

    @Test
    fun availableCommandRequestFailsBeforePreparation() = runBlocking {
        val agent = DefaultAgent(
            mcpClient = object : McpClient {
                override suspend fun connect(endpoint: String): McpConnectionSnapshot {
                    error("Сценарий connect не должен использоваться в этом тесте.")
                }

                override suspend fun callTool(endpoint: String, request: McpToolCallRequest): McpToolCallResult {
                    error("Сценарий tools/call не должен использоваться в этом тесте.")
                }
            },
        )

        val response = agent.handle(
            AgentRequest.CallAvailableCommand(
                commandId = AgentCommandId.TOOL_POSTS,
            ),
        )

        assertIs<AgentResponse.Failure>(response)
        assertEquals(
            "Команда `tool.posts` недоступна: агент ещё не подготовил MCP-возможности.",
            response.message,
        )
    }

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
