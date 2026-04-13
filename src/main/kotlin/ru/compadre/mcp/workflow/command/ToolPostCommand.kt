package ru.compadre.mcp.workflow.command

/**
 * Workflow-команда вызова сценария получения публикации через MCP-инструмент.
 */
data class ToolPostCommand(
    val endpointOverride: String? = null,
    val postId: Int,
) : Command
