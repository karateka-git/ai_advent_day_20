package ru.compadre.mcp.agent.bootstrap.models

/**
 * Типизированный идентификатор известного MCP-сервера внутри capability-модели агента.
 */
@JvmInline
value class McpServerId(
    val value: String,
) {
    companion object {
        val LOCAL_MCP_SERVER: McpServerId = McpServerId("local_mcp_server")
        val LOCAL_STATEFUL_MCP_SERVER: McpServerId = McpServerId("local_stateful_mcp_server")
    }
}
