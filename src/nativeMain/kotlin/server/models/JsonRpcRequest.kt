package server.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class JsonRpcRequest(
    val jsonrpc: String,
    val id: JsonElement? = null,
    val method: String,
    val params: JsonObject? = null
)
