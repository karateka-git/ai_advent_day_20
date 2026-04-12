package ru.compadre.aiadvent.day16.config

/**
 * Shared project-level defaults for the local MCP sandbox.
 */
object McpProjectConfig {
    const val SERVER_HOST: String = "127.0.0.1"
    const val SERVER_PORT: Int = 3000
    const val MCP_PATH: String = "/mcp"

    fun defaultEndpoint(): String = "http://$SERVER_HOST:$SERVER_PORT$MCP_PATH"
}
