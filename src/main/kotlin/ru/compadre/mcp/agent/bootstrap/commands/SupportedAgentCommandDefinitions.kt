package ru.compadre.mcp.agent.bootstrap.commands

/**
 * Возвращает поддерживаемые приложением command definitions.
 */
fun supportedAgentCommandDefinitions(): List<AgentCommandDefinition> = listOf(
    ToolPostsAgentCommandDefinition(),
    ToolPostAgentCommandDefinition(),
    ToolStartRandomPostsAgentCommandDefinition(),
    ToolSummaryPostsAgentCommandDefinition(),
    ToolSummariesAgentCommandDefinition(),
)
