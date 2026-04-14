package ru.compadre.mcp.mcp.server.stateful.subscriptions

import java.util.concurrent.ConcurrentHashMap

/**
 * Хранит по одной active random-post подписке на клиентскую stateful-сессию.
 */
class RandomPostSubscriptionRegistry {
    private val subscriptions = ConcurrentHashMap<String, RandomPostSubscription>()

    fun startOrUpdate(sessionId: String, intervalMinutes: Int): RandomPostSubscription {
        val subscription = RandomPostSubscription(
            sessionId = sessionId,
            intervalMinutes = intervalMinutes,
        )
        subscriptions[sessionId] = subscription
        return subscription
    }

    fun find(sessionId: String): RandomPostSubscription? = subscriptions[sessionId]

    fun all(): List<RandomPostSubscription> = subscriptions.values.toList()

    fun remove(sessionId: String) {
        subscriptions.remove(sessionId)
    }

    fun size(): Int = subscriptions.size
}
