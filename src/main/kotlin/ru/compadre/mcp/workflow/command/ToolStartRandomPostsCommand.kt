package ru.compadre.mcp.workflow.command

/**
 * Команда запуска или обновления stateful random-post push.
 */
data class ToolStartRandomPostsCommand(
    val intervalMinutes: Int? = null,
) : Command
