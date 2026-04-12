package ru.compadre.aiadvent.day16.client

import ru.compadre.aiadvent.day16.config.McpProjectConfig

/**
 * Временная точка входа клиента для следующего этапа реализации MCP client.
 */
fun main() {
    println(
        "MCP client entrypoint is prepared. " +
            "Default server endpoint: ${McpProjectConfig.defaultEndpoint()}"
    )
}
