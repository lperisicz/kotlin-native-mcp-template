package server.models

import kotlinx.serialization.Serializable

@Serializable
internal data class ToolCallResult(
    val content: List<ToolContent>,
    val isError: Boolean = false,
)

@Serializable
internal data class ToolContent(
    val type: String,
    val text: String,
)
