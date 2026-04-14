package ru.compadre.mcp.mcp.server.common.toolcall.tools.listposts

import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.JsonPlaceholderApiClient
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.common.models.JsonPlaceholderPost

private const val DEFAULT_POSTS_LIMIT = 10

/**
 * Возвращает `inputSchema` для инструмента `list_posts`.
 */
internal fun listPostsToolSchema(): ToolSchema = ToolSchema()

/**
 * Выполняет server-side сценарий инструмента `list_posts`.
 *
 * @param jsonPlaceholderApiClient клиент к внешнему источнику данных
 * @return результат вызова MCP-инструмента
 */
internal suspend fun listPostsToolResult(
    jsonPlaceholderApiClient: JsonPlaceholderApiClient,
): CallToolResult {
    val posts = jsonPlaceholderApiClient.fetchPosts(DEFAULT_POSTS_LIMIT)

    return CallToolResult(
        content = listOf(TextContent(formatPostsText(posts))),
        isError = false,
    )
}

private fun formatPostsText(posts: List<JsonPlaceholderPost>): String = buildList {
    add("Первые публикации (${posts.size}):")
    posts.forEach { post ->
        add("${post.id}. ${post.title}")
    }
}.joinToString(separator = System.lineSeparator())
