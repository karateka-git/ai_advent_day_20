package ru.compadre.mcp.mcp.server.common.summarypipeline.tools.pickrandomposts

import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlin.math.max
import kotlin.random.Random
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.JsonPlaceholderApiClient
import ru.compadre.mcp.mcp.server.common.summarypipeline.models.PostSelection
import ru.compadre.mcp.mcp.server.common.summarypipeline.models.SummaryPost
import ru.compadre.mcp.mcp.server.common.summarypipeline.tools.summaryPipelineJson

fun pickRandomPostsToolSchema(): ToolSchema = ToolSchema(
    properties = buildJsonObject {
        putJsonObject("count") {
            put("type", "integer")
            put("description", "Количество случайных публикаций для первого шага pipeline.")
        }
    },
    required = listOf("count"),
)

fun pickRandomPostsToolOutputSchema(): ToolSchema = ToolSchema(
    properties = buildJsonObject {
        putJsonObject("posts") {
            put("type", "array")
            put("description", "Список случайно выбранных публикаций.")
        }
    },
    required = listOf("posts"),
)

internal suspend fun pickRandomPostsToolResult(
    arguments: JsonObject?,
    jsonPlaceholderApiClient: JsonPlaceholderApiClient,
    random: Random = Random.Default,
): CallToolResult {
    val count = arguments.requiredIntArgument("count")
        ?: return CallToolResult(
            content = listOf(TextContent("Для инструмента `pick_random_posts` требуется целочисленный аргумент `count`.")),
            isError = true,
        )

    if (count < 1) {
        return CallToolResult(
            content = listOf(TextContent("Для инструмента `pick_random_posts` аргумент `count` должен быть не меньше 1.")),
            isError = true,
        )
    }

    val sourcePosts = jsonPlaceholderApiClient.fetchPosts(limit = max(count, 100))
    val selectedPosts = sourcePosts
        .shuffled(random)
        .take(count)
        .map { post ->
            SummaryPost(
                userId = post.userId,
                id = post.id,
                title = post.title,
                body = post.body,
            )
        }
    val selection = PostSelection(posts = selectedPosts)

    return CallToolResult(
        content = listOf(
            TextContent(
                buildString {
                    appendLine("Случайные публикации: ${selectedPosts.size}")
                    selectedPosts.forEachIndexed { index, post ->
                        append("${index + 1}. #${post.id} ${post.title}")
                        if (index != selectedPosts.lastIndex) {
                            appendLine()
                        }
                    }
                }.trim(),
            ),
        ),
        isError = false,
        structuredContent = summaryPipelineJson.parseToJsonElement(
            summaryPipelineJson.encodeToString(selection),
        ).jsonObject,
    )
}

private fun JsonObject?.requiredIntArgument(name: String): Int? =
    this?.get(name)
        ?.jsonPrimitive
        ?.contentOrNull
        ?.toIntOrNull()
