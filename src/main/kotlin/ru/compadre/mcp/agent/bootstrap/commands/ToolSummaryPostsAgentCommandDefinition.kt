package ru.compadre.mcp.agent.bootstrap.commands

import ru.compadre.mcp.agent.bootstrap.commands.models.CommandRouting
import ru.compadre.mcp.agent.bootstrap.models.AgentCommandId
import ru.compadre.mcp.agent.bootstrap.models.McpServerId

internal class ToolSummaryPostsAgentCommandDefinition : RequiredToolsAgentCommandDefinition(
    commandId = AgentCommandId.TOOL_SUMMARY_POSTS,
    cliPattern = "tool summary posts <count> [strategy]",
    description = "Собрать summary по случайным публикациям и сохранить его в локальное хранилище.",
    requiredToolNames = setOf("pick_random_posts", "merge_posts", "save_summary"),
    representativeToolName = "pick_random_posts",
    routing = CommandRouting.FixedServer(McpServerId.LOCAL_STATEFUL_MCP_SERVER),
)
