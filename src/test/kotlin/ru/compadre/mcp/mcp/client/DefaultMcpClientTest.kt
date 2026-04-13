package ru.compadre.mcp.mcp.client

import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlin.test.Test
import kotlin.test.assertEquals
import ru.compadre.mcp.mcp.toolcall.models.McpToolCallRequest
import ru.compadre.mcp.mcp.toolcall.models.McpToolCallResult

class DefaultMcpClientTest {
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
        val client = DefaultMcpClient()

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
