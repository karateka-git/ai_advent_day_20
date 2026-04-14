package ru.compadre.mcp.mcp.server.stateful.toolcall.tools.startrandomposts

import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import ru.compadre.mcp.mcp.server.stateful.background.RandomPostTickerService
import ru.compadre.mcp.mcp.server.stateful.subscriptions.RandomPostSubscriptionRegistry

private const val DEFAULT_INTERVAL_MINUTES = 5
private const val MINIMUM_INTERVAL_MINUTES = 1

internal fun startRandomPostsToolSchema(): ToolSchema = ToolSchema(
    properties = buildJsonObject {
        putJsonObject("intervalMinutes") {
            put("type", "integer")
            put("minimum", MINIMUM_INTERVAL_MINUTES)
            put("description", "Интервал в минутах между push-отправками случайных публикаций. Минимум 1, по умолчанию 5.")
        }
    },
)

internal suspend fun ClientConnection.startRandomPostsToolResult(
    arguments: JsonObject?,
    subscriptionRegistry: RandomPostSubscriptionRegistry,
    tickerService: RandomPostTickerService,
): CallToolResult {
    val requestedInterval = arguments.optionalIntArgument("intervalMinutes") ?: DEFAULT_INTERVAL_MINUTES
    if (requestedInterval < MINIMUM_INTERVAL_MINUTES) {
        return CallToolResult(
            content = listOf(TextContent("Для инструмента start_random_posts требуется `intervalMinutes >= 1`.")),
            isError = true,
        )
    }

    tickerService.startOrUpdate(
        sessionId = sessionId,
        intervalMinutes = requestedInterval,
    )
    val subscription = subscriptionRegistry.find(sessionId)
        ?: error("Random post subscription was not stored for session `$sessionId`.")

    return CallToolResult(
        content = listOf(
            TextContent(
                "Random posts push активирован для текущей сессии. Интервал: ${subscription.intervalMinutes} мин.",
            ),
        ),
        isError = false,
    )
}

private fun JsonObject?.optionalIntArgument(name: String): Int? =
    this
        ?.get(name)
        ?.jsonPrimitive
        ?.intOrNull
