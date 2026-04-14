package ru.compadre.mcp.presentation.cli

import kotlin.test.Test
import kotlin.test.assertEquals
import ru.compadre.mcp.workflow.result.AgentPreparationResult
import ru.compadre.mcp.workflow.result.AvailableCliCommandResult
import ru.compadre.mcp.workflow.result.ToolCallResult

class DefaultCliOutputFormatterTest {
    private val formatter = DefaultCliOutputFormatter()

    @Test
    fun formatRendersSuccessfulPreparationResult() {
        val result = AgentPreparationResult(
            prepared = true,
            availableCommands = listOf(
                AvailableCliCommandResult(
                    pattern = "tool posts",
                    description = "Показать первые 10 публикаций.",
                ),
                AvailableCliCommandResult(
                    pattern = "tool post <postId>",
                    description = "Получить публикацию по идентификатору.",
                ),
            ),
        )

        val expected = listOf(
            "Агент готов к работе.",
            "Доступные команды:",
            "tool posts - Показать первые 10 публикаций.",
            "tool post <postId> - Получить публикацию по идентификатору.",
        ).joinToString(System.lineSeparator())

        assertEquals(expected, formatter.format(result))
    }

    @Test
    fun formatRendersPreparationWarnings() {
        val result = AgentPreparationResult(
            prepared = true,
            warnings = listOf("Предупреждение: MCP-сервер `local_stateful_mcp_server` недоступен."),
            availableCommands = listOf(
                AvailableCliCommandResult(
                    pattern = "tool posts",
                    description = "Показать первые 10 публикаций.",
                ),
            ),
        )

        val expected = listOf(
            "Агент готов к работе.",
            "Предупреждение: MCP-сервер `local_stateful_mcp_server` недоступен.",
            "Доступные команды:",
            "tool posts - Показать первые 10 публикаций.",
        ).joinToString(System.lineSeparator())

        assertEquals(expected, formatter.format(result))
    }

    @Test
    fun formatRendersFailedPreparationResult() {
        val result = AgentPreparationResult(
            prepared = false,
            errorMessage = "boom",
        )

        val expected = listOf(
            "Не удалось подготовить агента.",
            "Ошибка: boom",
        ).joinToString(System.lineSeparator())

        assertEquals(expected, formatter.format(result))
    }

    @Test
    fun formatRendersSuccessfulToolCallResult() {
        val result = ToolCallResult(
            commandText = "tool post 1",
            successful = true,
            content = listOf(
                "Публикация #1",
                "Автор: 1",
                "Заголовок: Тестовый заголовок",
            ),
        )

        val expected = listOf(
            "Публикация #1",
            "Автор: 1",
            "Заголовок: Тестовый заголовок",
        ).joinToString(System.lineSeparator())

        assertEquals(expected, formatter.format(result))
    }

    @Test
    fun formatRendersFailedToolCallResult() {
        val result = ToolCallResult(
            commandText = "tool post 9999",
            successful = false,
            errorMessage = "Публикация не найдена.",
        )

        val expected = listOf(
            "Не удалось выполнить команду `tool post 9999`.",
            "Ошибка: Публикация не найдена.",
        ).joinToString(System.lineSeparator())

        assertEquals(expected, formatter.format(result))
    }
}
