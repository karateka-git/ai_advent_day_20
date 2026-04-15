package ru.compadre.mcp.agent

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import ru.compadre.mcp.agent.bootstrap.AgentCapabilityRegistry
import ru.compadre.mcp.agent.bootstrap.models.AgentCommandId
import ru.compadre.mcp.agent.bootstrap.models.KnownMcpServer
import ru.compadre.mcp.agent.bootstrap.models.McpServerId
import ru.compadre.mcp.mcp.client.McpClient
import ru.compadre.mcp.mcp.client.common.model.McpConnectionSnapshot
import ru.compadre.mcp.mcp.client.common.model.McpServerInfo
import ru.compadre.mcp.mcp.client.common.model.McpToolDescriptor
import ru.compadre.mcp.mcp.client.common.toolcall.model.McpToolCallRequest
import ru.compadre.mcp.mcp.client.common.toolcall.model.McpToolCallResult

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
                    error("tools/call should not be used in this test")
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
                    error("tools/call should not be used in this test")
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
                    error("connect should not be used in this test")
                }

                override suspend fun callTool(endpoint: String, request: McpToolCallRequest): McpToolCallResult {
                    error("tools/call should not be used in this test")
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
                    error("tools/call should not be used in this test")
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
                    error("tools/call should not be used in this test")
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
                    error("connect should not be used in this test")
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
                    error("connect should not be used in this test")
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

    @Test
    fun runSummaryPipelineUsesStatelessSourceAndStatefulSaveFlow() = runBlocking {
        val registry = AgentCapabilityRegistry()
        val statelessEndpoint = "http://127.0.0.1:3000/mcp"
        val statefulEndpoint = "http://127.0.0.1:3001/mcp"
        val callOrder = mutableListOf<String>()
        val agent = DefaultAgent(
            mcpClient = object : McpClient {
                override suspend fun connect(endpoint: String): McpConnectionSnapshot =
                    if (endpoint == statelessEndpoint) {
                        McpConnectionSnapshot(
                            endpoint = endpoint,
                            serverInfo = McpServerInfo(
                                name = "local_mcp_server",
                                version = "0.1.0",
                                title = "Local MCP Server",
                            ),
                            tools = listOf(
                                McpToolDescriptor(name = "list_posts", title = "List Posts"),
                            ),
                        )
                    } else {
                        McpConnectionSnapshot(
                            endpoint = endpoint,
                            serverInfo = McpServerInfo(
                                name = "local_stateful_mcp_server",
                                version = "0.1.0",
                                title = "Local Stateful MCP Server",
                            ),
                            tools = listOf(
                                McpToolDescriptor(name = "merge_posts", title = "Merge Posts"),
                                McpToolDescriptor(name = "save_summary", title = "Save Summary"),
                            ),
                        )
                    }

                override suspend fun callTool(endpoint: String, request: McpToolCallRequest): McpToolCallResult {
                    callOrder += "${endpoint}:${request.toolName}"
                    return when (request.toolName) {
                        "list_posts" -> {
                            assertEquals(statelessEndpoint, endpoint)
                            assertEquals(4, request.arguments["limit"])
                            McpToolCallResult(
                            toolName = request.toolName,
                            isError = false,
                            content = listOf("selected"),
                            structuredContent = buildJsonObject {
                                putJsonArray("posts") {
                                    add(
                                        buildJsonObject {
                                            put("userId", 1)
                                            put("id", 1)
                                            put("title", "One")
                                            put("body", "1234")
                                        },
                                    )
                                    add(
                                        buildJsonObject {
                                            put("userId", 1)
                                            put("id", 2)
                                            put("title", "Two")
                                            put("body", "12")
                                        },
                                    )
                                    add(
                                        buildJsonObject {
                                            put("userId", 1)
                                            put("id", 3)
                                            put("title", "Three")
                                            put("body", "123456")
                                        },
                                    )
                                    add(
                                        buildJsonObject {
                                            put("userId", 1)
                                            put("id", 4)
                                            put("title", "Four")
                                            put("body", "123")
                                        },
                                    )
                                }
                            },
                        )
                        }

                        "merge_posts" -> {
                            assertEquals(statefulEndpoint, endpoint)
                            val posts = request.arguments["posts"] as List<*>
                            assertEquals(3, posts.size)
                            McpToolCallResult(
                                toolName = request.toolName,
                                isError = false,
                                content = listOf("merged"),
                                structuredContent = buildJsonObject {
                                    put("title", "Summary")
                                    put("content", "Summary content")
                                    put("strategy", "long")
                                    putJsonArray("sourcePostIds") {
                                        add(JsonPrimitive(3))
                                        add(JsonPrimitive(1))
                                        add(JsonPrimitive(4))
                                    }
                                },
                            )
                        }

                        "save_summary" -> {
                            assertEquals(statefulEndpoint, endpoint)
                            McpToolCallResult(
                            toolName = request.toolName,
                            isError = false,
                            content = listOf("saved"),
                            structuredContent = buildJsonObject {
                                put("summaryId", "internal-uuid")
                                put("displayId", "summary-1")
                                put("savedAt", "2026-04-14T12:00:00Z")
                                put("title", "Summary")
                                put("content", "Summary content")
                                put("strategy", "long")
                                putJsonArray("sourcePostIds") {
                                    add(JsonPrimitive(3))
                                    add(JsonPrimitive(1))
                                    add(JsonPrimitive(4))
                                }
                            },
                        )
                        }

                        else -> error("unexpected tool ${request.toolName}")
                    }
                }
            },
            capabilityRegistry = registry,
        )

        agent.handle(
            AgentRequest.Prepare(
                servers = listOf(
                    KnownMcpServer(
                        serverId = McpServerId.LOCAL_MCP_SERVER,
                        endpoint = statelessEndpoint,
                    ),
                    KnownMcpServer(
                        serverId = McpServerId.LOCAL_STATEFUL_MCP_SERVER,
                        endpoint = statefulEndpoint,
                    ),
                ),
            ),
        )

        val response = agent.handle(
            AgentRequest.RunSummaryPipeline(
                count = 4,
                strategy = "long",
            ),
        )

        assertIs<AgentResponse.ToolCallSuccess>(response)
        assertEquals("summary_pipeline", response.result.toolName)
        assertEquals(false, response.result.isError)
        assertEquals(true, response.result.content.any { it.contains("summary-1") })
        assertEquals(
            listOf(
                "http://127.0.0.1:3000/mcp:list_posts",
                "http://127.0.0.1:3001/mcp:merge_posts",
                "http://127.0.0.1:3001/mcp:save_summary",
            ),
            callOrder,
        )
    }
}
