package ru.compadre.mcp.agent.bootstrap.models

/**
 * Стабильный типизированный идентификатор пользовательской команды агента.
 */
enum class AgentCommandId {
    TOOL_POSTS,
    TOOL_POST,
    TOOL_START_RANDOM_POSTS,
    TOOL_SUMMARY_POSTS,
    TOOL_SUMMARIES;

    /**
     * Возвращает стабильное строковое представление команды для сообщений и логики совместимости.
     */
    fun value(): String = when (this) {
        TOOL_POSTS -> "tool.posts"
        TOOL_POST -> "tool.post"
        TOOL_START_RANDOM_POSTS -> "tool.start-random-posts"
        TOOL_SUMMARY_POSTS -> "tool.summary.posts"
        TOOL_SUMMARIES -> "tool.summaries"
    }
}
