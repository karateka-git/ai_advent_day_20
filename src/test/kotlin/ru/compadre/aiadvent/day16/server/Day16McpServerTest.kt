package ru.compadre.aiadvent.day16.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Day16McpServerTest {
    @Test
    fun serverRegistersExpectedTools() {
        val server = createDay16McpServer()

        assertEquals(setOf("ping", "echo"), server.tools.keys)
        assertTrue(server.tools["ping"] != null)
        assertTrue(server.tools["echo"] != null)
    }
}
