package ru.compadre.mcp

import kotlinx.coroutines.runBlocking
import ru.compadre.mcp.agent.DefaultAgent
import ru.compadre.mcp.mcp.client.DefaultMcpClient
import ru.compadre.mcp.presentation.cli.CliCommandParser
import ru.compadre.mcp.presentation.cli.CliOutputFormatter
import ru.compadre.mcp.presentation.cli.DefaultCliCommandParser
import ru.compadre.mcp.presentation.cli.DefaultCliOutputFormatter
import ru.compadre.mcp.workflow.result.CommandResult
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

    val commandParser: CliCommandParser = DefaultCliCommandParser()
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
    println("MCP-агент готов к работе.")
    println("Введите `help`, чтобы увидеть доступные команды.")

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

        executeCommand(
            commandArgs = rawInput.split(Regex("\\s+")).toTypedArray(),
            commandParser = commandParser,
            commandHandler = commandHandler,
            outputFormatter = outputFormatter,
        )
    }
}

private fun configureLogging() {
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn")
}

private fun helpText(): String = listOf(
    "Доступные команды:",
    "tool posts - показать первые 10 публикаций из JSONPlaceholder.",
    "tool post <postId> - получить публикацию из JSONPlaceholder по идентификатору.",
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
