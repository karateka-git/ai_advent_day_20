package ru.compadre.mcp.mcp.server.stateful

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.ktor.server.sse.sse
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.SseServerTransport
import kotlinx.coroutines.awaitCancellation
import java.util.concurrent.ConcurrentHashMap
import ru.compadre.mcp.config.McpProjectConfig

private const val SESSION_ID_PARAM = "sessionId"

/**
 * Точка входа stateful MCP server для sandbox-проекта.
 */
fun main() {
    val statefulServer = createStatefulMcpServer()
    val server = embeddedServer(
        factory = CIO,
        host = McpProjectConfig.SERVER_HOST,
        port = McpProjectConfig.STATEFUL_SERVER_PORT,
        module = { configureStatefulMcpServer(statefulServer) },
    )

    println("Starting stateful MCP server at ${McpProjectConfig.statefulEndpoint()}")
    server.start(wait = true)
}

/**
 * Конфигурирует Ktor-приложение для stateful MCP server поверх классического SSE transport.
 *
 * Здесь мы не используем SDK helper `mcp(path)`, потому что для нестандартного пути `/mcp`
 * он формирует относительный POST endpoint без самого path. Нам нужен явный `/mcp?sessionId=...`.
 */
fun Application.configureStatefulMcpServer(
    statefulServer: Server = createStatefulMcpServer(),
) {
    install(ContentNegotiation) {
        json()
    }
    install(SSE)

    val transports = ConcurrentHashMap<String, SseServerTransport>()

    routing {
        route(McpProjectConfig.MCP_PATH) {
            sse {
                val transport = SseServerTransport(McpProjectConfig.MCP_PATH, this)
                transports[transport.sessionId] = transport
                transport.onClose {
                    transports.remove(transport.sessionId)
                }

                statefulServer.createSession(transport)
                awaitCancellation()
            }

            post {
                val sessionId = call.request.queryParameters[SESSION_ID_PARAM]
                if (sessionId.isNullOrBlank()) {
                    call.respond(
                        status = io.ktor.http.HttpStatusCode.BadRequest,
                        message = "sessionId query parameter is not provided",
                    )
                    return@post
                }

                val transport = transports[sessionId]
                if (transport == null) {
                    call.respond(
                        status = io.ktor.http.HttpStatusCode.NotFound,
                        message = "Session not found",
                    )
                    return@post
                }

                transport.handlePostMessage(call)
            }
        }
    }
}
