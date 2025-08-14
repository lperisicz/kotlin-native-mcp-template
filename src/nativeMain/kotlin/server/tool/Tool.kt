package server.tool

import kotlinx.serialization.json.JsonObject
import server.models.ToolCallParams
import server.models.ToolCallResult
import server.models.ToolItem

internal interface Tool {

    val toolDefinition: ToolDefinition

    suspend fun invoke(params: ToolCallParams): ToolCallResult

    data class ToolDefinition(
        val name: String,
        val description: String,
        val inputSchema: JsonObject,
    )
}

internal fun Tool.toToolItem(): ToolItem =
    ToolItem(
        name = toolDefinition.name,
        description = toolDefinition.description,
        inputSchema = toolDefinition.inputSchema,
    )