package ru.compadre.mcp.mcp.server.common.summarypipeline.models

import kotlinx.serialization.Serializable

@Serializable
data class SummaryPost(
    val userId: Int,
    val id: Int,
    val title: String,
    val body: String,
)

@Serializable
data class PostSelection(
    val posts: List<SummaryPost>,
)

@Serializable
data class SummaryDraft(
    val title: String,
    val content: String,
    val sourcePostIds: List<Int>,
    val strategy: String,
)

@Serializable
data class SavedSummary(
    val summaryId: String,
    val savedAt: String,
    val title: String,
    val content: String,
    val sourcePostIds: List<Int>,
    val strategy: String,
)
