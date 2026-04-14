package ru.compadre.mcp.presentation.cli

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import ru.compadre.mcp.workflow.command.ToolPostCommand
import ru.compadre.mcp.workflow.command.ToolPostsCommand
import ru.compadre.mcp.workflow.command.ToolStartRandomPostsCommand

class DefaultCliCommandParserTest {
    @Test
    fun parseRejectsEmptyCommand() {
        val parser = DefaultCliCommandParser()

        assertFailsWith<IllegalArgumentException> {
            parser.parse(emptyArray())
        }
    }

    @Test
    fun parseRejectsRemovedConnectCommand() {
        val parser = DefaultCliCommandParser()

        assertFailsWith<IllegalArgumentException> {
            parser.parse(arrayOf("connect"))
        }
    }

    @Test
    fun parseRejectsUnknownCommand() {
        val parser = DefaultCliCommandParser()

        assertFailsWith<IllegalArgumentException> {
            parser.parse(arrayOf("tools"))
        }
    }

    @Test
    fun parseAcceptsToolPostCommand() {
        val parser = DefaultCliCommandParser()

        val command = parser.parse(arrayOf("tool", "post", "1"))

        assertIs<ToolPostCommand>(command)
        assertEquals(1, command.postId)
    }

    @Test
    fun parseAcceptsToolPostsCommandWithBomPrefix() {
        val parser = DefaultCliCommandParser()

        val command = parser.parse(arrayOf("\uFEFFtool", "posts"))

        assertEquals(ToolPostsCommand, command)
    }

    @Test
    fun parseRejectsToolPostWithoutNumericPostId() {
        val parser = DefaultCliCommandParser()

        assertFailsWith<IllegalArgumentException> {
            parser.parse(arrayOf("tool", "post", "abc"))
        }
    }

    @Test
    fun parseAcceptsToolStartRandomPostsWithoutInterval() {
        val parser = DefaultCliCommandParser()

        val command = parser.parse(arrayOf("tool", "start-random-posts"))

        assertIs<ToolStartRandomPostsCommand>(command)
        assertEquals(null, command.intervalMinutes)
    }

    @Test
    fun parseAcceptsToolStartRandomPostsWithInterval() {
        val parser = DefaultCliCommandParser()

        val command = parser.parse(arrayOf("tool", "start-random-posts", "7"))

        assertIs<ToolStartRandomPostsCommand>(command)
        assertEquals(7, command.intervalMinutes)
    }

    @Test
    fun parseRejectsToolStartRandomPostsWithIntervalBelowMinimum() {
        val parser = DefaultCliCommandParser()

        assertFailsWith<IllegalArgumentException> {
            parser.parse(arrayOf("tool", "start-random-posts", "0"))
        }
    }
}
