package server.tool.game

import game.GameServer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import logger.Logger
import logger.debug
import server.models.ToolCallParams
import server.models.ToolCallResult
import server.models.ToolContent
import server.tool.Tool

internal object JoinGameTool : Tool {

    private const val TAG = "JoinGameTool"

    override val toolDefinition = Tool.ToolDefinition(
        name = "tictactoe_join_game",
        description = "Returns a string representation of assigned tictactoe player. \"O\" or \"X\"",
        inputSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {})
            put("required", JsonArray(emptyList()))
        },
    )

    override suspend fun invoke(params: ToolCallParams): ToolCallResult {
        Logger.debug(TAG, "Tool called with: $params")

        val assignedPlayer = GameServer.joinGame()

        val content = listOf(
            ToolContent(
                type = "text",
                text = assignedPlayer,
            )
        )

        return ToolCallResult(content = content, isError = false)
    }
}