package ru.compadre.mcp.mcp.server.common.summarypipeline.tools

import kotlinx.serialization.json.Json

internal val summaryPipelineJson: Json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}
