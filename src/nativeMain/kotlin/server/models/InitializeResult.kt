package server.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class InitializeResult(
    val protocolVersion: String,
    val capabilities: JsonObject,
    val serverInfo: ServerInfo,
)
