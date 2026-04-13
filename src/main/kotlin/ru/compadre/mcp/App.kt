package ru.compadre.mcp

import kotlinx.coroutines.runBlocking
import ru.compadre.mcp.agent.DefaultAgent
import ru.compadre.mcp.config.McpProjectConfig
import ru.compadre.mcp.mcp.client.DefaultMcpClient
import ru.compadre.mcp.presentation.cli.CliCommandParser
import ru.compadre.mcp.presentation.cli.CliOutputFormatter
import ru.compadre.mcp.presentation.cli.DefaultCliCommandParser
import ru.compadre.mcp.presentation.cli.DefaultCliOutputFormatter
import ru.compadre.mcp.workflow.result.CommandResult
import ru.compadre.mcp.workflow.result.ConnectResult
import ru.compadre.mcp.workflow.service.DefaultWorkflowCommandHandler
import ru.compadre.mcp.workflow.service.WorkflowCommandHandler
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets

/**
 * Главная точка входа приложения, связывающая presentation, workflow, agent и MCP-слои.
 */
fun main(args: Array<String>): Unit = runBlocking {
    configureUtf8Console()
    configureLogging()

    val commandParser: CliCommandParser = DefaultCliCommandParser(McpProjectConfig::defaultEndpoint)
    val commandHandler: WorkflowCommandHandler = DefaultWorkflowCommandHandler(
        agent = DefaultAgent(DefaultMcpClient()),
    )
    val outputFormatter: CliOutputFormatter = DefaultCliOutputFormatter()

    if (args.isEmpty()) {
        runInteractiveShell(
            commandParser = commandParser,
            commandHandler = commandHandler,
            outputFormatter = outputFormatter,
        )
        return@runBlocking
    }

    executeCommand(
        commandArgs = args,
        commandParser = commandParser,
        commandHandler = commandHandler,
        outputFormatter = outputFormatter,
    )
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

private suspend fun runInteractiveShell(
    commandParser: CliCommandParser,
    commandHandler: WorkflowCommandHandler,
    outputFormatter: CliOutputFormatter,
) {
    println("MCP-клиент готов к работе.")
    println("Введите `help`, чтобы увидеть доступные команды.")
    var isConnected = false

    while (true) {
        print("> ")
        val rawInput = readlnOrNull()
            ?.trim()
            ?.trimStart('\uFEFF')
            ?: run {
                println("Сессия клиента завершена.")
                return
            }

        if (rawInput.isBlank()) {
            continue
        }

        when (rawInput.lowercase()) {
            "exit", "quit" -> {
                println("Сессия клиента завершена.")
                return
            }

            "help" -> {
                println(helpText())
                continue
            }
        }

        if (rawInput.lowercase().startsWith("tool ") && !isConnected) {
            println("Сначала выполните `connect`, чтобы получить доступ к CLI-инструментам.")
            continue
        }

        val result = executeCommand(
            commandArgs = rawInput.split(Regex("\\s+")).toTypedArray(),
            commandParser = commandParser,
            commandHandler = commandHandler,
            outputFormatter = outputFormatter,
        )
        if (result is ConnectResult) {
            isConnected = result.connected
        }
    }
}

private fun configureLogging() {
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn")
}

private fun helpText(): String = listOf(
    "Доступные команды:",
    "connect - подключиться к MCP-серверу и показать доступные из CLI инструменты.",
    "tool posts - после `connect` показать первые 10 публикаций из JSONPlaceholder.",
    "tool post <postId> - после `connect` получить публикацию из JSONPlaceholder по идентификатору.",
    "help - показать это сообщение.",
    "exit - завершить сессию клиента.",
).joinToString(separator = System.lineSeparator())

private suspend fun executeCommand(
    commandArgs: Array<String>,
    commandParser: CliCommandParser,
    commandHandler: WorkflowCommandHandler,
    outputFormatter: CliOutputFormatter,
): CommandResult? {
    try {
        val command = commandParser.parse(commandArgs)
        val result = commandHandler.handle(command)
        val output = outputFormatter.format(result)
        println(output)
        return result
    } catch (error: IllegalArgumentException) {
        println(error.message ?: "Не удалось разобрать команду.")
        return null
    }
}
