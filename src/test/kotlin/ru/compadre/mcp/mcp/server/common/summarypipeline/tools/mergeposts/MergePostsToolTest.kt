package ru.compadre.mcp.mcp.server.common.summarypipeline.tools.mergeposts

import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class MergePostsToolTest {
    @Test
    fun mergePostsBuildsSummaryDraft() {
        val result = mergePostsToolResult(
            arguments = buildJsonObject {
                put("strategy", "long")
                putJsonArray("posts") {
                    repeat(3) { index ->
                        add(
                            buildJsonObject {
                                put("userId", 1)
                                put("id", index + 1)
                                put("title", "Post ${index + 1}")
                                put("body", "Body ${index + 1}")
                            },
                        )
                    }
                }
            },
        )

        assertEquals(false, result.isError)
        val content = assertIs<TextContent>(result.content.single())
        assertEquals(true, content.text.contains("Стратегия отбора: long"))
        val structuredContent = assertNotNull(result.structuredContent)
        assertEquals("long", structuredContent.getValue("strategy").toString().trim('"'))
        assertEquals(3, structuredContent.getValue("sourcePostIds").jsonArray.size)
    }
}
