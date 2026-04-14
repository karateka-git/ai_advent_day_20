package ru.compadre.mcp

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ru.compadre.mcp.agent.DefaultAgent
import ru.compadre.mcp.mcp.client.RoutingMcpClient
import ru.compadre.mcp.presentation.cli.CliCommandParser
import ru.compadre.mcp.presentation.cli.CliOutputFormatter
import ru.compadre.mcp.presentation.cli.DefaultCliCommandParser
import ru.compadre.mcp.presentation.cli.DefaultCliOutputFormatter
import ru.compadre.mcp.workflow.command.PrepareAgentCommand
import ru.compadre.mcp.workflow.result.AgentPreparationResult
import ru.compadre.mcp.workflow.result.AvailableCliCommandResult
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

    val mcpClient = RoutingMcpClient()

    val commandParser: CliCommandParser = DefaultCliCommandParser()
    val commandHandler: WorkflowCommandHandler = DefaultWorkflowCommandHandler(
        agent = DefaultAgent(mcpClient),
    )
    val outputFormatter: CliOutputFormatter = DefaultCliOutputFormatter()

    try {
        val preparationResult = prepareAgent(
            commandHandler = commandHandler,
        )

        if (!preparationResult.prepared) {
            println(outputFormatter.format(preparationResult))
            return@runBlocking
        }

        if (args.isEmpty()) {
            println(outputFormatter.format(preparationResult))
            runInteractiveShell(
                availableCommands = preparationResult.availableCommands,
                commandParser = commandParser,
                commandHandler = commandHandler,
                outputFormatter = outputFormatter,
                mcpClient = mcpClient,
            )
            return@runBlocking
        }

        executeCommand(
            commandArgs = args,
            commandParser = commandParser,
            commandHandler = commandHandler,
            outputFormatter = outputFormatter,
        )
    } finally {
        mcpClient.close()
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

/**
 * Выполняет стартовую подготовку агента до входа в пользовательский цикл и показывает анимированное состояние загрузки.
 */
private suspend fun prepareAgent(
    commandHandler: WorkflowCommandHandler,
): AgentPreparationResult = coroutineScope {
    val preparationJob = async {
        commandHandler.handle(PrepareAgentCommand)
    }
    val loadingFrames = listOf("", ".", "..", "...")
    var frameIndex = 0

    while (!preparationJob.isCompleted) {
        val dots = loadingFrames[frameIndex % loadingFrames.size]
        print("\rПодготовка агента$dots   ")
        delay(250)
        frameIndex++
    }

    print("\r${" ".repeat(40)}\r")
    preparationJob.await() as AgentPreparationResult
}

private suspend fun runInteractiveShell(
    availableCommands: List<AvailableCliCommandResult>,
    commandParser: CliCommandParser,
    commandHandler: WorkflowCommandHandler,
    outputFormatter: CliOutputFormatter,
    mcpClient: RoutingMcpClient,
) = coroutineScope {
    println("Введите `help`, чтобы увидеть доступные команды.")
    val notificationJob = launch(Dispatchers.IO) {
        mcpClient.randomPostNotifications().collect { notification ->
            print("\r${" ".repeat(80)}\r")
            println("[push] ${notification.message}")
            print("> ")
        }
    }

    try {
        while (true) {
            print("> ")
            val rawInput = readlnOrNull()
                ?.trim()
                ?.trimStart('\uFEFF')
                ?: run {
                    println("Сессия клиента завершена.")
                    return@coroutineScope
                }

            if (rawInput.isBlank()) {
                continue
            }

            when (rawInput.lowercase()) {
                "exit", "quit" -> {
                    println("Сессия клиента завершена.")
                    return@coroutineScope
                }

                "help" -> {
                    println(helpText(availableCommands))
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
    } finally {
        notificationJob.cancel()
    }
}

private fun configureLogging() {
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn")
    System.setProperty("org.slf4j.simpleLogger.log.io.modelcontextprotocol.kotlin.sdk.client.Client", "off")
    System.setProperty("org.slf4j.simpleLogger.log.io.modelcontextprotocol.kotlin.sdk.shared.AbstractClientTransport", "off")
}

/**
 * Строит help-сообщение только из тех прикладных команд, которые агент реально нашёл при подготовке.
 */
private fun helpText(availableCommands: List<AvailableCliCommandResult>): String = buildList {
    add("Доступные команды:")
    availableCommands.forEach { command ->
        add("${command.pattern} - ${command.description}")
    }
    add("help - показать это сообщение.")
    add("exit - завершить сессию клиента.")
}.joinToString(separator = System.lineSeparator())

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
