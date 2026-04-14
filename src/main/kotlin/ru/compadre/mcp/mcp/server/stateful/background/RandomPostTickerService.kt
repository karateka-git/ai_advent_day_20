package ru.compadre.mcp.mcp.server.stateful.background

import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.BaseNotificationParams
import io.modelcontextprotocol.kotlin.sdk.types.CustomNotification
import io.modelcontextprotocol.kotlin.sdk.types.Method
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.JsonPlaceholderApiClient
import ru.compadre.mcp.mcp.server.stateful.subscriptions.RandomPostSubscriptionRegistry
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import ru.compadre.mcp.mcp.client.common.notifications.RANDOM_POST_NOTIFICATION_METHOD
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Управляет фоновыми session-bound задачами отправки случайных публикаций.
 */
internal class RandomPostTickerService(
    private val subscriptionRegistry: RandomPostSubscriptionRegistry,
    private val jsonPlaceholderApiClient: JsonPlaceholderApiClient,
    private val isSessionActive: (String) -> Boolean,
    private val connectionProvider: (String) -> ClientConnection?,
    private val intervalToDelay: (Int) -> Duration = { minutes -> minutes.minutes },
    private val randomPostIdProvider: () -> Int = { Random.nextInt(1, 101) },
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val jobs = mutableMapOf<String, Job>()

    fun startOrUpdate(sessionId: String, intervalMinutes: Int) {
        subscriptionRegistry.startOrUpdate(sessionId, intervalMinutes)
        jobs.remove(sessionId)?.cancel()
        jobs[sessionId] = scope.launch {
            while (true) {
                delay(intervalToDelay(intervalMinutes))

                if (!isSessionActive(sessionId)) {
                    stop(sessionId)
                    break
                }

                val connection = connectionProvider(sessionId)
                if (connection == null) {
                    stop(sessionId)
                    break
                }

                val message = runCatching {
                    val postId = randomPostIdProvider()
                    val post = jsonPlaceholderApiClient.fetchPost(postId)
                    if (post == null) {
                        "Случайная публикация не найдена для postId=$postId."
                    } else {
                        "Случайная публикация #${post.id}: ${post.title}"
                    }
                }.getOrElse { error ->
                    error.message ?: "Не удалось получить случайную публикацию."
                }

                val delivered = runCatching {
                    connection.notification(
                        CustomNotification(
                            method = Method.Custom(RANDOM_POST_NOTIFICATION_METHOD),
                            params = BaseNotificationParams(
                                meta = buildJsonObject {
                                    put("message", message)
                                },
                            ),
                        ),
                    )
                }.isSuccess

                if (!delivered) {
                    stop(sessionId)
                    break
                }
            }
        }
    }

    fun stop(sessionId: String) {
        jobs.remove(sessionId)?.cancel()
        subscriptionRegistry.remove(sessionId)
    }

    fun close() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        scope.cancel()
    }
}
