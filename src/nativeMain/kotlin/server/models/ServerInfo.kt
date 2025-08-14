package server.models

import kotlinx.serialization.Serializable

@Serializable
internal data class ServerInfo(
    val name: String,
    val version: String,
)
