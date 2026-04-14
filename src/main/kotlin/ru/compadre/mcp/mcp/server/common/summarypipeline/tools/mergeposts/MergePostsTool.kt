package ru.compadre.mcp.mcp.server.common.summarypipeline.tools.mergeposts

import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import ru.compadre.mcp.mcp.server.common.summarypipeline.models.SummaryDraft
import ru.compadre.mcp.mcp.server.common.summarypipeline.models.SummaryPost
import ru.compadre.mcp.mcp.server.common.summarypipeline.tools.summaryPipelineJson

fun mergePostsToolSchema(): ToolSchema = ToolSchema(
    properties = buildJsonObject {
        putJsonObject("strategy") {
            put("type", "string")
            put("description", "Стратегия отбора публикаций, использованная агентом.")
        }
        putJsonObject("posts") {
            put("type", "array")
            put("description", "Публикации, которые нужно объединить в summary.")
        }
    },
    required = listOf("strategy", "posts"),
)

fun mergePostsToolOutputSchema(): ToolSchema = ToolSchema(
    properties = buildJsonObject {
        putJsonObject("title") {
            put("type", "string")
        }
        putJsonObject("content") {
            put("type", "string")
        }
        putJsonObject("sourcePostIds") {
            put("type", "array")
        }
        putJsonObject("strategy") {
            put("type", "string")
        }
    },
    required = listOf("title", "content", "sourcePostIds", "strategy"),
)

internal fun mergePostsToolResult(arguments: JsonObject?): CallToolResult {
    val strategy = arguments.requiredStringArgument("strategy")
        ?: return CallToolResult(
            content = listOf(TextContent("Для инструмента `merge_posts` требуется строковый аргумент `strategy`.")),
            isError = true,
        )
    val posts = arguments.summaryPostsArgument("posts")
        ?: return CallToolResult(
            content = listOf(TextContent("Для инструмента `merge_posts` требуется массив публикаций `posts`.")),
            isError = true,
        )

    if (posts.isEmpty()) {
        return CallToolResult(
            content = listOf(TextContent("Инструмент `merge_posts` не получил публикации для объединения.")),
            isError = true,
        )
    }

    val draft = SummaryDraft(
        title = "Summary по публикациям ($strategy)",
        content = buildString {
            appendLine("Стратегия отбора: $strategy")
            appendLine("Количество объединённых публикаций: ${posts.size}")
            appendLine()
            posts.forEachIndexed { index, post ->
                appendLine("${index + 1}. ${post.title}")
                appendLine("Post ID: ${post.id}")
                appendLine(post.body.lineSequence().firstOrNull()?.trim().orEmpty())
                if (index != posts.lastIndex) {
                    appendLine()
                }
            }
        }.trim(),
        sourcePostIds = posts.map { it.id },
        strategy = strategy,
    )

    return CallToolResult(
        content = listOf(TextContent(draft.content)),
        isError = false,
        structuredContent = summaryPipelineJson.parseToJsonElement(
            summaryPipelineJson.encodeToString(draft),
        ).jsonObject,
    )
}

private fun JsonObject?.requiredStringArgument(name: String): String? =
    this?.get(name)
        ?.jsonPrimitive
        ?.contentOrNull
        ?.takeIf { it.isNotBlank() }

private fun JsonObject?.summaryPostsArgument(name: String): List<SummaryPost>? =
    this?.get(name)
        ?.jsonArray
        ?.mapNotNull { element ->
            runCatching {
                val post = element.jsonObject
                SummaryPost(
                    userId = post.getValue("userId").jsonPrimitive.content.toInt(),
                    id = post.getValue("id").jsonPrimitive.content.toInt(),
                    title = post.getValue("title").jsonPrimitive.content,
                    body = post.getValue("body").jsonPrimitive.content,
                )
            }.getOrNull()
        }
