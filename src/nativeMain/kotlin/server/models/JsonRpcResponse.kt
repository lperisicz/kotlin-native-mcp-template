package server.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class JsonRpcResponse(
    val jsonrpc: String,
    val id: JsonElement? = null,
    val result: JsonElement? = null,
    val error: JsonElement? = null
)
