package ru.compadre.mcp.mcp.server.stateful.session

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerSession

/**
 * Тонкая проектная обёртка над active sessions stateful MCP server.
 */
class ClientSessionRegistry(
    private val server: Server,
) {
    fun sessions(): Map<String, ServerSession> = server.sessions

    fun contains(sessionId: String): Boolean = server.sessions.containsKey(sessionId)

    fun size(): Int = server.sessions.size
}
