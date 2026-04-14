package ru.compadre.mcp.mcp.server.stateful.background

import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.BaseNotificationParams
import io.modelcontextprotocol.kotlin.sdk.types.CreateMessageRequest
import io.modelcontextprotocol.kotlin.sdk.types.CreateMessageResult
import io.modelcontextprotocol.kotlin.sdk.types.CustomNotification
import io.modelcontextprotocol.kotlin.sdk.types.ElicitRequest
import io.modelcontextprotocol.kotlin.sdk.types.ElicitRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.ElicitResult
import io.modelcontextprotocol.kotlin.sdk.types.EmptyResult
import io.modelcontextprotocol.kotlin.sdk.types.ListRootsRequest
import io.modelcontextprotocol.kotlin.sdk.types.ListRootsResult
import io.modelcontextprotocol.kotlin.sdk.types.LoggingMessageNotification
import io.modelcontextprotocol.kotlin.sdk.types.ProgressToken
import io.modelcontextprotocol.kotlin.sdk.types.RequestId
import io.modelcontextprotocol.kotlin.sdk.types.ResourceUpdatedNotification
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import ru.compadre.mcp.mcp.client.common.notifications.RANDOM_POST_NOTIFICATION_METHOD
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.JsonPlaceholderApiClient
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.common.models.JsonPlaceholderPost
import ru.compadre.mcp.mcp.server.stateful.subscriptions.RandomPostSubscriptionRegistry
import kotlin.time.Duration.Companion.milliseconds

class RandomPostTickerServiceTest {
    @Test
    fun tickerSendsRandomPostNotificationToActiveSession() = runBlocking {
        val subscriptionRegistry = RandomPostSubscriptionRegistry()
        val sentNotifications = mutableListOf<CustomNotification>()
        val service = RandomPostTickerService(
            subscriptionRegistry = subscriptionRegistry,
            jsonPlaceholderApiClient = object : JsonPlaceholderApiClient {
                override suspend fun fetchPost(postId: Int): JsonPlaceholderPost? = JsonPlaceholderPost(
                    userId = 1,
                    id = postId,
                    title = "title $postId",
                    body = "body",
                )

                override suspend fun fetchPosts(limit: Int): List<JsonPlaceholderPost> = emptyList()
            },
            isSessionActive = { it == "session-1" },
            connectionProvider = {
                if (it == "session-1") {
                    object : ClientConnection {
                        override val sessionId: String = "session-1"

                        override suspend fun notification(
                            notification: io.modelcontextprotocol.kotlin.sdk.types.ServerNotification,
                            relatedRequestId: RequestId?,
                        ) {
                            sentNotifications += assertIs<CustomNotification>(notification)
                        }

                        override suspend fun ping(
                            request: io.modelcontextprotocol.kotlin.sdk.types.PingRequest,
                            options: io.modelcontextprotocol.kotlin.sdk.shared.RequestOptions?,
                        ): EmptyResult = EmptyResult()

                        override suspend fun createMessage(
                            request: CreateMessageRequest,
                            options: io.modelcontextprotocol.kotlin.sdk.shared.RequestOptions?,
                        ): CreateMessageResult = throw UnsupportedOperationException()

                        override suspend fun listRoots(
                            request: ListRootsRequest,
                            options: io.modelcontextprotocol.kotlin.sdk.shared.RequestOptions?,
                        ): ListRootsResult = throw UnsupportedOperationException()

                        override suspend fun createElicitation(
                            message: String,
                            requestedSchema: ElicitRequestParams.RequestedSchema,
                            options: io.modelcontextprotocol.kotlin.sdk.shared.RequestOptions?,
                        ): ElicitResult = throw UnsupportedOperationException()

                        override suspend fun createElicitation(
                            request: ElicitRequest,
                            options: io.modelcontextprotocol.kotlin.sdk.shared.RequestOptions?,
                        ): ElicitResult = throw UnsupportedOperationException()

                        override suspend fun sendLoggingMessage(notification: LoggingMessageNotification) = Unit

                        override suspend fun sendResourceUpdated(notification: ResourceUpdatedNotification) = Unit

                        override suspend fun sendResourceListChanged() = Unit

                        override suspend fun sendToolListChanged() = Unit

                        override suspend fun sendPromptListChanged() = Unit
                    }
                } else {
                    null
                }
            },
            intervalToDelay = { 50.milliseconds },
            randomPostIdProvider = { 42 },
        )

        try {
            service.startOrUpdate(sessionId = "session-1", intervalMinutes = 1)
            repeat(20) {
                if (sentNotifications.isNotEmpty()) return@repeat
                delay(25)
            }

            assertEquals(1, sentNotifications.size)
            assertEquals(RANDOM_POST_NOTIFICATION_METHOD, sentNotifications.single().method.value)
            assertEquals(
                "Случайная публикация #42: title 42",
                sentNotifications.single().meta?.get("message")?.jsonPrimitive?.content,
            )
            assertEquals(1, subscriptionRegistry.size())
        } finally {
            service.close()
        }
    }
}
