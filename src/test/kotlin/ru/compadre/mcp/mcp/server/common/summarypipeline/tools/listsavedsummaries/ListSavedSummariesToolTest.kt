package ru.compadre.mcp.mcp.server.common.summarypipeline.tools.listsavedsummaries

import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.json.jsonArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import ru.compadre.mcp.mcp.server.common.summarypipeline.models.SavedSummary
import ru.compadre.mcp.mcp.server.common.summarypipeline.models.SummaryDraft
import ru.compadre.mcp.mcp.server.common.summarypipeline.storage.SummaryStorage

class ListSavedSummariesToolTest {
    @Test
    fun listSavedSummariesReturnsStructuredList() {
        val result = listSavedSummariesToolResult(
            summaryStorage = object : SummaryStorage {
                override fun save(draft: SummaryDraft): SavedSummary = error("save should not be called")

                override fun list(): List<SavedSummary> = listOf(
                    SavedSummary(
                        summaryId = "summary-1",
                        savedAt = "2026-04-14T12:00:00Z",
                        title = "Summary title",
                        content = "Summary content",
                        sourcePostIds = listOf(1, 2, 3),
                        strategy = "long",
                    ),
                )
            },
        )

        assertEquals(false, result.isError)
        assertIs<TextContent>(result.content.single())
        val structuredContent = assertNotNull(result.structuredContent)
        assertEquals(1, structuredContent.getValue("summaries").jsonArray.size)
    }
}
