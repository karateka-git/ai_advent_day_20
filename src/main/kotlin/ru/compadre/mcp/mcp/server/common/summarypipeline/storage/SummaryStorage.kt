package ru.compadre.mcp.mcp.server.common.summarypipeline.storage

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Clock
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.json.Json
import ru.compadre.mcp.mcp.server.common.summarypipeline.models.SavedSummary
import ru.compadre.mcp.mcp.server.common.summarypipeline.models.SummaryDraft

internal interface SummaryStorage {
    fun save(draft: SummaryDraft): SavedSummary

    fun list(): List<SavedSummary>
}

internal class FileSummaryStorage(
    private val storagePath: Path = defaultSummaryStoragePath(),
    private val clock: Clock = Clock.systemUTC(),
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    },
) : SummaryStorage {
    override fun save(draft: SummaryDraft): SavedSummary {
        val savedSummary = SavedSummary(
            summaryId = UUID.randomUUID().toString(),
            savedAt = Instant.now(clock).toString(),
            title = draft.title,
            content = draft.content,
            sourcePostIds = draft.sourcePostIds,
            strategy = draft.strategy,
        )
        val current = readAll()
        writeAll(current + savedSummary)
        return savedSummary
    }

    override fun list(): List<SavedSummary> = readAll()

    private fun readAll(): List<SavedSummary> {
        if (Files.notExists(storagePath)) {
            return emptyList()
        }

        val raw = Files.readString(storagePath)
        if (raw.isBlank()) {
            return emptyList()
        }

        return json.decodeFromString(raw)
    }

    private fun writeAll(summaries: List<SavedSummary>) {
        storagePath.parent?.let { Files.createDirectories(it) }
        Files.writeString(
            storagePath,
            json.encodeToString(summaries),
        )
    }
}

internal fun defaultSummaryStoragePath(): Path = Paths.get(
    "build",
    "tmp",
    "mcp-summary-storage",
    "saved-summaries.json",
)
