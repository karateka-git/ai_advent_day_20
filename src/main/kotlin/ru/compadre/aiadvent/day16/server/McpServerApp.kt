package ru.compadre.aiadvent.day16.server

import ru.compadre.aiadvent.day16.config.McpProjectConfig

/**
 * Placeholder server entrypoint for the upcoming MCP server implementation.
 */
fun main() {
    println(
        "MCP server entrypoint is prepared. " +
            "Target endpoint: ${McpProjectConfig.defaultEndpoint()}"
    )
}
