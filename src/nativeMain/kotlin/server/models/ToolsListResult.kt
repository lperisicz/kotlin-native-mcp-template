package server.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class ToolsListResult(
    val tools: List<ToolItem>,
)

@Serializable
internal data class ToolItem(
    val name: String,
    val description: String,
    val inputSchema: JsonObject,
)