package ru.compadre.mcp.presentation.cli

import ru.compadre.mcp.workflow.result.CommandResult
import ru.compadre.mcp.workflow.result.ConnectResult
import ru.compadre.mcp.workflow.result.ToolCallResult

/**
 * Стандартный форматтер workflow-результатов для CLI.
 */
class DefaultCliOutputFormatter : CliOutputFormatter {
    override fun format(result: CommandResult): String = when (result) {
        is ConnectResult -> formatConnectResult(result)
        is ToolCallResult -> formatUnsupportedToolCallResult(result)
    }

    private fun formatConnectResult(result: ConnectResult): String {
        if (!result.connected) {
            return buildList {
                add("Не удалось подключиться к MCP-серверу: ${result.endpoint}")
                add("Ошибка: ${result.errorMessage ?: "<неизвестно>"}")
            }.joinToString(separator = System.lineSeparator())
        }

        val lines = buildList {
            add("Подключение к MCP-серверу установлено: ${result.endpoint}")
            add("Имя сервера: ${result.serverName ?: "<неизвестно>"}")
            add("Версия сервера: ${result.serverVersion ?: "<неизвестно>"}")
            add("Заголовок сервера: ${result.serverTitle ?: "<неизвестно>"}")
            add("Инструкции сервера: ${result.serverInstructions ?: "<нет>"}")

            if (result.tools.isEmpty()) {
                add("Доступные инструменты: <нет>")
            } else {
                add("Доступные инструменты (${result.tools.size}):")
                result.tools.forEachIndexed { index, tool ->
                    val title = tool.title?.takeIf { it.isNotBlank() } ?: tool.name
                    val description = tool.description?.takeIf { it.isNotBlank() } ?: "Описание не указано."
                    add("${index + 1}. $title [${tool.name}] - $description")
                }
            }
        }

        return lines.joinToString(separator = System.lineSeparator())
    }

    private fun formatUnsupportedToolCallResult(result: ToolCallResult): String = buildList {
        add("Результат вызова инструмента `${result.toolName}` пока не поддержан в CLI formatter.")
        if (result.errorMessage != null) {
            add("Ошибка: ${result.errorMessage}")
        }
    }.joinToString(separator = System.lineSeparator())
}
