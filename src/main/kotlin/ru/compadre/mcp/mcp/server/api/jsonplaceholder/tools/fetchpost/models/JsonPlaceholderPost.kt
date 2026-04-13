package ru.compadre.mcp.mcp.server.api.jsonplaceholder.tools.fetchpost.models

import kotlinx.serialization.Serializable

/**
 * Публикация из mock API `JSONPlaceholder`.
 *
 * @property userId идентификатор автора публикации
 * @property id идентификатор публикации
 * @property title заголовок публикации
 * @property body текст публикации
 */
@Serializable
internal data class JsonPlaceholderPost(
    val userId: Int,
    val id: Int,
    val title: String,
    val body: String,
)
