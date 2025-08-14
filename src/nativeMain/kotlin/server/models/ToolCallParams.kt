package server.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class ToolCallParams(
    val name: String,
    val arguments: JsonObject? = null,
)
