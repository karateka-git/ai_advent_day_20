package ru.compadre.mcp.mcp.server.stateful

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.DefaultJsonPlaceholderApiClient
import ru.compadre.mcp.mcp.server.common.api.jsonplaceholder.JsonPlaceholderApiClient
import ru.compadre.mcp.mcp.server.common.summarypipeline.storage.FileSummaryStorage
import ru.compadre.mcp.mcp.server.common.summarypipeline.storage.SummaryStorage
import ru.compadre.mcp.mcp.server.common.summarypipeline.tools.getsavedsummary.getSavedSummaryToolOutputSchema
import ru.compadre.mcp.mcp.server.common.summarypipeline.tools.getsavedsummary.getSavedSummaryToolResult
import ru.compadre.mcp.mcp.server.common.summarypipeline.tools.getsavedsummary.getSavedSummaryToolSchema
import ru.compadre.mcp.mcp.server.common.summarypipeline.tools.listsavedsummaries.listSavedSummariesToolOutputSchema
import ru.compadre.mcp.mcp.server.common.summarypipeline.tools.listsavedsummaries.listSavedSummariesToolResult
import ru.compadre.mcp.mcp.server.common.summarypipeline.tools.listsavedsummaries.listSavedSummariesToolSchema
import ru.compadre.mcp.mcp.server.common.summarypipeline.tools.mergeposts.mergePostsToolOutputSchema
import ru.compadre.mcp.mcp.server.common.summarypipeline.tools.mergeposts.mergePostsToolResult
import ru.compadre.mcp.mcp.server.common.summarypipeline.tools.mergeposts.mergePostsToolSchema
import ru.compadre.mcp.mcp.server.common.summarypipeline.tools.pickrandomposts.pickRandomPostsToolOutputSchema
import ru.compadre.mcp.mcp.server.common.summarypipeline.tools.pickrandomposts.pickRandomPostsToolResult
import ru.compadre.mcp.mcp.server.common.summarypipeline.tools.pickrandomposts.pickRandomPostsToolSchema
import ru.compadre.mcp.mcp.server.common.summarypipeline.tools.savesummary.saveSummaryToolOutputSchema
import ru.compadre.mcp.mcp.server.common.summarypipeline.tools.savesummary.saveSummaryToolResult
import ru.compadre.mcp.mcp.server.common.summarypipeline.tools.savesummary.saveSummaryToolSchema
import ru.compadre.mcp.mcp.server.stateful.background.RandomPostTickerService
import ru.compadre.mcp.mcp.server.stateful.subscriptions.RandomPostSubscriptionRegistry
import ru.compadre.mcp.mcp.server.stateful.toolcall.tools.startrandomposts.startRandomPostsToolResult
import ru.compadre.mcp.mcp.server.stateful.toolcall.tools.startrandomposts.startRandomPostsToolSchema

internal fun createStatefulMcpServer(
    jsonPlaceholderApiClient: JsonPlaceholderApiClient = DefaultJsonPlaceholderApiClient(),
    randomPostSubscriptionRegistry: RandomPostSubscriptionRegistry = RandomPostSubscriptionRegistry(),
    summaryStorage: SummaryStorage = FileSummaryStorage(),
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
        instructions = "Локальный stateful MCP server для push-сценария random posts и summary pipeline.",
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

        addTool(
            name = "pick_random_posts",
            title = "Pick Random Posts",
            description = "Получает заданное количество случайных публикаций из локального каталога.",
            inputSchema = pickRandomPostsToolSchema(),
            outputSchema = pickRandomPostsToolOutputSchema(),
        ) { request ->
            pickRandomPostsToolResult(
                arguments = request.arguments,
            )
        }

        addTool(
            name = "merge_posts",
            title = "Merge Posts",
            description = "Объединяет выбранные публикации в один summary.",
            inputSchema = mergePostsToolSchema(),
            outputSchema = mergePostsToolOutputSchema(),
        ) { request ->
            mergePostsToolResult(
                arguments = request.arguments,
            )
        }

        addTool(
            name = "save_summary",
            title = "Save Summary",
            description = "Сохраняет summary в локальное хранилище.",
            inputSchema = saveSummaryToolSchema(),
            outputSchema = saveSummaryToolOutputSchema(),
        ) { request ->
            saveSummaryToolResult(
                arguments = request.arguments,
                summaryStorage = summaryStorage,
            )
        }

        addTool(
            name = "list_saved_summaries",
            title = "List Saved Summaries",
            description = "Возвращает все summary, сохранённые в локальном хранилище.",
            inputSchema = listSavedSummariesToolSchema(),
            outputSchema = listSavedSummariesToolOutputSchema(),
        ) {
            listSavedSummariesToolResult(
                summaryStorage = summaryStorage,
            )
        }

        addTool(
            name = "get_saved_summary",
            title = "Get Saved Summary",
            description = "Возвращает одну summary из локального хранилища по идентификатору.",
            inputSchema = getSavedSummaryToolSchema(),
            outputSchema = getSavedSummaryToolOutputSchema(),
        ) { request ->
            getSavedSummaryToolResult(
                arguments = request.arguments,
                summaryStorage = summaryStorage,
            )
        }
    }

    serverRef = server
    return server
}
