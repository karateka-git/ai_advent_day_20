package ru.compadre.mcp.mcp.server.common.toolcall.tools.listposts

import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.JsonPlaceholderApiClient
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.common.models.JsonPlaceholderPost

class ListPostsToolTest {
    @Test
    fun listPostsToolReturnsFirstPostsWithStructuredContent() = runBlocking {
        val result = listPostsToolResult(
            jsonPlaceholderApiClient = object : JsonPlaceholderApiClient {
                override suspend fun fetchPost(postId: Int): JsonPlaceholderPost? = null

                override suspend fun fetchPosts(limit: Int): List<JsonPlaceholderPost> = listOf(
                    JsonPlaceholderPost(
                        userId = 1,
                        id = 1,
                        title = "Первый пост",
                        body = "Текст 1",
                    ),
                    JsonPlaceholderPost(
                        userId = 1,
                        id = 2,
                        title = "Второй пост",
                        body = "Текст 2",
                    ),
                )
            },
        )

        assertEquals(false, result.isError)
        val content = assertIs<TextContent>(result.content.single())
        assertEquals(
            listOf(
                "Первые публикации (2):",
                "1. Первый пост",
                "2. Второй пост",
            ).joinToString(separator = System.lineSeparator()),
            content.text,
        )

        val structuredContent = assertNotNull(result.structuredContent)
        val posts = structuredContent["posts"]?.jsonArray
        assertEquals(2, posts?.size)
        assertEquals("1", posts?.get(0)?.jsonObject?.get("id")?.jsonPrimitive?.content)
        assertEquals("Текст 2", posts?.get(1)?.jsonObject?.get("body")?.jsonPrimitive?.content)
    }
}
