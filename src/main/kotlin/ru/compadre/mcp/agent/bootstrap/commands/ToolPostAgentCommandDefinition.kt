package ru.compadre.mcp.agent.bootstrap.commands

import ru.compadre.mcp.agent.bootstrap.commands.models.CommandRouting
import ru.compadre.mcp.agent.bootstrap.models.AgentCommandId
import ru.compadre.mcp.agent.bootstrap.models.McpServerId

/**
 * Definition пользовательской команды получения публикации из JSONPlaceholder.
 */
internal class ToolPostAgentCommandDefinition : ToolBasedAgentCommandDefinition(
    commandId = AgentCommandId.TOOL_POST,
    cliPattern = "tool post <postId>",
    description = "Показать публикацию из JSONPlaceholder по идентификатору.",
    toolName = "fetch_post",
    routing = CommandRouting.FixedServer(McpServerId.LOCAL_MCP_SERVER),
)
