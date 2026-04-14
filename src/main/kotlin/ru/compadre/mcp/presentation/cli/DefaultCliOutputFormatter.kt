package ru.compadre.mcp.presentation.cli

import ru.compadre.mcp.workflow.result.AgentPreparationResult
import ru.compadre.mcp.workflow.result.CommandResult
import ru.compadre.mcp.workflow.result.ToolCallResult

/**
 * Стандартный форматтер workflow-результатов для CLI.
 */
class DefaultCliOutputFormatter : CliOutputFormatter {
    override fun format(result: CommandResult): String = when (result) {
        is AgentPreparationResult -> formatPreparationResult(result)
        is ToolCallResult -> formatToolCallResult(result)
    }

    /**
     * Форматирует snapshot доступных пользователю команд после подготовки агента.
     */
    private fun formatPreparationResult(result: AgentPreparationResult): String {
        if (!result.prepared) {
            return buildList {
                add("Не удалось подготовить агента.")
                add("Ошибка: ${result.errorMessage ?: "<неизвестно>"}")
            }.joinToString(separator = System.lineSeparator())
        }

        return buildList {
            add("Агент готов к работе.")
            result.warnings.forEach { warning ->
                add(warning)
            }

            if (result.availableCommands.isEmpty()) {
                add("Пользовательские команды не найдены.")
            } else {
                add("Доступные команды:")
                result.availableCommands.forEach { command ->
                    add("${command.pattern} - ${command.description}")
                }
            }
        }.joinToString(separator = System.lineSeparator())
    }

    /**
     * Форматирует результат вызова прикладной команды без показа server-level деталей маршрутизации.
     */
    private fun formatToolCallResult(result: ToolCallResult): String {
        if (!result.successful) {
            return buildList {
                add("Не удалось выполнить команду `${result.commandText}`.")
                add("Ошибка: ${result.errorMessage ?: "<неизвестно>"}")
            }.joinToString(separator = System.lineSeparator())
        }

        return result.content.joinToString(separator = System.lineSeparator())
    }
}
