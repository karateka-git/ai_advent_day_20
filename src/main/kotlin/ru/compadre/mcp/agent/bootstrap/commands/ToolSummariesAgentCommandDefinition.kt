package ru.compadre.mcp.agent.bootstrap.commands

import ru.compadre.mcp.agent.bootstrap.commands.models.CommandRouting
import ru.compadre.mcp.agent.bootstrap.models.AgentCommandId
import ru.compadre.mcp.agent.bootstrap.models.McpServerId

internal class ToolSummariesAgentCommandDefinition : ToolBasedAgentCommandDefinition(
    commandId = AgentCommandId.TOOL_SUMMARIES,
    cliPattern = "tool summaries",
    description = "Показать все summary, сохранённые в локальном хранилище.",
    toolName = "list_saved_summaries",
    routing = CommandRouting.FixedServer(McpServerId.LOCAL_MCP_SERVER),
)
