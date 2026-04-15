package ru.compadre.mcp.mcp.server.stateless

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.DefaultJsonPlaceholderApiClient
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.JsonPlaceholderApiClient
import ru.compadre.mcp.mcp.server.common.toolcall.tools.fetchpost.fetchPostToolResult
import ru.compadre.mcp.mcp.server.common.toolcall.tools.fetchpost.fetchPostToolSchema
import ru.compadre.mcp.mcp.server.common.toolcall.tools.listposts.listPostsToolOutputSchema
import ru.compadre.mcp.mcp.server.common.toolcall.tools.listposts.listPostsToolResult
import ru.compadre.mcp.mcp.server.common.toolcall.tools.listposts.listPostsToolSchema

internal fun createStatelessMcpServer(
    jsonPlaceholderApiClient: JsonPlaceholderApiClient = DefaultJsonPlaceholderApiClient(),
): Server = Server(
    serverInfo = Implementation(
        name = "local_mcp_server",
        version = "0.1.0",
        title = "Local MCP Server",
    ),
    options = ServerOptions(
        capabilities = ServerCapabilities(
            tools = ServerCapabilities.Tools(listChanged = false),
        ),
    ),
    instructions = "Локальный MCP server для sandbox-проекта.",
) {
    addTool(
        name = "ping",
        title = "Ping",
        description = "Возвращает короткий ответ сервера для проверки доступности.",
        inputSchema = ToolSchema(),
    ) {
        CallToolResult(
            content = listOf(TextContent("pong")),
            isError = false,
        )
    }

    addTool(
        name = "echo",
        title = "Echo",
        description = "Возвращает переданную строку обратно клиенту.",
        inputSchema = echoToolSchema(),
    ) { request ->
        val message = request.requiredStringArgument("message")
            ?: return@addTool CallToolResult(
                content = listOf(TextContent("Для инструмента echo требуется строковый аргумент `message`.")),
                isError = true,
            )

        CallToolResult(
            content = listOf(TextContent(message)),
            isError = false,
        )
    }

    addTool(
        name = "fetch_post",
        title = "Fetch Post",
        description = "Получает публикацию из mock API JSONPlaceholder по идентификатору.",
        inputSchema = fetchPostToolSchema(),
    ) { request ->
        fetchPostToolResult(
            arguments = request.arguments,
            jsonPlaceholderApiClient = jsonPlaceholderApiClient,
        )
    }

    addTool(
        name = "list_posts",
        title = "List Posts",
        description = "Возвращает первые публикации из mock API JSONPlaceholder.",
        inputSchema = listPostsToolSchema(),
        outputSchema = listPostsToolOutputSchema(),
    ) {
        listPostsToolResult(
            jsonPlaceholderApiClient = jsonPlaceholderApiClient,
        )
    }
}

private fun echoToolSchema(): ToolSchema = ToolSchema(
    properties = buildJsonObject {
        putJsonObject("message") {
            put("type", "string")
            put("description", "Строка, которую сервер должен вернуть обратно.")
        }
    },
    required = listOf("message"),
)

private fun CallToolRequest.requiredStringArgument(name: String): String? =
    arguments
        ?.get(name)
        ?.jsonPrimitive
        ?.contentOrNull
