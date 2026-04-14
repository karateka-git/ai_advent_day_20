package ru.compadre.mcp.mcp.server.common.summarypipeline.tools.savesummary

import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import ru.compadre.mcp.mcp.server.common.summarypipeline.storage.FileSummaryStorage

class SaveSummaryToolTest {
    @Test
    fun saveSummaryPersistsSummaryAndReturnsMetadata() {
        val tempFile = Files.createTempDirectory("save-summary-tool-test")
            .resolve("summaries.json")
        val storage = FileSummaryStorage(
            storagePath = tempFile,
            clock = Clock.fixed(Instant.parse("2026-04-14T12:30:00Z"), ZoneOffset.UTC),
        )

        val result = saveSummaryToolResult(
            arguments = buildJsonObject {
                put("title", "Summary title")
                put("content", "Summary content")
                put("strategy", "short")
                putJsonArray("sourcePostIds") {
                    add(JsonPrimitive(7))
                    add(JsonPrimitive(8))
                    add(JsonPrimitive(9))
                }
            },
            summaryStorage = storage,
        )

        assertEquals(false, result.isError)
        assertIs<TextContent>(result.content.single())
        val structuredContent = assertNotNull(result.structuredContent)
        assertEquals("short", structuredContent.getValue("strategy").toString().trim('"'))
        assertEquals(3, structuredContent.getValue("sourcePostIds").jsonArray.size)
        assertEquals(1, storage.list().size)
    }
}
