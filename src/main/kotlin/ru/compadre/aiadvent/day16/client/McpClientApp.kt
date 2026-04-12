package ru.compadre.aiadvent.day16.client

import ru.compadre.aiadvent.day16.config.McpProjectConfig

/**
 * Placeholder client entrypoint for the upcoming MCP client implementation.
 */
fun main() {
    println(
        "MCP client entrypoint is prepared. " +
            "Default server endpoint: ${McpProjectConfig.defaultEndpoint()}"
    )
}
