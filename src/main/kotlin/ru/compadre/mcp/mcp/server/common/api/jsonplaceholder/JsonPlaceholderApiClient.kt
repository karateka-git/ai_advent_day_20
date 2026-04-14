package ru.compadre.mcp.mcp.server.common.api.jsonplaceholder

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.common.models.JsonPlaceholderPost

/**
 * Контракт доступа к внешнему API `JSONPlaceholder`.
 */
internal interface JsonPlaceholderApiClient {
    /**
     * Возвращает публикацию по её идентификатору.
     *
     * @param postId идентификатор публикации
     * @return найденная публикация или `null`, если публикация не существует
     */
    suspend fun fetchPost(postId: Int): JsonPlaceholderPost?

    /**
     * Возвращает первые публикации из API `JSONPlaceholder`.
     *
     * @param limit максимальное количество публикаций
     * @return список публикаций
     */
    suspend fun fetchPosts(limit: Int): List<JsonPlaceholderPost>
}

/**
 * Стандартная HTTP-реализация клиента к API `JSONPlaceholder`.
 */
internal class DefaultJsonPlaceholderApiClient(
    private val baseUrl: String = "https://jsonplaceholder.typicode.com",
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    },
) : JsonPlaceholderApiClient {
    override suspend fun fetchPost(postId: Int): JsonPlaceholderPost? {
        val response = httpClient.get("$baseUrl/posts/$postId")

        return when (response.status) {
            HttpStatusCode.OK -> response.body<JsonPlaceholderPost>()
            HttpStatusCode.NotFound -> null
            else -> error(
                "Mock API вернул неожиданный статус `${response.status.value}` для публикации `$postId`.",
            )
        }
    }

    override suspend fun fetchPosts(limit: Int): List<JsonPlaceholderPost> {
        val response = httpClient.get("$baseUrl/posts") {
            parameter("_limit", limit)
        }

        return when (response.status) {
            HttpStatusCode.OK -> response.body<List<JsonPlaceholderPost>>()
            else -> error(
                "Mock API вернул неожиданный статус `${response.status.value}` при запросе списка публикаций.",
            )
        }
    }
}
