package ru.compadre.aiadvent.day16.server

import ru.compadre.aiadvent.day16.config.McpProjectConfig

/**
 * Временная точка входа сервера для следующего этапа реализации MCP server.
 */
fun main() {
    println(
        "MCP server entrypoint is prepared. " +
            "Target endpoint: ${McpProjectConfig.defaultEndpoint()}"
    )
}
