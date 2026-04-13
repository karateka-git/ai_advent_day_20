package ru.compadre.mcp.agent.bootstrap.models

/**
 * Пользовательская команда, которую агент может обслужить на основе найденных MCP-возможностей.
 */
data class AvailableAgentCommand(
    val commandId: AgentCommandId,
    val cliPattern: String,
    val description: String,
    val toolName: String,
    val serverId: McpServerId,
    val endpoint: String,
)
