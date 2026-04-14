package ru.compadre.mcp.mcp.client.stateful

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.sse.SSE
import io.modelcontextprotocol.kotlin.sdk.ExperimentalMcpApi
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.ClientOptions
import io.modelcontextprotocol.kotlin.sdk.client.mcpClient
import io.modelcontextprotocol.kotlin.sdk.client.mcpSseTransport
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.CustomNotification
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.Method
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import ru.compadre.mcp.mcp.client.McpClient
import ru.compadre.mcp.mcp.client.common.model.McpConnectionSnapshot
import ru.compadre.mcp.mcp.client.common.notifications.RANDOM_POST_NOTIFICATION_METHOD
import ru.compadre.mcp.mcp.client.common.notifications.RandomPostMessage
import ru.compadre.mcp.mcp.client.common.model.McpServerInfo
import ru.compadre.mcp.mcp.client.common.model.McpToolDescriptor
import ru.compadre.mcp.mcp.client.common.session.McpClientSession
import ru.compadre.mcp.mcp.client.common.toolcall.model.McpToolCallRequest
import ru.compadre.mcp.mcp.client.common.toolcall.model.McpToolCallResult

/**
 * Stateful MCP-клиент, удерживающий transport и session lifecycle между вызовами.
 */
class StatefulMcpClient : McpClient {
    private val randomPostNotifications = MutableSharedFlow<RandomPostMessage>(extraBufferCapacity = 16)
    private var httpClient: HttpClient? = null
    private var transport: SseClientTransport? = null
    private var sdkClient: Client? = null
    private var endpoint: String? = null
    private var sessionId: String? = null

    override suspend fun connect(endpoint: String): McpConnectionSnapshot {
        if (sdkClient != null) {
            check(this.endpoint == endpoint) { "Stateful MCP client already connected to another endpoint." }
            return snapshot(endpoint)
        }

        val createdHttpClient = HttpClient(CIO) {
            install(SSE)
        }
        val createdTransport = createdHttpClient.mcpSseTransport(endpoint)
        val createdSdkClient = createSdkClient(createdTransport)
        createdSdkClient.setNotificationHandler<CustomNotification>(Method.Custom(RANDOM_POST_NOTIFICATION_METHOD)) { notification ->
            val message = notification.meta
                ?.get("message")
                ?.jsonPrimitive
                ?.content

            if (message != null) {
                randomPostNotifications.tryEmit(RandomPostMessage(message))
            }
            CompletableDeferred(Unit)
        }

        httpClient = createdHttpClient
        transport = createdTransport
        sdkClient = createdSdkClient
        this.endpoint = endpoint
        sessionId = extractSessionId(createdTransport)

        requireSession(endpoint)
        return snapshot(endpoint)
    }

    suspend fun listTools(): List<McpToolDescriptor> = requireClient()
        .listTools()
        .tools
        .map(::toToolDescriptor)

    suspend fun callTool(request: McpToolCallRequest): McpToolCallResult = toToolCallResult(
        toolName = request.toolName,
        result = requireClient().callTool(
            name = request.toolName,
            arguments = request.arguments,
        ),
    )

    override suspend fun callTool(endpoint: String, request: McpToolCallRequest): McpToolCallResult {
        check(this.endpoint == endpoint) { "Stateful MCP client is not connected to endpoint `$endpoint`." }
        return callTool(request)
    }

    fun serverInfo(): McpServerInfo = toServerInfo(requireClient())

    fun sessionId(): String? = sessionId

    fun session(): McpClientSession = requireSession(
        endpoint = endpoint ?: error("Stateful MCP client is not connected."),
    )

    fun randomPostNotifications(): SharedFlow<RandomPostMessage> = randomPostNotifications

    suspend fun close() {
        try {
            sdkClient?.close()
        } finally {
            sdkClient = null
            transport = null
            httpClient = null
            endpoint = null
            sessionId = null
        }
    }

    private fun requireClient(): Client = sdkClient ?: error("Stateful MCP client is not connected.")

    private fun requireSession(endpoint: String): McpClientSession = McpClientSession(
        endpoint = endpoint,
        sessionId = sessionId
            ?: error("Stateful transport did not provide session id after initialization."),
        protocolVersion = null,
    )

    private suspend fun snapshot(endpoint: String): McpConnectionSnapshot = McpConnectionSnapshot(
        endpoint = endpoint,
        serverInfo = serverInfo(),
        tools = run {
            val client = requireClient()
            client.listTools().tools.map(::toToolDescriptor)
        },
    )

    @OptIn(ExperimentalMcpApi::class)
    private suspend fun createSdkClient(transport: SseClientTransport): Client = mcpClient(
        clientInfo = Implementation(
            name = "local_stateful_mcp_client",
            version = "0.1.0",
            title = "Local Stateful MCP Client",
        ),
        clientOptions = ClientOptions(),
        transport = transport,
    )

    private fun toServerInfo(client: Client): McpServerInfo = McpServerInfo(
        name = client.serverVersion?.name ?: "<unknown>",
        version = client.serverVersion?.version ?: "<unknown>",
        title = client.serverVersion?.title,
        instructions = client.serverInstructions,
    )

    private fun toToolDescriptor(tool: Tool): McpToolDescriptor = McpToolDescriptor(
        name = tool.name,
        title = tool.title,
        description = tool.description,
    )

    internal fun toToolCallResult(toolName: String, result: CallToolResult): McpToolCallResult = McpToolCallResult(
        toolName = toolName,
        isError = result.isError == true,
        content = result.content.map { content ->
            when (content) {
                is TextContent -> content.text
                else -> content.toString()
            }
        },
        structuredContent = result.structuredContent,
    )

    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun extractSessionId(transport: SseClientTransport): String? {
        val endpointField = transport::class.java.getDeclaredField("endpoint")
        endpointField.isAccessible = true
        val endpointValue = endpointField.get(transport) as CompletableDeferred<*>?
        val endpointUrl = endpointValue?.getCompleted()?.toString() ?: return null

        return URI(endpointUrl)
            .query
            ?.split("&")
            ?.firstOrNull { it.startsWith("sessionId=") }
            ?.substringAfter("=")
            ?.takeIf { it.isNotBlank() }
    }
}
