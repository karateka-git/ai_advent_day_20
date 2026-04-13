package ru.compadre.mcp.mcp.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.sse.SSE
import io.modelcontextprotocol.kotlin.sdk.ExperimentalMcpApi
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.ClientOptions
import io.modelcontextprotocol.kotlin.sdk.client.mcpClient
import io.modelcontextprotocol.kotlin.sdk.client.mcpStreamableHttpTransport
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import ru.compadre.mcp.mcp.client.model.McpConnectionSnapshot
import ru.compadre.mcp.mcp.client.model.McpServerInfo
import ru.compadre.mcp.mcp.client.model.McpToolDescriptor
import ru.compadre.mcp.mcp.toolcall.models.McpToolCallRequest
import ru.compadre.mcp.mcp.toolcall.models.McpToolCallResult

/**
 * Стандартная реализация проектного MCP-клиента через Kotlin MCP SDK.
 */
class DefaultMcpClient : McpClient {
    override suspend fun connect(endpoint: String): McpConnectionSnapshot {
        val httpClient = createHttpClient()

        try {
            val sdkClient = createSdkClient(httpClient, endpoint)
            try {
                val tools = sdkClient.listTools().tools.map(::toToolDescriptor)
                return McpConnectionSnapshot(
                    endpoint = endpoint,
                    serverInfo = toServerInfo(sdkClient),
                    tools = tools,
                )
            } finally {
                sdkClient.close()
            }
        } finally {
            httpClient.close()
        }
    }

    override suspend fun callTool(endpoint: String, request: McpToolCallRequest): McpToolCallResult {
        val httpClient = createHttpClient()

        try {
            val sdkClient = createSdkClient(httpClient, endpoint)
            try {
                return toToolCallResult(
                    toolName = request.toolName,
                    result = sdkClient.callTool(
                        name = request.toolName,
                        arguments = request.arguments,
                    ),
                )
            } finally {
                sdkClient.close()
            }
        } finally {
            httpClient.close()
        }
    }

    private fun createHttpClient(): HttpClient = HttpClient(CIO) {
        install(SSE)
    }

    @OptIn(ExperimentalMcpApi::class)
    private suspend fun createSdkClient(httpClient: HttpClient, endpoint: String): Client {
        val transport = httpClient.mcpStreamableHttpTransport(endpoint)

        return mcpClient(
            clientInfo = Implementation(
                name = "local_mcp_client",
                version = "0.1.0",
                title = "Local MCP Client",
            ),
            clientOptions = ClientOptions(),
            transport = transport,
        )
    }

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
    )
}
