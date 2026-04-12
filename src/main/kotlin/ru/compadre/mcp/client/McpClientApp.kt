package ru.compadre.mcp.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.sse.SSE
import io.modelcontextprotocol.kotlin.sdk.ExperimentalMcpApi
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.ClientOptions
import io.modelcontextprotocol.kotlin.sdk.client.mcpClient
import io.modelcontextprotocol.kotlin.sdk.client.mcpStreamableHttpTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ListToolsResult
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import ru.compadre.mcp.config.McpProjectConfig
import kotlinx.coroutines.runBlocking
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets

/**
 * Точка входа минимального MCP client для сценария `initialize -> tools/list`.
 */
fun main(): Unit = runBlocking {
    configureUtf8Console()

    val endpoint = McpProjectConfig.defaultEndpoint()
    val httpClient = createHttpClient()

    try {
        val mcpClient = createMcpClient(httpClient, endpoint)
        try {
            printConnectionSummary(endpoint, mcpClient)

            val toolsResult = mcpClient.listTools()
            println(renderToolsList(toolsResult))
        } finally {
            mcpClient.close()
        }
    } finally {
        httpClient.close()
    }
}

private fun createHttpClient(): HttpClient = HttpClient(CIO) {
    install(SSE)
}

private fun configureUtf8Console() {
    System.setOut(
        PrintStream(
            FileOutputStream(FileDescriptor.out),
            true,
            StandardCharsets.UTF_8,
        ),
    )
    System.setErr(
        PrintStream(
            FileOutputStream(FileDescriptor.err),
            true,
            StandardCharsets.UTF_8,
        ),
    )
}

@OptIn(ExperimentalMcpApi::class)
private suspend fun createMcpClient(httpClient: HttpClient, endpoint: String): Client {
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

private fun printConnectionSummary(endpoint: String, client: Client) {
    println("Connected to MCP server: $endpoint")
    println("Server name: ${client.serverVersion?.name ?: "<unknown>"}")
    println("Server version: ${client.serverVersion?.version ?: "<unknown>"}")
    println("Server title: ${client.serverVersion?.title ?: "<unknown>"}")
    println("Server instructions: ${client.serverInstructions ?: "<none>"}")
}

internal fun renderToolsList(result: ListToolsResult): String {
    val tools = result.tools
    if (tools.isEmpty()) {
        return "Available tools: <none>"
    }

    val lines = buildList {
        add("Available tools (${tools.size}):")
        tools.forEachIndexed { index, tool ->
            add("${index + 1}. ${formatToolLine(tool)}")
        }
    }

    return lines.joinToString(separator = System.lineSeparator())
}

private fun formatToolLine(tool: Tool): String {
    val title = tool.title?.takeIf { it.isNotBlank() } ?: tool.name
    val description = tool.description?.takeIf { it.isNotBlank() } ?: "Описание не указано."
    return "$title [${tool.name}] - $description"
}
