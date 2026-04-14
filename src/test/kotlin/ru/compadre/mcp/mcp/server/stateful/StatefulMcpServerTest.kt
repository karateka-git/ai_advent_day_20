package ru.compadre.mcp.mcp.server.stateful

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.JsonPlaceholderApiClient
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.common.models.JsonPlaceholderPost

class StatefulMcpServerTest {
    @Test
    fun statefulServerRegistersExpectedTools() {
        val server = createStatefulMcpServer(
            jsonPlaceholderApiClient = object : JsonPlaceholderApiClient {
                override suspend fun fetchPost(postId: Int): JsonPlaceholderPost? = null

                override suspend fun fetchPosts(limit: Int): List<JsonPlaceholderPost> = emptyList()
            },
        )

        assertEquals(
            setOf(
                "start_random_posts",
                "pick_random_posts",
                "merge_posts",
                "save_summary",
                "list_saved_summaries",
                "get_saved_summary",
            ),
            server.tools.keys,
        )
        assertTrue(server.tools["start_random_posts"] != null)
        assertTrue(server.tools["pick_random_posts"] != null)
        assertTrue(server.tools["merge_posts"] != null)
        assertTrue(server.tools["save_summary"] != null)
        assertTrue(server.tools["list_saved_summaries"] != null)
        assertTrue(server.tools["get_saved_summary"] != null)
    }
}
