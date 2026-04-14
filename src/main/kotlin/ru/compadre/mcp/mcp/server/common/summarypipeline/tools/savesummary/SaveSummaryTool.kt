package ru.compadre.mcp.mcp.server.common.summarypipeline.tools.savesummary

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
import ru.compadre.mcp.mcp.server.common.summarypipeline.storage.SummaryStorage
import ru.compadre.mcp.mcp.server.common.summarypipeline.tools.summaryPipelineJson

fun saveSummaryToolSchema(): ToolSchema = ToolSchema(
    properties = buildJsonObject {
        putJsonObject("title") {
            put("type", "string")
        }
        putJsonObject("content") {
            put("type", "string")
        }
        putJsonObject("strategy") {
            put("type", "string")
        }
        putJsonObject("sourcePostIds") {
            put("type", "array")
        }
    },
    required = listOf("title", "content", "strategy", "sourcePostIds"),
)

fun saveSummaryToolOutputSchema(): ToolSchema = ToolSchema(
    properties = buildJsonObject {
        putJsonObject("summaryId") {
            put("type", "string")
        }
        putJsonObject("savedAt") {
            put("type", "string")
        }
        putJsonObject("strategy") {
            put("type", "string")
        }
        putJsonObject("sourcePostIds") {
            put("type", "array")
        }
    },
    required = listOf("summaryId", "savedAt", "strategy", "sourcePostIds"),
)

internal fun saveSummaryToolResult(
    arguments: JsonObject?,
    summaryStorage: SummaryStorage,
): CallToolResult {
    val title = arguments.requiredStringArgument("title")
        ?: return missingStringArgument("title")
    val content = arguments.requiredStringArgument("content")
        ?: return missingStringArgument("content")
    val strategy = arguments.requiredStringArgument("strategy")
        ?: return missingStringArgument("strategy")
    val sourcePostIds = arguments.requiredIntListArgument("sourcePostIds")
        ?: return CallToolResult(
            content = listOf(TextContent("Для инструмента `save_summary` требуется массив целочисленных `sourcePostIds`.")),
            isError = true,
        )

    val savedSummary = summaryStorage.save(
        SummaryDraft(
            title = title,
            content = content,
            sourcePostIds = sourcePostIds,
            strategy = strategy,
        ),
    )

    return CallToolResult(
        content = listOf(TextContent("Summary сохранён: ${savedSummary.summaryId} (${savedSummary.savedAt})")),
        isError = false,
        structuredContent = summaryPipelineJson.parseToJsonElement(
            summaryPipelineJson.encodeToString(savedSummary),
        ).jsonObject,
    )
}

private fun missingStringArgument(argumentName: String): CallToolResult = CallToolResult(
    content = listOf(TextContent("Для инструмента `save_summary` требуется строковый аргумент `$argumentName`.")),
    isError = true,
)

private fun JsonObject?.requiredStringArgument(name: String): String? =
    this?.get(name)
        ?.jsonPrimitive
        ?.contentOrNull
        ?.takeIf { it.isNotBlank() }

private fun JsonObject?.requiredIntListArgument(name: String): List<Int>? =
    this?.get(name)
        ?.jsonArray
        ?.mapNotNull { element -> element.jsonPrimitive.contentOrNull?.toIntOrNull() }
