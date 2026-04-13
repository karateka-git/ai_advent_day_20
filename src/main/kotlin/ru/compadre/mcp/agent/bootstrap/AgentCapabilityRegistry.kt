package ru.compadre.mcp.agent.bootstrap

import ru.compadre.mcp.agent.bootstrap.models.AgentCommandId
import ru.compadre.mcp.agent.bootstrap.models.AgentCapabilitySnapshot

/**
 * Хранит последний снимок подготовленных возможностей агента.
 */
class AgentCapabilityRegistry(
    initialSnapshot: AgentCapabilitySnapshot = AgentCapabilitySnapshot(),
) {
    private var currentSnapshot: AgentCapabilitySnapshot = initialSnapshot

    /**
     * Заменяет текущий снимок возможностей результатом последней подготовки.
     */
    fun replace(snapshot: AgentCapabilitySnapshot) {
        currentSnapshot = snapshot
    }

    /**
     * Возвращает текущий снимок возможностей агента.
     */
    fun snapshot(): AgentCapabilitySnapshot = currentSnapshot

    /**
     * Возвращает доступную пользователю команду по её стабильному идентификатору.
     */
    fun availableCommand(commandId: AgentCommandId) = currentSnapshot.availableCommands
        .firstOrNull { command -> command.commandId == commandId }
}
