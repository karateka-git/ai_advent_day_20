package ru.compadre.mcp.agent.bootstrap.commands

import ru.compadre.mcp.agent.bootstrap.commands.models.CommandRouting
import ru.compadre.mcp.agent.bootstrap.models.AgentCommandId
import ru.compadre.mcp.agent.bootstrap.models.AvailableAgentCommand
import ru.compadre.mcp.agent.bootstrap.models.PreparedMcpServer

/**
 * Базовый контракт определения пользовательской команды агента поверх MCP-возможностей.
 */
interface AgentCommandDefinition {
    val commandId: AgentCommandId
    val cliPattern: String
    val description: String
    val routing: CommandRouting

    /**
     * Пытается разрешить пользовательскую команду на основе подготовленных MCP-серверов.
     */
    fun resolve(servers: List<PreparedMcpServer>): AvailableAgentCommand?
}
