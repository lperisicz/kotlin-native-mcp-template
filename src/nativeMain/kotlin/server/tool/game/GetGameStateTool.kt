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

internal object GetGameStateTool : Tool {

    private const val TAG = "GetGameStateTool"

    override val toolDefinition = Tool.ToolDefinition(
        name = "tictactoe_get_game_state",
        description = "Get game state, returned as a String array of \"O\" or \"X\" inside the returned object under key: \"gameState\"" +
                "Returned data should represent game state line by line",
        inputSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {})
            put("required", JsonArray(emptyList()))
        },
    )

    override suspend fun invoke(params: ToolCallParams): ToolCallResult {
        Logger.debug(TAG, "Tool called with: $params")

        val currentState = GameServer.getState()

        val content = listOf(
            ToolContent(
                type = "text",
                text = currentState.joinToString(),
            )
        )

        return ToolCallResult(content = content, isError = false)
    }
}