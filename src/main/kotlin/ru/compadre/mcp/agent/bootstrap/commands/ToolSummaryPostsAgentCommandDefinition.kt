package ru.compadre.mcp.agent.bootstrap.commands

import ru.compadre.mcp.agent.bootstrap.commands.models.CommandRouting
import ru.compadre.mcp.agent.bootstrap.models.AgentCommandId
import ru.compadre.mcp.agent.bootstrap.models.AvailableAgentCommand
import ru.compadre.mcp.agent.bootstrap.models.McpServerId
import ru.compadre.mcp.agent.bootstrap.models.PreparedMcpServer

internal class ToolSummaryPostsAgentCommandDefinition : AgentCommandDefinition {
    override val commandId: AgentCommandId = AgentCommandId.TOOL_SUMMARY_POSTS
    override val cliPattern: String = "tool summary posts <count> [strategy]"
    override val description: String =
        "Собрать summary по публикациям через cross-server orchestration и сохранить его в локальное хранилище."
    override val routing: CommandRouting = CommandRouting.FixedServer(McpServerId.LOCAL_MCP_SERVER)

    override fun resolve(servers: List<PreparedMcpServer>): AvailableAgentCommand? {
        val sourceServer = servers.firstOrNull { server ->
            server.serverId == McpServerId.LOCAL_MCP_SERVER &&
                server.prepared &&
                server.tools.any { tool -> tool.name == LIST_POSTS_TOOL }
        } ?: return null

        val pipelineServer = servers.firstOrNull { server ->
            server.serverId == McpServerId.LOCAL_STATEFUL_MCP_SERVER &&
                server.prepared &&
                setOf(MERGE_POSTS_TOOL, SAVE_SUMMARY_TOOL).all { toolName ->
                    server.tools.any { tool -> tool.name == toolName }
                }
        } ?: return null

        // Команда выполняется cross-server flow, поэтому endpoint/toolName здесь только
        // репрезентуют входную capability команды, а не весь дальнейший orchestration path.
        return AvailableAgentCommand(
            commandId = commandId,
            cliPattern = cliPattern,
            description = description,
            toolName = LIST_POSTS_TOOL,
            serverId = sourceServer.serverId,
            endpoint = sourceServer.endpoint,
        )
    }

    private companion object {
        const val LIST_POSTS_TOOL = "list_posts"
        const val MERGE_POSTS_TOOL = "merge_posts"
        const val SAVE_SUMMARY_TOOL = "save_summary"
    }
}
