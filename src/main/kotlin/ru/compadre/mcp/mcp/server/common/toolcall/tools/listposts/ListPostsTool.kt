package ru.compadre.mcp.mcp.server.common.toolcall.tools.listposts

import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.JsonPlaceholderApiClient
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.common.models.JsonPlaceholderPost

private const val DEFAULT_POSTS_LIMIT = 10

/**
 * Возвращает `inputSchema` для инструмента `list_posts`.
 */
internal fun listPostsToolSchema(): ToolSchema = ToolSchema(
    properties = buildJsonObject {
        putJsonObject("limit") {
            put("type", "integer")
            put("description", "Необязательное количество публикаций, которое нужно вернуть.")
        }
    },
)

/**
 * Возвращает `outputSchema` для инструмента `list_posts`.
 */
internal fun listPostsToolOutputSchema(): ToolSchema = ToolSchema(
    properties = buildJsonObject {
        putJsonObject("posts") {
            put("type", "array")
            put("description", "Список публикаций из JSONPlaceholder в machine-readable виде.")
        }
    },
    required = listOf("posts"),
)

/**
 * Выполняет server-side сценарий инструмента `list_posts`.
 *
 * @param arguments JSON-аргументы вызова инструмента
 * @param jsonPlaceholderApiClient клиент к внешнему источнику данных
 * @return результат вызова MCP-инструмента
 */
internal suspend fun listPostsToolResult(
    arguments: JsonObject? = null,
    jsonPlaceholderApiClient: JsonPlaceholderApiClient,
): CallToolResult {
    val limit = arguments.optionalIntArgument("limit") ?: DEFAULT_POSTS_LIMIT
    if (limit < 1) {
        return CallToolResult(
            content = listOf(TextContent("Для инструмента `list_posts` аргумент `limit` должен быть не меньше 1.")),
            isError = true,
        )
    }

    val posts = jsonPlaceholderApiClient.fetchPosts(limit)

    return CallToolResult(
        content = listOf(TextContent(formatPostsText(posts))),
        isError = false,
        structuredContent = postsStructuredContent(posts),
    )
}

private fun formatPostsText(posts: List<JsonPlaceholderPost>): String = buildList {
    add("Первые публикации (${posts.size}):")
    posts.forEach { post ->
        add("${post.id}. ${post.title}")
    }
}.joinToString(separator = System.lineSeparator())

private fun postsStructuredContent(posts: List<JsonPlaceholderPost>): JsonObject = buildJsonObject {
    put("posts", buildJsonArray {
        posts.forEach { post ->
            add(
                buildJsonObject {
                    put("userId", post.userId)
                    put("id", post.id)
                    put("title", post.title)
                    put("body", post.body)
                },
            )
        }
    })
}

private fun JsonObject?.optionalIntArgument(name: String): Int? =
    this?.get(name)
        ?.jsonPrimitive
        ?.contentOrNull
        ?.toIntOrNull()
