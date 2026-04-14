package ru.compadre.mcp.agent.bootstrap.commands

import ru.compadre.mcp.agent.bootstrap.commands.models.CommandRouting
import ru.compadre.mcp.agent.bootstrap.models.AgentCommandId
import ru.compadre.mcp.agent.bootstrap.models.AvailableAgentCommand
import ru.compadre.mcp.agent.bootstrap.models.PreparedMcpServer

internal abstract class RequiredToolsAgentCommandDefinition(
    override val commandId: AgentCommandId,
    override val cliPattern: String,
    override val description: String,
    private val requiredToolNames: Set<String>,
    private val representativeToolName: String,
    override val routing: CommandRouting,
) : AgentCommandDefinition {
    override fun resolve(servers: List<PreparedMcpServer>): AvailableAgentCommand? {
        val server = when (val currentRouting = routing) {
            is CommandRouting.FixedServer -> servers.firstOrNull { preparedServer ->
                preparedServer.serverId == currentRouting.serverId &&
                    preparedServer.prepared &&
                    requiredToolNames.all { toolName ->
                        preparedServer.tools.any { tool -> tool.name == toolName }
                    }
            }
        } ?: return null

        return AvailableAgentCommand(
            commandId = commandId,
            cliPattern = cliPattern,
            description = description,
            toolName = representativeToolName,
            serverId = server.serverId,
            endpoint = server.endpoint,
        )
    }
}
