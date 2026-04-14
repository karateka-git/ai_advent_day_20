package ru.compadre.mcp.agent

import kotlinx.serialization.decodeFromString
import ru.compadre.mcp.agent.bootstrap.AgentCapabilityRegistry
import ru.compadre.mcp.agent.bootstrap.commands.AvailableAgentCommandResolver
import ru.compadre.mcp.agent.bootstrap.models.AgentCapabilitySnapshot
import ru.compadre.mcp.agent.bootstrap.models.AgentCommandId
import ru.compadre.mcp.agent.bootstrap.models.KnownMcpServer
import ru.compadre.mcp.agent.bootstrap.models.McpServerId
import ru.compadre.mcp.agent.bootstrap.models.PreparedMcpServer
import ru.compadre.mcp.mcp.client.McpClient
import ru.compadre.mcp.mcp.client.common.toolcall.model.McpToolCallRequest
import ru.compadre.mcp.mcp.client.common.toolcall.model.McpToolCallResult
import ru.compadre.mcp.mcp.server.common.summarypipeline.models.PostSelection
import ru.compadre.mcp.mcp.server.common.summarypipeline.models.SavedSummary
import ru.compadre.mcp.mcp.server.common.summarypipeline.models.SummaryDraft
import ru.compadre.mcp.mcp.server.common.summarypipeline.models.SummaryPost
import ru.compadre.mcp.mcp.server.common.summarypipeline.tools.summaryPipelineJson

class DefaultAgent(
    private val mcpClient: McpClient,
    private val capabilityRegistry: AgentCapabilityRegistry = AgentCapabilityRegistry(),
    private val commandResolver: AvailableAgentCommandResolver = AvailableAgentCommandResolver(),
) : Agent {
    override suspend fun handle(request: AgentRequest): AgentResponse = when (request) {
        is AgentRequest.Prepare -> handlePrepare(request)
        is AgentRequest.Connect -> handleConnect(request)
        is AgentRequest.CallAvailableCommand -> handleCallAvailableCommand(request)
        is AgentRequest.RunSummaryPipeline -> handleRunSummaryPipeline(request)
        is AgentRequest.CallTool -> handleCallTool(request)
    }

    private suspend fun handlePrepare(request: AgentRequest.Prepare): AgentResponse =
        runCatching {
            val preparedServers = request.servers.map { server -> prepareServer(server) }
            val snapshot = AgentCapabilitySnapshot(
                servers = preparedServers,
                availableCommands = commandResolver.resolve(preparedServers),
            )

            capabilityRegistry.replace(snapshot)
            AgentResponse.PreparationSuccess(snapshot)
        }.getOrElse { error ->
            AgentResponse.Failure(
                message = error.message ?: "Не удалось выполнить агентный запрос подготовки MCP-возможностей.",
            )
        }

    private suspend fun handleConnect(request: AgentRequest.Connect): AgentResponse =
        runCatching {
            val snapshot = mcpClient.connect(request.endpoint)

            AgentResponse.ConnectSuccess(
                endpoint = snapshot.endpoint,
                serverInfo = snapshot.serverInfo,
                tools = snapshot.tools,
            )
        }.getOrElse { error ->
            AgentResponse.Failure(
                message = error.message ?: "Не удалось выполнить агентный запрос connect.",
            )
        }

    private suspend fun handleCallAvailableCommand(request: AgentRequest.CallAvailableCommand): AgentResponse {
        val capabilitySnapshot = capabilityRegistry.snapshot()
        val availableCommand = capabilityRegistry.availableCommand(request.commandId)
            ?: return AgentResponse.Failure(
                message = unavailableCommandMessage(
                    commandId = request.commandId,
                    capabilitySnapshot = capabilitySnapshot,
                ),
            )

        return handleCallTool(
            AgentRequest.CallTool(
                endpoint = availableCommand.endpoint,
                toolCallRequest = McpToolCallRequest(
                    toolName = availableCommand.toolName,
                    arguments = request.arguments,
                ),
            ),
        )
    }

    private suspend fun handleRunSummaryPipeline(request: AgentRequest.RunSummaryPipeline): AgentResponse =
        runCatching {
            val server = requirePreparedSummaryPipelineServer()
            ensureSummaryPipelineToolsExist(server)

            val randomPostsResult = mcpClient.callTool(
                endpoint = server.endpoint,
                request = McpToolCallRequest(
                    toolName = PICK_RANDOM_POSTS_TOOL,
                    arguments = mapOf("count" to request.count),
                ),
            )
            if (randomPostsResult.isError) {
                return AgentResponse.ToolCallSuccess(
                    endpoint = server.endpoint,
                    result = randomPostsResult.copy(toolName = SUMMARY_PIPELINE_RESULT_TOOL),
                )
            }

            val randomPosts = decodeStructured<PostSelection>(randomPostsResult).posts
            val selectedPosts = selectSummaryPosts(randomPosts, request.strategy)

            val mergeResult = mcpClient.callTool(
                endpoint = server.endpoint,
                request = McpToolCallRequest(
                    toolName = MERGE_POSTS_TOOL,
                    arguments = mapOf(
                        "strategy" to request.strategy,
                        "posts" to selectedPosts.map { post ->
                            mapOf(
                                "userId" to post.userId,
                                "id" to post.id,
                                "title" to post.title,
                                "body" to post.body,
                            )
                        },
                    ),
                ),
            )
            if (mergeResult.isError) {
                return AgentResponse.ToolCallSuccess(
                    endpoint = server.endpoint,
                    result = mergeResult.copy(toolName = SUMMARY_PIPELINE_RESULT_TOOL),
                )
            }

            val summaryDraft = decodeStructured<SummaryDraft>(mergeResult)
            val saveResult = mcpClient.callTool(
                endpoint = server.endpoint,
                request = McpToolCallRequest(
                    toolName = SAVE_SUMMARY_TOOL,
                    arguments = mapOf(
                        "title" to summaryDraft.title,
                        "content" to summaryDraft.content,
                        "sourcePostIds" to summaryDraft.sourcePostIds,
                        "strategy" to summaryDraft.strategy,
                    ),
                ),
            )
            if (saveResult.isError) {
                return AgentResponse.ToolCallSuccess(
                    endpoint = server.endpoint,
                    result = saveResult.copy(toolName = SUMMARY_PIPELINE_RESULT_TOOL),
                )
            }

            val savedSummary = decodeStructured<SavedSummary>(saveResult)
            AgentResponse.ToolCallSuccess(
                endpoint = server.endpoint,
                result = McpToolCallResult(
                    toolName = SUMMARY_PIPELINE_RESULT_TOOL,
                    isError = false,
                    content = listOf(
                        "Summary pipeline выполнен успешно.",
                        "Стратегия: ${request.strategy}",
                        "Выбраны публикации: ${selectedPosts.joinToString { it.id.toString() }}",
                        "Заголовок: ${savedSummary.title}",
                        "Сохранён summary: ${savedSummary.displayId}",
                        "Время сохранения: ${savedSummary.savedAt}",
                    ),
                    structuredContent = saveResult.structuredContent,
                ),
            )
        }.getOrElse { error ->
            AgentResponse.Failure(
                message = error.message ?: "Не удалось выполнить summary pipeline.",
            )
        }

    private suspend fun handleCallTool(request: AgentRequest.CallTool): AgentResponse =
        runCatching {
            AgentResponse.ToolCallSuccess(
                endpoint = request.endpoint,
                result = mcpClient.callTool(
                    endpoint = request.endpoint,
                    request = request.toolCallRequest,
                ),
            )
        }.getOrElse { error ->
            AgentResponse.Failure(
                message = error.message ?: "Не удалось выполнить агентный запрос callTool.",
            )
        }

    private suspend fun prepareServer(server: KnownMcpServer): PreparedMcpServer =
        runCatching {
            val snapshot = mcpClient.connect(server.endpoint)

            PreparedMcpServer(
                serverId = server.serverId,
                endpoint = snapshot.endpoint,
                prepared = true,
                serverInfo = snapshot.serverInfo,
                tools = snapshot.tools,
            )
        }.getOrElse { error ->
            PreparedMcpServer(
                serverId = server.serverId,
                endpoint = server.endpoint,
                prepared = false,
                errorMessage = error.message ?: "Не удалось подготовить MCP-сервер `${server.serverId.value}`.",
            )
        }

    private fun requirePreparedSummaryPipelineServer(): PreparedMcpServer =
        capabilityRegistry.snapshot().servers.firstOrNull {
            it.serverId == McpServerId.LOCAL_STATEFUL_MCP_SERVER && it.prepared
        } ?: error("Summary pipeline недоступен: stateful MCP-сервер не подготовлен.")

    private fun ensureSummaryPipelineToolsExist(server: PreparedMcpServer) {
        val toolNames = server.tools.map { it.name }.toSet()
        val requiredTools = setOf(PICK_RANDOM_POSTS_TOOL, MERGE_POSTS_TOOL, SAVE_SUMMARY_TOOL)
        val missingTools = requiredTools - toolNames
        check(missingTools.isEmpty()) {
            "Summary pipeline недоступен: отсутствуют MCP-инструменты ${missingTools.joinToString()}."
        }
    }

    private inline fun <reified T> decodeStructured(result: McpToolCallResult): T {
        val structuredContent = result.structuredContent
            ?: error("Инструмент `${result.toolName}` не вернул structuredContent, необходимый для pipeline.")

        return summaryPipelineJson.decodeFromString(structuredContent.toString())
    }

    private fun selectSummaryPosts(posts: List<SummaryPost>, strategy: String): List<SummaryPost> = when (strategy) {
        "long" -> posts.sortedByDescending { it.body.length }.take(3)
        "short" -> posts.sortedBy { it.body.length }.take(3)
        else -> error("Неизвестная стратегия summary pipeline: `$strategy`.")
    }

    private fun unavailableCommandMessage(
        commandId: AgentCommandId,
        capabilitySnapshot: AgentCapabilitySnapshot,
    ): String = if (capabilitySnapshot.servers.isEmpty()) {
        "Команда `${commandId.value()}` недоступна: агент ещё не подготовил MCP-возможности."
    } else {
        "Команда `${commandId.value()}` недоступна: агент не нашёл для неё подходящий MCP-инструмент."
    }

    private companion object {
        const val PICK_RANDOM_POSTS_TOOL = "pick_random_posts"
        const val MERGE_POSTS_TOOL = "merge_posts"
        const val SAVE_SUMMARY_TOOL = "save_summary"
        const val SUMMARY_PIPELINE_RESULT_TOOL = "summary_pipeline"
    }
}
