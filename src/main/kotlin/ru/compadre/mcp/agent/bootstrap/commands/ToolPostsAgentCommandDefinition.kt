package ru.compadre.mcp.agent.bootstrap.commands

import ru.compadre.mcp.agent.bootstrap.commands.models.CommandRouting
import ru.compadre.mcp.agent.bootstrap.models.AgentCommandId
import ru.compadre.mcp.agent.bootstrap.models.McpServerId

/**
 * Definition пользовательской команды списка публикаций из JSONPlaceholder.
 */
internal class ToolPostsAgentCommandDefinition : ToolBasedAgentCommandDefinition(
    commandId = AgentCommandId.TOOL_POSTS,
    cliPattern = "tool posts",
    description = "Показать первые 10 публикаций из JSONPlaceholder.",
    toolName = "list_posts",
    routing = CommandRouting.FixedServer(McpServerId.LOCAL_MCP_SERVER),
)
