package ru.compadre.mcp.mcp.server.stateless

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.JsonPlaceholderApiClient
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.common.models.JsonPlaceholderPost
import ru.compadre.mcp.mcp.server.common.summarypipeline.models.SavedSummary
import ru.compadre.mcp.mcp.server.common.summarypipeline.models.SummaryDraft
import ru.compadre.mcp.mcp.server.common.summarypipeline.storage.SummaryStorage

class StatelessMcpServerTest {
    @Test
    fun serverRegistersExpectedTools() {
        val server = createStatelessMcpServer(
            jsonPlaceholderApiClient = object : JsonPlaceholderApiClient {
                override suspend fun fetchPost(postId: Int): JsonPlaceholderPost? = null

                override suspend fun fetchPosts(limit: Int): List<JsonPlaceholderPost> = emptyList()
            },
            summaryStorage = object : SummaryStorage {
                override fun save(draft: SummaryDraft): SavedSummary = error("not used")

                override fun list(): List<SavedSummary> = emptyList()
            },
        )

        assertEquals(
            setOf(
                "ping",
                "echo",
                "fetch_post",
                "list_posts",
                "pick_random_posts",
                "merge_posts",
                "save_summary",
                "list_saved_summaries",
            ),
            server.tools.keys,
        )
        assertTrue(server.tools["ping"] != null)
        assertTrue(server.tools["echo"] != null)
        assertTrue(server.tools["fetch_post"] != null)
        assertTrue(server.tools["list_posts"] != null)
        assertTrue(server.tools["pick_random_posts"] != null)
        assertTrue(server.tools["merge_posts"] != null)
        assertTrue(server.tools["save_summary"] != null)
        assertTrue(server.tools["list_saved_summaries"] != null)
    }
}
