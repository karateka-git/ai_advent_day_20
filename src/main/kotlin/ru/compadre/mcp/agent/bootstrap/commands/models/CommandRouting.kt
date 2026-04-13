package ru.compadre.mcp.agent.bootstrap.commands.models

import ru.compadre.mcp.agent.bootstrap.models.McpServerId

/**
 * Правило маршрутизации пользовательской команды на MCP-сервер.
 */
sealed interface CommandRouting {
    /**
     * Требует выполнения команды на конкретном известном MCP-сервере.
     */
    data class FixedServer(
        val serverId: McpServerId,
    ) : CommandRouting
}
