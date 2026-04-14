package ru.compadre.mcp.mcp.server.stateful.subscriptions

/**
 * Session-bound настройка периодической отправки случайных публикаций.
 */
data class RandomPostSubscription(
    val sessionId: String,
    val intervalMinutes: Int,
)
