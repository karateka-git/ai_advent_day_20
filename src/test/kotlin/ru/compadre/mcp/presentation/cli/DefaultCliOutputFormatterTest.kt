package ru.compadre.mcp.presentation.cli

import kotlin.test.Test
import kotlin.test.assertEquals
import ru.compadre.mcp.workflow.result.ConnectResult
import ru.compadre.mcp.workflow.result.ConnectToolResult
import ru.compadre.mcp.workflow.result.ToolCallResult

class DefaultCliOutputFormatterTest {
    private val formatter = DefaultCliOutputFormatter()

    @Test
    fun formatRendersSuccessfulConnectResult() {
        val result = ConnectResult(
            endpoint = "http://127.0.0.1:3000/mcp",
            connected = true,
            serverName = "local_mcp_server",
            serverVersion = "0.1.0",
            serverTitle = "Local MCP Server",
            serverInstructions = "Локальный MCP server для sandbox-проекта.",
            tools = listOf(
                ConnectToolResult(
                    name = "ping",
                    title = "Ping",
                    description = "Возвращает короткий ответ сервера для проверки доступности.",
                ),
                ConnectToolResult(
                    name = "echo",
                    title = "Echo",
                    description = "Возвращает переданную строку обратно клиенту.",
                ),
            ),
        )

        val expected = listOf(
            "Подключение к MCP-серверу установлено: http://127.0.0.1:3000/mcp",
            "Имя сервера: local_mcp_server",
            "Версия сервера: 0.1.0",
            "Заголовок сервера: Local MCP Server",
            "Инструкции сервера: Локальный MCP server для sandbox-проекта.",
            "Доступные инструменты (2):",
            "1. Ping [ping] - Возвращает короткий ответ сервера для проверки доступности.",
            "2. Echo [echo] - Возвращает переданную строку обратно клиенту.",
        ).joinToString(System.lineSeparator())

        assertEquals(expected, formatter.format(result))
    }

    @Test
    fun formatRendersFailedConnectResult() {
        val result = ConnectResult(
            endpoint = "http://127.0.0.1:3000/mcp",
            connected = false,
            errorMessage = "boom",
        )

        val expected = listOf(
            "Не удалось подключиться к MCP-серверу: http://127.0.0.1:3000/mcp",
            "Ошибка: boom",
        ).joinToString(System.lineSeparator())

        assertEquals(expected, formatter.format(result))
    }

    @Test
    fun formatRendersSuccessfulToolCallResult() {
        val result = ToolCallResult(
            endpoint = "http://127.0.0.1:3000/mcp",
            toolName = "fetch_post",
            successful = true,
            content = listOf(
                "Публикация #1",
                "Автор: 1",
                "Заголовок: Тестовый заголовок",
            ),
        )

        val expected = listOf(
            "Инструмент `fetch_post` выполнен успешно: http://127.0.0.1:3000/mcp",
            "Публикация #1",
            "Автор: 1",
            "Заголовок: Тестовый заголовок",
        ).joinToString(System.lineSeparator())

        assertEquals(expected, formatter.format(result))
    }

    @Test
    fun formatRendersFailedToolCallResult() {
        val result = ToolCallResult(
            endpoint = "http://127.0.0.1:3000/mcp",
            toolName = "fetch_post",
            successful = false,
            errorMessage = "Публикация не найдена.",
        )

        val expected = listOf(
            "Не удалось выполнить инструмент `fetch_post` через MCP: http://127.0.0.1:3000/mcp",
            "Ошибка: Публикация не найдена.",
        ).joinToString(System.lineSeparator())

        assertEquals(expected, formatter.format(result))
    }
}
