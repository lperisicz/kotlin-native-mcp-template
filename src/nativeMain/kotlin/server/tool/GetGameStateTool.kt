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

internal object GetGameStateTool : Tool {

    private const val TAG = "GetGameStateTool"

    override val toolDefinition = Tool.ToolDefinition(
        name = "get_game_state",
        description = "Get the current game state including the board and available players as JSON",
        inputSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {})
            put("required", JsonArray(emptyList()))
        },
    )

    override suspend fun invoke(params: ToolCallParams): ToolCallResult {
        Logger.debug(TAG, "Tool called with: $params")

                return try {
            // Get current game state from GameServer
            val currentGameState = GameServer.getState()
            val player1 = GameState.player1.ifEmpty { "Available" }
            val player2 = GameState.player2.ifEmpty { "Available" }

            // Build simple JSON response with game state
            val gameStateJson = buildJsonObject {
                put("gameState", JsonArray(currentGameState.map { kotlinx.serialization.json.JsonPrimitive(it) }))
                put("player1", kotlinx.serialization.json.JsonPrimitive(player1))
                put("player2", kotlinx.serialization.json.JsonPrimitive(player2))
            }

            val content = listOf(
                ToolContent(
                    type = "text",
                    text = "Current game state:\n\n```json\n${gameStateJson}\n```\n\nGame Board (3x3):\n```\n${formatGameBoard(currentGameState)}\n```"
                )
            )

            ToolCallResult(content = content, isError = false)
        } catch (e: Exception) {
            Logger.debug(TAG, "Error in GetGameStateTool: ${e.message}")

            val errorContent = listOf(
                ToolContent(
                    type = "text",
                    text = "Error getting game state: ${e.message ?: "Unknown error occurred"}"
                )
            )

            ToolCallResult(content = errorContent, isError = true)
        }
    }

            private fun formatGameBoard(gameState: List<String>): String {
        return """
 ${gameState[0].ifEmpty { "0" }} | ${gameState[1].ifEmpty { "1" }} | ${gameState[2].ifEmpty { "2" }}
-----------
 ${gameState[3].ifEmpty { "3" }} | ${gameState[4].ifEmpty { "4" }} | ${gameState[5].ifEmpty { "5" }}
-----------
 ${gameState[6].ifEmpty { "6" }} | ${gameState[7].ifEmpty { "7" }} | ${gameState[8].ifEmpty { "8" }}
        """.trimIndent()
    }
}
