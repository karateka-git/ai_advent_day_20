package ru.compadre.mcp.client

import kotlinx.coroutines.runBlocking
import ru.compadre.mcp.agent.DefaultAgent
import ru.compadre.mcp.application.command.ConnectCommand
import ru.compadre.mcp.application.result.ConnectResult
import ru.compadre.mcp.application.service.ApplicationCommandHandler
import ru.compadre.mcp.application.service.DefaultApplicationCommandHandler
import ru.compadre.mcp.config.McpProjectConfig
import ru.compadre.mcp.mcp.DefaultMcpClient
import ru.compadre.mcp.mcp.model.McpToolDescriptor
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets

/**
 * Точка входа минимального MCP client для сценария `initialize -> tools/list`.
 */
fun main(args: Array<String>): Unit = runBlocking {
    configureUtf8Console()
    when (parseClientCommand(args)) {
        ClientCommand.Connect -> runConnectCommand()
    }
}

internal enum class ClientCommand {
    Connect,
}

private suspend fun runConnectCommand() {
    val endpoint = McpProjectConfig.defaultEndpoint()
    val commandHandler: ApplicationCommandHandler = DefaultApplicationCommandHandler(
        agent = DefaultAgent(DefaultMcpClient()),
    )
    val result = commandHandler.handle(
        ConnectCommand(endpointOverride = endpoint),
    )

    when (result) {
        is ConnectResult -> {
            if (!result.connected) {
                throw IllegalStateException(result.errorMessage ?: "Не удалось подключиться к MCP server.")
            }

            printConnectionSummary(result)
            println(renderToolsList(result.tools.map { tool ->
                McpToolDescriptor(
                    name = tool.name,
                    title = tool.title,
                    description = tool.description,
                )
            }))
        }
    }
}

internal fun parseClientCommand(args: Array<String>): ClientCommand {
    val rawCommand = args.firstOrNull()?.trim()?.lowercase() ?: "connect"

    return when (rawCommand) {
        "connect" -> ClientCommand.Connect
        else -> throw IllegalArgumentException(
            "Неизвестная команда клиента: `$rawCommand`. Поддерживаемые команды: connect.",
        )
    }
}

private fun configureUtf8Console() {
    System.setOut(
        PrintStream(
            FileOutputStream(FileDescriptor.out),
            true,
            StandardCharsets.UTF_8,
        ),
    )
    System.setErr(
        PrintStream(
            FileOutputStream(FileDescriptor.err),
            true,
            StandardCharsets.UTF_8,
        ),
    )
}

private fun printConnectionSummary(result: ConnectResult) {
    println("Connected to MCP server: ${result.endpoint}")
    println("Server name: ${result.serverName ?: "<unknown>"}")
    println("Server version: ${result.serverVersion ?: "<unknown>"}")
    println("Server title: ${result.serverTitle ?: "<unknown>"}")
    println("Server instructions: ${result.serverInstructions ?: "<none>"}")
}

internal fun renderToolsList(tools: List<McpToolDescriptor>): String {
    if (tools.isEmpty()) {
        return "Available tools: <none>"
    }

    val lines = buildList {
        add("Available tools (${tools.size}):")
        tools.forEachIndexed { index, tool ->
            add("${index + 1}. ${formatToolLine(tool)}")
        }
    }

    return lines.joinToString(separator = System.lineSeparator())
}

private fun formatToolLine(tool: McpToolDescriptor): String {
    val title = tool.title?.takeIf { it.isNotBlank() } ?: tool.name
    val description = tool.description?.takeIf { it.isNotBlank() } ?: "Описание не указано."
    return "$title [${tool.name}] - $description"
}
