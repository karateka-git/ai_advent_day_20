package ru.compadre.mcp.mcp.server.common.summarypipeline.tools.pickrandomposts

import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.JsonPlaceholderApiClient
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.common.models.JsonPlaceholderPost

class PickRandomPostsToolTest {
    @Test
    fun pickRandomPostsReturnsStructuredSelection() = runBlocking {
        val result = pickRandomPostsToolResult(
            arguments = buildJsonObject {
                put("count", 3)
            },
            jsonPlaceholderApiClient = object : JsonPlaceholderApiClient {
                override suspend fun fetchPost(postId: Int): JsonPlaceholderPost? = null

                override suspend fun fetchPosts(limit: Int): List<JsonPlaceholderPost> = (1..5).map { id ->
                    JsonPlaceholderPost(
                        userId = 1,
                        id = id,
                        title = "Post $id",
                        body = "Body $id",
                    )
                }
            },
            random = Random(42),
        )

        assertEquals(false, result.isError)
        assertIs<TextContent>(result.content.single())
        val structuredContent = assertNotNull(result.structuredContent)
        assertEquals(3, structuredContent.getValue("posts").jsonArray.size)
    }
}
