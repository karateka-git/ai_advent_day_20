package ru.compadre.mcp.mcp.server.common.summarypipeline.storage

import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ru.compadre.mcp.mcp.server.common.summarypipeline.models.SummaryDraft

class FileSummaryStorageTest {
    @Test
    fun storagePersistsSavedSummariesToFile() {
        val tempFile = Files.createTempDirectory("summary-storage-test")
            .resolve("summaries.json")
        val storage = FileSummaryStorage(
            storagePath = tempFile,
            clock = Clock.fixed(Instant.parse("2026-04-14T12:00:00Z"), ZoneOffset.UTC),
        )

        val savedSummary = storage.save(
            SummaryDraft(
                title = "Summary 1",
                content = "Test content",
                sourcePostIds = listOf(1, 2, 3),
                strategy = "long",
            ),
        )

        val allSummaries = storage.list()

        assertEquals(1, allSummaries.size)
        assertEquals(savedSummary, allSummaries.single())
        assertEquals("2026-04-14T12:00:00Z", savedSummary.savedAt)
        assertTrue(Files.exists(tempFile))
    }
}
