package ru.compadre.mcp.mcp.server.common.summarypipeline.tools.listsavedsummaries

import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import ru.compadre.mcp.mcp.server.common.summarypipeline.storage.SummaryStorage
import ru.compadre.mcp.mcp.server.common.summarypipeline.tools.summaryPipelineJson

fun listSavedSummariesToolSchema(): ToolSchema = ToolSchema()

fun listSavedSummariesToolOutputSchema(): ToolSchema = ToolSchema(
    properties = buildJsonObject {
        putJsonObject("summaries") {
            put("type", "array")
            put("description", "Список всех сохранённых summary.")
        }
    },
    required = listOf("summaries"),
)

internal fun listSavedSummariesToolResult(summaryStorage: SummaryStorage): CallToolResult {
    val summaries = summaryStorage.list()
    val content = if (summaries.isEmpty()) {
        "Сохранённые summary не найдены."
    } else {
        buildString {
            appendLine("Сохранённые summary: ${summaries.size}")
            summaries.forEachIndexed { index, summary ->
                append("${index + 1}. ${summary.title} [${summary.summaryId}]")
                if (index != summaries.lastIndex) {
                    appendLine()
                }
            }
        }
    }

    return CallToolResult(
        content = listOf(TextContent(content.trim())),
        isError = false,
        structuredContent = buildJsonObject {
            put(
                "summaries",
                summaryPipelineJson.parseToJsonElement(
                    summaryPipelineJson.encodeToString(summaries),
                ),
            )
        },
    )
}
