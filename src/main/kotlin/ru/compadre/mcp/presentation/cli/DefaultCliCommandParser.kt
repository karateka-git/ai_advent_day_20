package ru.compadre.mcp.presentation.cli

import ru.compadre.mcp.workflow.command.Command
import ru.compadre.mcp.workflow.command.ToolPostCommand
import ru.compadre.mcp.workflow.command.ToolPostsCommand
import ru.compadre.mcp.workflow.command.ToolStartRandomPostsCommand
import ru.compadre.mcp.workflow.command.ToolSummariesCommand
import ru.compadre.mcp.workflow.command.ToolSummaryPostsCommand

/**
 * Стандартный CLI-разборщик пользовательских команд проекта.
 */
class DefaultCliCommandParser : CliCommandParser {
    override fun parse(args: Array<String>): Command {
        val rawCommand = args.firstOrNull()
            ?.trim()
            ?.trimStart('\uFEFF')
            ?.lowercase()
            ?: throw IllegalArgumentException(
                "Команда не указана. Поддерживаемые команды: $supportedCommandsDescription",
            )

        return when (rawCommand) {
            "tool" -> parseToolCommand(args)
            else -> throw IllegalArgumentException(
                "Неизвестная команда клиента: `$rawCommand`. Поддерживаемые команды: $supportedCommandsDescription",
            )
        }
    }

    private fun parseToolCommand(args: Array<String>): Command {
        val toolName = args.getOrNull(1)
            ?.trim()
            ?.lowercase()
            ?: throw IllegalArgumentException(
                "Не указано имя tool-команды. Поддерживаемые форматы: $supportedCommandsDescription",
            )

        return when (toolName) {
            "posts" -> ToolPostsCommand
            "post" -> {
                val postId = args.getOrNull(2)?.toIntOrNull()
                    ?: throw IllegalArgumentException(
                        "Для команды `tool post` требуется числовой аргумент `<postId>`.",
                    )

                ToolPostCommand(postId = postId)
            }

            "start-random-posts" -> {
                val intervalMinutes = args.getOrNull(2)?.let { rawInterval ->
                    val parsedInterval = rawInterval.toIntOrNull()
                        ?: throw IllegalArgumentException(
                            "Для команды `tool start-random-posts` аргумент `[intervalMinutes]` должен быть целым числом.",
                        )

                    require(parsedInterval >= 1) {
                        "Для команды `tool start-random-posts` аргумент `[intervalMinutes]` должен быть не меньше 1."
                    }
                    parsedInterval
                }

                ToolStartRandomPostsCommand(intervalMinutes = intervalMinutes)
            }

            "summary" -> parseSummaryCommand(args)
            "summaries" -> ToolSummariesCommand
            else -> throw IllegalArgumentException(
                "Неизвестная tool-команда: `$toolName`. Поддерживаемые форматы: $supportedCommandsDescription",
            )
        }
    }

    private fun parseSummaryCommand(args: Array<String>): Command {
        val target = args.getOrNull(2)
            ?.trim()
            ?.lowercase()
            ?: throw IllegalArgumentException(
                "Для команды `tool summary` требуется указать целевой сценарий. Поддерживаемый формат: `tool summary posts <count> [strategy]`.",
            )

        return when (target) {
            "posts" -> {
                val count = args.getOrNull(3)?.toIntOrNull()
                    ?: throw IllegalArgumentException(
                        "Для команды `tool summary posts` требуется числовой аргумент `<count>`.",
                    )
                require(count >= 1) {
                    "Для команды `tool summary posts` аргумент `<count>` должен быть не меньше 1."
                }

                val strategy = args.getOrNull(4)
                    ?.trim()
                    ?.lowercase()
                    ?: "long"
                require(strategy in supportedStrategies) {
                    "Для команды `tool summary posts` стратегия должна быть одной из: ${supportedStrategies.joinToString()}."
                }

                ToolSummaryPostsCommand(
                    count = count,
                    strategy = strategy,
                )
            }

            else -> throw IllegalArgumentException(
                "Неизвестный сценарий для `tool summary`: `$target`. Поддерживаемый формат: `tool summary posts <count> [strategy]`.",
            )
        }
    }

    private companion object {
        private val supportedStrategies = setOf("long", "short")
        private const val supportedCommandsDescription =
            "tool posts, tool post <postId>, tool start-random-posts [intervalMinutes], tool summary posts <count> [strategy], tool summaries"
    }
}
