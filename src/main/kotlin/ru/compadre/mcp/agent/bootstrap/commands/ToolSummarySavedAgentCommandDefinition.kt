package ru.compadre.mcp.agent.bootstrap.commands

import ru.compadre.mcp.agent.bootstrap.commands.models.CommandRouting
import ru.compadre.mcp.agent.bootstrap.models.AgentCommandId
import ru.compadre.mcp.agent.bootstrap.models.McpServerId

internal class ToolSummarySavedAgentCommandDefinition : ToolBasedAgentCommandDefinition(
    commandId = AgentCommandId.TOOL_SUMMARY_SAVED,
    cliPattern = "tool summary saved <summaryId>",
    description = "Показать одну сохранённую summary по идентификатору.",
    toolName = "get_saved_summary",
    routing = CommandRouting.FixedServer(McpServerId.LOCAL_STATEFUL_MCP_SERVER),
)
