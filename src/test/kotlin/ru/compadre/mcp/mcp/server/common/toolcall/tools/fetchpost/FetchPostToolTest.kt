package ru.compadre.mcp.mcp.server.common.toolcall.tools.fetchpost

import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.JsonPlaceholderApiClient
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.common.models.JsonPlaceholderPost

class FetchPostToolTest {
    @Test
    fun fetchPostToolReturnsFormattedPostOnSuccessfulLookup() = runBlocking {
        val result = fetchPostToolResult(
            arguments = buildJsonObject {
                put("postId", 1)
            },
            jsonPlaceholderApiClient = object : JsonPlaceholderApiClient {
                override suspend fun fetchPost(postId: Int): JsonPlaceholderPost = JsonPlaceholderPost(
                    userId = 7,
                    id = postId,
                    title = "Тестовый заголовок",
                    body = "Тестовый текст",
                )

                override suspend fun fetchPosts(limit: Int): List<JsonPlaceholderPost> = emptyList()
            },
        )

        assertEquals(false, result.isError)
        val content = assertIs<TextContent>(result.content.single())
        assertEquals(
            listOf(
                "Публикация #1",
                "Автор: 7",
                "Заголовок: Тестовый заголовок",
                "Текст: Тестовый текст",
            ).joinToString(separator = System.lineSeparator()),
            content.text,
        )
    }

    @Test
    fun fetchPostToolReturnsValidationErrorWhenPostIdIsMissing() = runBlocking {
        val result = fetchPostToolResult(
            arguments = buildJsonObject {},
            jsonPlaceholderApiClient = object : JsonPlaceholderApiClient {
                override suspend fun fetchPost(postId: Int): JsonPlaceholderPost? {
                    error("Клиент не должен вызываться при ошибке валидации.")
                }

                override suspend fun fetchPosts(limit: Int): List<JsonPlaceholderPost> = emptyList()
            },
        )

        assertEquals(true, result.isError)
        val content = assertIs<TextContent>(result.content.single())
        assertEquals(
            "Для инструмента fetch_post требуется числовой аргумент `postId`.",
            content.text,
        )
    }
}
