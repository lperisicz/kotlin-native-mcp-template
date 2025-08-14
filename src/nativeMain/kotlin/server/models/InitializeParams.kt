package server.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class InitializeParams(
    // TODO check what to with different protocol versions
    // currently supports 2025-03-26 - 2025-06-18 as tested
    val protocolVersion: String,
    val capabilities: JsonObject,
    val clientInfo: JsonObject
)
