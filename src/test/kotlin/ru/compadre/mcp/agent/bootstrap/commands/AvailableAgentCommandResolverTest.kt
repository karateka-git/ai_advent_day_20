package ru.compadre.mcp.agent.bootstrap.commands

import kotlin.test.Test
import kotlin.test.assertEquals
import ru.compadre.mcp.agent.bootstrap.models.AgentCommandId
import ru.compadre.mcp.agent.bootstrap.models.McpServerId
import ru.compadre.mcp.agent.bootstrap.models.PreparedMcpServer
import ru.compadre.mcp.mcp.client.common.model.McpToolDescriptor

class AvailableAgentCommandResolverTest {
    @Test
    fun resolveBuildsCommandsFromExplicitDefinitions() {
        val resolver = AvailableAgentCommandResolver(
            definitions = supportedAgentCommandDefinitions(),
        )

        val result = resolver.resolve(
            servers = listOf(
                PreparedMcpServer(
                    serverId = McpServerId.LOCAL_MCP_SERVER,
                    endpoint = "http://127.0.0.1:3000/mcp",
                    prepared = true,
                    tools = listOf(
                        McpToolDescriptor(name = "list_posts", title = "List Posts"),
                        McpToolDescriptor(name = "fetch_post", title = "Fetch Post"),
                    ),
                ),
                PreparedMcpServer(
                    serverId = McpServerId.LOCAL_STATEFUL_MCP_SERVER,
                    endpoint = "http://127.0.0.1:3001/mcp",
                    prepared = true,
                    tools = listOf(
                        McpToolDescriptor(name = "start_random_posts", title = "Start Random Posts"),
                    ),
                ),
            ),
        )

        assertEquals(3, result.size)
        assertEquals(AgentCommandId.TOOL_POSTS, result.first().commandId)
        assertEquals("tool start-random-posts [intervalMinutes]", result.last().cliPattern)
    }

    @Test
    fun resolveSkipsDefinitionsThatCannotBeResolved() {
        val resolver = AvailableAgentCommandResolver(
            definitions = supportedAgentCommandDefinitions(),
        )

        val result = resolver.resolve(
            servers = listOf(
                PreparedMcpServer(
                    serverId = McpServerId.LOCAL_MCP_SERVER,
                    endpoint = "http://127.0.0.1:3000/mcp",
                    prepared = true,
                    tools = listOf(
                        McpToolDescriptor(name = "fetch_post", title = "Fetch Post"),
                    ),
                ),
            ),
        )

        assertEquals(1, result.size)
        assertEquals(AgentCommandId.TOOL_POST, result.single().commandId)
    }

    @Test
    fun resolveDoesNotFallbackToAnotherServerWhenDefinitionUsesFixedRouting() {
        val resolver = AvailableAgentCommandResolver(
            definitions = supportedAgentCommandDefinitions(),
        )

        val result = resolver.resolve(
            servers = listOf(
                PreparedMcpServer(
                    serverId = McpServerId("secondary"),
                    endpoint = "http://127.0.0.1:3100/mcp",
                    prepared = true,
                    tools = listOf(
                        McpToolDescriptor(name = "list_posts", title = "List Posts"),
                        McpToolDescriptor(name = "fetch_post", title = "Fetch Post"),
                    ),
                ),
            ),
        )

        assertEquals(0, result.size)
    }
}
