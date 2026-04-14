package ru.compadre.mcp.mcp.server.common.toolcall.tools.fetchpost

import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.JsonPlaceholderApiClient
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.common.models.JsonPlaceholderPost

/**
 * Возвращает `inputSchema` для инструмента `fetch_post`.
 */
internal fun fetchPostToolSchema(): ToolSchema = ToolSchema(
    properties = buildJsonObject {
        putJsonObject("postId") {
            put("type", "number")
            put("description", "Идентификатор публикации в JSONPlaceholder.")
        }
    },
    required = listOf("postId"),
)

/**
 * Выполняет server-side сценарий инструмента `fetch_post`.
 *
 * @param arguments JSON-аргументы вызова инструмента
 * @param jsonPlaceholderApiClient клиент к внешнему источнику данных
 * @return результат вызова MCP-инструмента
 */
internal suspend fun fetchPostToolResult(
    arguments: JsonObject?,
    jsonPlaceholderApiClient: JsonPlaceholderApiClient,
): CallToolResult {
    val postId = arguments.requiredIntArgument("postId")
        ?: return errorToolResult("Для инструмента fetch_post требуется числовой аргумент `postId`.")

    val post = jsonPlaceholderApiClient.fetchPost(postId)
        ?: return errorToolResult("Публикация с идентификатором `$postId` не найдена.")

    return CallToolResult(
        content = listOf(TextContent(formatPostText(post))),
        isError = false,
    )
}

private fun formatPostText(post: JsonPlaceholderPost): String = buildList {
    add("Публикация #${post.id}")
    add("Автор: ${post.userId}")
    add("Заголовок: ${post.title}")
    add("Текст: ${post.body}")
}.joinToString(separator = System.lineSeparator())

private fun errorToolResult(message: String): CallToolResult = CallToolResult(
    content = listOf(TextContent(message)),
    isError = true,
)

private fun JsonObject?.requiredIntArgument(name: String): Int? =
    this
        ?.get(name)
        ?.jsonPrimitive
        ?.intOrNull
