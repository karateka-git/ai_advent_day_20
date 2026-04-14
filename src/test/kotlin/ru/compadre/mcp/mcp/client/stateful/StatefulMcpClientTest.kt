package ru.compadre.mcp.mcp.client.stateful

import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.BaseNotificationParams
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.CustomNotification
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.Method
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import ru.compadre.mcp.config.McpProjectConfig
import ru.compadre.mcp.mcp.client.common.notifications.RANDOM_POST_NOTIFICATION_METHOD
import ru.compadre.mcp.mcp.client.common.toolcall.model.McpToolCallRequest
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.JsonPlaceholderApiClient
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.common.models.JsonPlaceholderPost
import ru.compadre.mcp.mcp.server.stateful.configureStatefulMcpServer
import ru.compadre.mcp.mcp.server.stateful.createStatefulMcpServer
import ru.compadre.mcp.mcp.server.stateful.session.ClientSessionRegistry
import ru.compadre.mcp.mcp.server.stateful.subscriptions.RandomPostSubscriptionRegistry
import kotlin.time.Duration.Companion.milliseconds

class StatefulMcpClientTest {
    @Test
    fun statefulClientConnectsListsToolsAndClosesSession() = runBlocking {
        val subscriptionRegistry = RandomPostSubscriptionRegistry()
        val statefulServer = createStatefulMcpServer(
            jsonPlaceholderApiClient = object : JsonPlaceholderApiClient {
                override suspend fun fetchPost(postId: Int): JsonPlaceholderPost? = JsonPlaceholderPost(
                    userId = 1,
                    id = postId,
                    title = "test title $postId",
                    body = "test body",
                )

                override suspend fun fetchPosts(limit: Int): List<JsonPlaceholderPost> = emptyList()
            },
            randomPostSubscriptionRegistry = subscriptionRegistry,
            intervalToDelay = { 100.milliseconds },
            randomPostIdProvider = { 42 },
        )
        val sessionRegistry = ClientSessionRegistry(statefulServer)
        val port = ServerSocket(0).use { it.localPort }
        val engine = embeddedServer(CIO, host = McpProjectConfig.SERVER_HOST, port = port) {
            configureStatefulMcpServer(statefulServer)
        }.start()
        val endpoint = "http://${McpProjectConfig.SERVER_HOST}:$port${McpProjectConfig.MCP_PATH}"

        val client = StatefulMcpClient()
        try {
            val snapshot = client.connect(endpoint)
            assertEquals(endpoint, snapshot.endpoint)
            assertTrue(client.session().sessionId.isNotBlank())
            assertEquals(1, sessionRegistry.size())

            val tools = client.listTools().map { it.name }.toSet()
            assertEquals(
                setOf(
                    "start_random_posts",
                    "pick_random_posts",
                    "merge_posts",
                    "save_summary",
                    "list_saved_summaries",
                    "get_saved_summary",
                ),
                tools,
            )

            val startResult = client.callTool(
                McpToolCallRequest(
                    toolName = "start_random_posts",
                    arguments = emptyMap(),
                ),
            )
            assertEquals(false, startResult.isError)
            assertEquals(5, subscriptionRegistry.all().single().intervalMinutes)

            client.callTool(
                McpToolCallRequest(
                    toolName = "start_random_posts",
                    arguments = mapOf("intervalMinutes" to 7),
                ),
            )
            assertEquals(1, subscriptionRegistry.size())
            assertEquals(7, subscriptionRegistry.all().single().intervalMinutes)

        } finally {
            client.close()
            engine.stop(1000, 1000)
        }
    }

    @Test
    fun statefulClientReceivesImmediateCustomNotification() = runBlocking {
        val statefulServer = Server(
            serverInfo = Implementation(
                name = "test_stateful_mcp_server",
                version = "0.1.0",
                title = "Test Stateful MCP Server",
            ),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = false),
                ),
            ),
            instructions = "Test server for custom notifications.",
        ) {
            addTool(
                name = "start_random_posts",
                description = "Starts random posts.",
            ) {
                notification(
                    CustomNotification(
                        method = Method.Custom(RANDOM_POST_NOTIFICATION_METHOD),
                        params = BaseNotificationParams(
                            meta = buildJsonObject {
                                put("message", "test push message")
                            },
                        ),
                    ),
                )

                CallToolResult(
                    content = listOf(TextContent("ok")),
                    isError = false,
                )
            }
        }

        val port = ServerSocket(0).use { it.localPort }
        val engine = embeddedServer(CIO, host = McpProjectConfig.SERVER_HOST, port = port) {
            configureStatefulMcpServer(statefulServer)
        }.start()
        val endpoint = "http://${McpProjectConfig.SERVER_HOST}:$port${McpProjectConfig.MCP_PATH}"

        val client = StatefulMcpClient()
        try {
            client.connect(endpoint)
            val notificationDeferred = async {
                withTimeout(5_000) {
                    client.randomPostNotifications().first()
                }
            }

            client.callTool(
                McpToolCallRequest(
                    toolName = "start_random_posts",
                    arguments = emptyMap(),
                ),
            )
            val notification = notificationDeferred.await()

            assertEquals("test push message", notification.message)
        } finally {
            client.close()
            engine.stop(1000, 1000)
        }
    }

    @Test
    fun statefulClientReceivesDelayedRandomPostNotification() = runBlocking {
        val subscriptionRegistry = RandomPostSubscriptionRegistry()
        val statefulServer = createStatefulMcpServer(
            jsonPlaceholderApiClient = object : JsonPlaceholderApiClient {
                override suspend fun fetchPost(postId: Int): JsonPlaceholderPost? = JsonPlaceholderPost(
                    userId = 1,
                    id = postId,
                    title = "title $postId",
                    body = "body",
                )

                override suspend fun fetchPosts(limit: Int): List<JsonPlaceholderPost> = emptyList()
            },
            randomPostSubscriptionRegistry = subscriptionRegistry,
            intervalToDelay = { 50.milliseconds },
            randomPostIdProvider = { 42 },
        )

        val port = ServerSocket(0).use { it.localPort }
        val engine = embeddedServer(CIO, host = McpProjectConfig.SERVER_HOST, port = port) {
            configureStatefulMcpServer(statefulServer)
        }.start()
        val endpoint = "http://${McpProjectConfig.SERVER_HOST}:$port${McpProjectConfig.MCP_PATH}"

        val client = StatefulMcpClient()
        try {
            client.connect(endpoint)
            val notificationDeferred = async {
                withTimeout(5_000) {
                    client.randomPostNotifications().first()
                }
            }

            client.callTool(
                McpToolCallRequest(
                    toolName = "start_random_posts",
                    arguments = mapOf("intervalMinutes" to 1),
                ),
            )

            val notification = notificationDeferred.await()
            assertEquals("Случайная публикация #42: title 42", notification.message)
            assertEquals(1, subscriptionRegistry.size())
        } finally {
            client.close()
            engine.stop(1000, 1000)
        }
    }

    @Test
    fun toToolCallResultKeepsStructuredContent() {
        val client = StatefulMcpClient()

        val result = client.toToolCallResult(
            toolName = "save_summary",
            result = CallToolResult(
                content = listOf(TextContent("saved")),
                isError = false,
                structuredContent = buildJsonObject {
                    put("displayId", "summary-1")
                },
            ),
        )

        assertNotNull(result.structuredContent)
        assertEquals("\"summary-1\"", result.structuredContent?.get("displayId").toString())
    }
}
