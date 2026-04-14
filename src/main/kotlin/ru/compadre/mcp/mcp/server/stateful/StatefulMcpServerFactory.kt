package ru.compadre.mcp.mcp.server.stateful

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.DefaultJsonPlaceholderApiClient
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.JsonPlaceholderApiClient
import ru.compadre.mcp.mcp.server.stateful.background.RandomPostTickerService
import ru.compadre.mcp.mcp.server.stateful.subscriptions.RandomPostSubscriptionRegistry
import ru.compadre.mcp.mcp.server.stateful.toolcall.tools.startrandomposts.startRandomPostsToolResult
import ru.compadre.mcp.mcp.server.stateful.toolcall.tools.startrandomposts.startRandomPostsToolSchema
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Собирает stateful MCP server для push-сценариев поверх Streamable HTTP.
 *
 * В этом контуре намеренно регистрируется только один пользовательский инструмент:
 * start_random_posts.
 */
internal fun createStatefulMcpServer(
    jsonPlaceholderApiClient: JsonPlaceholderApiClient = DefaultJsonPlaceholderApiClient(),
    randomPostSubscriptionRegistry: RandomPostSubscriptionRegistry = RandomPostSubscriptionRegistry(),
    intervalToDelay: (Int) -> Duration = { minutes -> minutes.minutes },
    randomPostIdProvider: () -> Int = { kotlin.random.Random.nextInt(1, 101) },
): Server {
    lateinit var serverRef: Server
    val tickerService = RandomPostTickerService(
        subscriptionRegistry = randomPostSubscriptionRegistry,
        jsonPlaceholderApiClient = jsonPlaceholderApiClient,
        isSessionActive = { sessionId -> serverRef.sessions.containsKey(sessionId) },
        connectionProvider = { sessionId ->
            serverRef.sessions[sessionId]?.let { serverRef.clientConnection(sessionId) }
        },
        intervalToDelay = intervalToDelay,
        randomPostIdProvider = randomPostIdProvider,
    )

    val server = Server(
        serverInfo = Implementation(
            name = "local_stateful_mcp_server",
            version = "0.1.0",
            title = "Local Stateful MCP Server",
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = false),
            ),
        ),
        instructions = "Локальный stateful MCP server для push-сценария random posts.",
    ) {
        onClose {
            tickerService.close()
        }

        addTool(
            name = "start_random_posts",
            title = "Start Random Posts",
            description = "Включает или обновляет periodic push случайных публикаций для текущей stateful-сессии.",
            inputSchema = startRandomPostsToolSchema(),
        ) { request ->
            startRandomPostsToolResult(
                arguments = request.arguments,
                subscriptionRegistry = randomPostSubscriptionRegistry,
                tickerService = tickerService,
            )
        }
    }

    serverRef = server
    return server
}
