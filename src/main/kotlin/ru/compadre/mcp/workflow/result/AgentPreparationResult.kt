package ru.compadre.mcp.workflow.result

/**
 * Результат подготовки агента, пригодный для показа пользователю в CLI.
 */
data class AgentPreparationResult(
    val prepared: Boolean,
    val availableCommands: List<AvailableCliCommandResult> = emptyList(),
    val errorMessage: String? = null,
) : CommandResult

/**
 * Краткое описание пользовательской команды, доступной после подготовки агента.
 */
data class AvailableCliCommandResult(
    val pattern: String,
    val description: String,
)
