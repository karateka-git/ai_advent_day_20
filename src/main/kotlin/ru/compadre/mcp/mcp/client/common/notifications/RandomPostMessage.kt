package ru.compadre.mcp.mcp.client.common.notifications

const val RANDOM_POST_NOTIFICATION_METHOD: String = "notifications/random_post"

data class RandomPostMessage(
    val message: String,
)
