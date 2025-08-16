package server.tool

import game.GameServer
import game.GameState
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import logger.Logger
import logger.debug
import server.models.ToolCallParams
import server.models.ToolCallResult
import server.models.ToolContent

internal object ResetGameTool : Tool {

    private const val TAG = "ResetGameTool"

    override val toolDefinition = Tool.ToolDefinition(
        name = "reset_game",
        description = "Reset the game board to start a new game. Clears all board positions to empty strings and resets player assignments.",
        inputSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {})
            put("required", JsonArray(emptyList()))
        },
    )

    override suspend fun invoke(params: ToolCallParams): ToolCallResult {
        Logger.debug(TAG, "Tool called with: $params")

        return try {
            // Reset game state to all empty strings
            val emptyState = listOf("", "", "", "", "", "", "", "", "")
            GameServer.updateState(emptyState)

            // Reset player assignments
            GameState.player1 = ""
            GameState.player2 = ""

            val content = listOf(
                ToolContent(
                    type = "text",
                    text = "🎮 Game reset successfully! \n\nNew 3x3 Game Board:\n```\n 0 | 1 | 2     # Row 0: [0,0] [0,1] [0,2]\n-----------\n 3 | 4 | 5     # Row 1: [1,0] [1,1] [1,2]\n-----------\n 6 | 7 | 8     # Row 2: [2,0] [2,1] [2,2]\n```\n\nAll positions cleared and players reset. Ready for a new game!"
                )
            )

            ToolCallResult(content = content, isError = false)
        } catch (e: Exception) {
            Logger.debug(TAG, "Error in ResetGameTool: ${e.message}")

            val errorContent = listOf(
                ToolContent(
                    type = "text",
                    text = "Error resetting game: ${e.message ?: "Unknown error occurred"}"
                )
            )

            ToolCallResult(content = errorContent, isError = true)
        }
    }
}
