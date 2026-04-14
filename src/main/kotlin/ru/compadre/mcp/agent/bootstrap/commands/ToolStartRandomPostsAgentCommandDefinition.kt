package ru.compadre.mcp.agent.bootstrap.commands

import ru.compadre.mcp.agent.bootstrap.commands.models.CommandRouting
import ru.compadre.mcp.agent.bootstrap.models.AgentCommandId
import ru.compadre.mcp.agent.bootstrap.models.McpServerId

/**
 * Definition пользовательской команды запуска stateful random-post push.
 */
internal class ToolStartRandomPostsAgentCommandDefinition : ToolBasedAgentCommandDefinition(
    commandId = AgentCommandId.TOOL_START_RANDOM_POSTS,
    cliPattern = "tool start-random-posts [intervalMinutes]",
    description = "Включить или обновить периодическую отправку случайных публикаций в текущую сессию.",
    toolName = "start_random_posts",
    routing = CommandRouting.FixedServer(McpServerId.LOCAL_STATEFUL_MCP_SERVER),
)
