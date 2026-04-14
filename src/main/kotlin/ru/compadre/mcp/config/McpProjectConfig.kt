package ru.compadre.mcp.config

import ru.compadre.mcp.agent.bootstrap.models.KnownMcpServer
import ru.compadre.mcp.agent.bootstrap.models.McpServerId

/**
 * Общие настройки проекта для локального MCP sandbox.
 */
object McpProjectConfig {
    const val SERVER_HOST: String = "127.0.0.1"
    const val STATELESS_SERVER_PORT: Int = 3000
    const val STATEFUL_SERVER_PORT: Int = 3001
    const val MCP_PATH: String = "/mcp"

    fun defaultEndpoint(): String = statelessEndpoint()

    fun statelessEndpoint(): String = "http://$SERVER_HOST:$STATELESS_SERVER_PORT$MCP_PATH"

    fun statefulEndpoint(): String = "http://$SERVER_HOST:$STATEFUL_SERVER_PORT$MCP_PATH"

    /**
     * Возвращает стартовый список известных MCP-серверов для подготовки агента.
     */
    fun knownMcpServers(): List<KnownMcpServer> = listOf(
        KnownMcpServer(
            serverId = McpServerId.LOCAL_MCP_SERVER,
            endpoint = statelessEndpoint(),
        ),
        KnownMcpServer(
            serverId = McpServerId.LOCAL_STATEFUL_MCP_SERVER,
            endpoint = statefulEndpoint(),
        ),
    )
}
