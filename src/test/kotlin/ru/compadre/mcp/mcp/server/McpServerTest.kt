package ru.compadre.mcp.mcp.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ru.compadre.mcp.mcp.server.fetchpost.JsonPlaceholderPost
import ru.compadre.mcp.mcp.server.fetchpost.PostLookupClient

class McpServerTest {
    @Test
    fun serverRegistersExpectedTools() {
        val server = createMcpServer(
            postLookupClient = object : PostLookupClient {
                override suspend fun fetchPost(postId: Int): JsonPlaceholderPost? = null
            },
        )

        assertEquals(setOf("ping", "echo", "fetch_post"), server.tools.keys)
        assertTrue(server.tools["ping"] != null)
        assertTrue(server.tools["echo"] != null)
        assertTrue(server.tools["fetch_post"] != null)
    }
}
