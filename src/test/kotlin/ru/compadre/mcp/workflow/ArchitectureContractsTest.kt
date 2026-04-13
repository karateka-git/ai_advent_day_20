package ru.compadre.mcp.workflow

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import ru.compadre.mcp.agent.AgentRequest
import ru.compadre.mcp.agent.AgentResponse
import ru.compadre.mcp.agent.bootstrap.models.AgentCapabilitySnapshot
import ru.compadre.mcp.agent.bootstrap.models.KnownMcpServer
import ru.compadre.mcp.agent.bootstrap.models.PreparedMcpServer
import ru.compadre.mcp.mcp.client.model.McpServerInfo
import ru.compadre.mcp.mcp.client.model.McpToolDescriptor
import ru.compadre.mcp.mcp.toolcall.models.McpToolCallRequest
import ru.compadre.mcp.mcp.toolcall.models.McpToolCallResult
import ru.compadre.mcp.workflow.command.Command
import ru.compadre.mcp.workflow.command.PrepareAgentCommand
import ru.compadre.mcp.workflow.command.ToolPostCommand
import ru.compadre.mcp.workflow.command.ToolPostsCommand
import ru.compadre.mcp.workflow.result.AgentPreparationResult
import ru.compadre.mcp.workflow.result.AvailableCliCommandResult
import ru.compadre.mcp.workflow.result.CommandResult
import ru.compadre.mcp.workflow.result.ToolCallResult

class ArchitectureContractsTest {
    @Test
    fun prepareAgentCommandImplementsBaseCommandContract() {
        val command: Command = PrepareAgentCommand

        assertEquals(PrepareAgentCommand, command)
    }

    @Test
    fun preparationResultImplementsBaseCommandResultContract() {
        val result: CommandResult = AgentPreparationResult(
            prepared = true,
            availableCommands = listOf(
                AvailableCliCommandResult(
                    pattern = "tool posts",
                    description = "Показать первые 10 публикаций.",
                ),
            ),
        )

        assertIs<AgentPreparationResult>(result)
        assertEquals(true, result.prepared)
        assertEquals(1, result.availableCommands.size)
    }

    @Test
    fun toolPostCommandImplementsBaseCommandContract() {
        val command: Command = ToolPostCommand(postId = 1)

        assertIs<ToolPostCommand>(command)
        assertEquals(1, command.postId)
    }

    @Test
    fun toolPostsCommandImplementsBaseCommandContract() {
        val command: Command = ToolPostsCommand

        assertEquals(ToolPostsCommand, command)
    }

    @Test
    fun toolCallResultImplementsBaseCommandResultContract() {
        val result: CommandResult = ToolCallResult(
            commandText = "tool post 1",
            successful = true,
            content = listOf("Публикация #1"),
        )

        assertIs<ToolCallResult>(result)
        assertEquals(true, result.successful)
        assertEquals("tool post 1", result.commandText)
        assertEquals(listOf("Публикация #1"), result.content)
    }

    @Test
    fun agentConnectResponseKeepsNormalizedMcpData() {
        val response: AgentResponse = AgentResponse.ConnectSuccess(
            endpoint = "http://127.0.0.1:3000/mcp",
            serverInfo = McpServerInfo(
                name = "local_mcp_server",
                version = "0.1.0",
                title = "Local MCP Server",
            ),
            tools = listOf(
                McpToolDescriptor(name = "ping", title = "Ping"),
                McpToolDescriptor(name = "echo", title = "Echo"),
            ),
        )

        assertIs<AgentResponse.ConnectSuccess>(response)
        assertEquals("local_mcp_server", response.serverInfo.name)
        assertEquals(2, response.tools.size)
    }

    @Test
    fun agentConnectRequestKeepsEndpoint() {
        val request: AgentRequest = AgentRequest.Connect(
            endpoint = "http://127.0.0.1:3000/mcp",
        )

        assertIs<AgentRequest.Connect>(request)
        assertEquals("http://127.0.0.1:3000/mcp", request.endpoint)
    }

    @Test
    fun agentPrepareRequestKeepsKnownServers() {
        val request: AgentRequest = AgentRequest.Prepare(
            servers = listOf(
                KnownMcpServer(
                    serverId = "local_mcp_server",
                    endpoint = "http://127.0.0.1:3000/mcp",
                ),
            ),
        )

        assertIs<AgentRequest.Prepare>(request)
        assertEquals(1, request.servers.size)
        assertEquals("local_mcp_server", request.servers.single().serverId)
    }

    @Test
    fun agentAvailableCommandRequestKeepsCommandIdAndArguments() {
        val request: AgentRequest = AgentRequest.CallAvailableCommand(
            commandId = "tool.post",
            arguments = mapOf("postId" to 1),
        )

        assertIs<AgentRequest.CallAvailableCommand>(request)
        assertEquals("tool.post", request.commandId)
        assertEquals(1, request.arguments["postId"])
    }

    @Test
    fun agentToolCallRequestKeepsEndpointAndToolPayload() {
        val request: AgentRequest = AgentRequest.CallTool(
            endpoint = "http://127.0.0.1:3000/mcp",
            toolCallRequest = McpToolCallRequest(
                toolName = "fetch_post",
                arguments = mapOf("postId" to 1),
            ),
        )

        assertIs<AgentRequest.CallTool>(request)
        assertEquals("http://127.0.0.1:3000/mcp", request.endpoint)
        assertEquals("fetch_post", request.toolCallRequest.toolName)
        assertEquals(1, request.toolCallRequest.arguments["postId"])
    }

    @Test
    fun agentToolCallResponseKeepsNormalizedMcpData() {
        val response: AgentResponse = AgentResponse.ToolCallSuccess(
            endpoint = "http://127.0.0.1:3000/mcp",
            result = McpToolCallResult(
                toolName = "fetch_post",
                isError = false,
                content = listOf("Публикация #1"),
            ),
        )

        assertIs<AgentResponse.ToolCallSuccess>(response)
        assertEquals("fetch_post", response.result.toolName)
        assertEquals(false, response.result.isError)
        assertEquals(listOf("Публикация #1"), response.result.content)
    }

    @Test
    fun agentPreparationResponseKeepsCapabilitySnapshot() {
        val response: AgentResponse = AgentResponse.PreparationSuccess(
            snapshot = AgentCapabilitySnapshot(
                servers = listOf(
                    PreparedMcpServer(
                        serverId = "local_mcp_server",
                        endpoint = "http://127.0.0.1:3000/mcp",
                        prepared = true,
                    ),
                ),
            ),
        )

        assertIs<AgentResponse.PreparationSuccess>(response)
        assertEquals(1, response.snapshot.servers.size)
        assertEquals(true, response.snapshot.servers.single().prepared)
    }
}
