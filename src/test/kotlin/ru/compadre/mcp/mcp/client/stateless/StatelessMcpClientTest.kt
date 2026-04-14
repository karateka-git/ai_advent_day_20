package ru.compadre.mcp.mcp.client.stateless

import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlin.test.Test
import kotlin.test.assertEquals
import ru.compadre.mcp.mcp.client.common.toolcall.model.McpToolCallRequest
import ru.compadre.mcp.mcp.client.common.toolcall.model.McpToolCallResult

class StatelessMcpClientTest {
    @Test
    fun toolCallRequestKeepsProjectLevelContract() {
        val request = McpToolCallRequest(
            toolName = "fetch_post",
            arguments = mapOf("postId" to 1),
        )

        assertEquals("fetch_post", request.toolName)
        assertEquals(1, request.arguments["postId"])
    }

    @Test
    fun toolCallResultKeepsProjectLevelContract() {
        val result = McpToolCallResult(
            toolName = "fetch_post",
            isError = false,
            content = listOf("Публикация #1"),
        )

        assertEquals("fetch_post", result.toolName)
        assertEquals(false, result.isError)
        assertEquals(listOf("Публикация #1"), result.content)
    }

    @Test
    fun sdkCallToolResultMapsToProjectResult() {
        val client = StatelessMcpClient()

        val result = client.toToolCallResult(
            toolName = "fetch_post",
            result = CallToolResult(
                content = listOf(
                    TextContent("Публикация #1"),
                    TextContent("Автор: 1"),
                ),
                isError = false,
            ),
        )

        assertEquals(
            McpToolCallResult(
                toolName = "fetch_post",
                isError = false,
                content = listOf("Публикация #1", "Автор: 1"),
            ),
            result,
        )
    }
}
