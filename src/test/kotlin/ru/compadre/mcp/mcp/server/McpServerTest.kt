package ru.compadre.mcp.mcp.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ru.compadre.mcp.mcp.server.api.jsonplaceholder.JsonPlaceholderApiClient
import ru.compadre.mcp.mcp.server.api.jsonplaceholder.tools.fetchpost.models.JsonPlaceholderPost

class McpServerTest {
    @Test
    fun serverRegistersExpectedTools() {
        val server = createMcpServer(
            jsonPlaceholderApiClient = object : JsonPlaceholderApiClient {
                override suspend fun fetchPost(postId: Int): JsonPlaceholderPost? = null
            },
        )

        assertEquals(setOf("ping", "echo", "fetch_post"), server.tools.keys)
        assertTrue(server.tools["ping"] != null)
        assertTrue(server.tools["echo"] != null)
        assertTrue(server.tools["fetch_post"] != null)
    }
}
