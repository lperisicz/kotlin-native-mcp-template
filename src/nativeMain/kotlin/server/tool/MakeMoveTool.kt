package server.tool

import game.GameServer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import logger.Logger
import logger.debug
import server.models.ToolCallParams
import server.models.ToolCallResult
import server.models.ToolContent

internal object MakeMoveTool : Tool {

    private const val TAG = "MakeMoveTool"

    override val toolDefinition = Tool.ToolDefinition(
        name = "make_move",
        description = "Make a strategic move on the tic-tac-toe game board using optimal game theory. PRIORITY ORDER: 1) Win immediately if you have 2-in-a-row, 2) Block opponent's win if they have 2-in-a-row, 3) Create a fork (2 potential wins), 4) Block opponent's fork, 5) Take center (position 4), 6) Take corners (0,2,6,8), 7) Take sides (1,3,5,7). WINNING LINES: Rows [0,1,2],[3,4,5],[6,7,8], Columns [0,3,6],[1,4,7],[2,5,8], Diagonals [0,4,8],[2,4,6]. Analyze GameState.state array, identify your symbol from JoinGame, check all 8 winning lines for immediate wins/blocks, then follow priority hierarchy. Perfect play leads to draws, so focus on opponent mistakes.",
        inputSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("playerId", buildJsonObject {
                    put("type", "string")
                    put("description", "Your player ID - either 'X' or 'O'. You can only place your own symbol.")
                    put("enum", JsonArray(listOf(JsonPrimitive("X"), JsonPrimitive("O"))))
                })
                put("position", buildJsonObject {
                    put("type", "integer")
                    put("description", "Board position (0-8) where to place your symbol. Positions are numbered 0-8 from top-left to bottom-right.")
                    put("minimum", 0)
                    put("maximum", 8)
                })
            })
            put("required", JsonArray(listOf(JsonPrimitive("playerId"), JsonPrimitive("position"))))
        },
    )

    override suspend fun invoke(params: ToolCallParams): ToolCallResult {
        Logger.debug(TAG, "Tool called with: $params")

        return try {
            // Extract parameters
            val playerId = params.arguments?.get("playerId")?.jsonPrimitive?.content
            val position = params.arguments?.get("position")?.jsonPrimitive?.content?.toIntOrNull()

            // Validate playerId
            if (playerId == null || (playerId != "X" && playerId != "O")) {
                return ToolCallResult(
                    content = listOf(
                        ToolContent(
                            type = "text",
                            text = "Error: Invalid playerId. Must be 'X' or 'O'."
                        )
                    ),
                    isError = true
                )
            }

            // Validate position
            if (position == null || position < 0 || position > 8) {
                return ToolCallResult(
                    content = listOf(
                        ToolContent(
                            type = "text",
                            text = "Error: Invalid position. Must be an integer between 0 and 8."
                        )
                    ),
                    isError = true
                )
            }

            // Get current game state
            val currentState = GameServer.getState().toMutableList()

            // Check if position is empty
            if (currentState[position].isNotEmpty()) {
                return ToolCallResult(
                    content = listOf(
                        ToolContent(
                            type = "text",
                            text = "Error: Position $position is already occupied by '${currentState[position]}'. Choose an empty position."
                        )
                    ),
                    isError = true
                )
            }

            // Make the move
            currentState[position] = playerId
            GameServer.updateState(currentState)

            // Format response with updated board
            val boardVisualization = formatGameBoard(currentState)

            val content = listOf(
                ToolContent(
                    type = "text",
                    text = "✅ Move successful! Player '$playerId' placed at position $position.\n\nUpdated Game Board:\n```\n$boardVisualization\n```\n\nNext player can make their move!"
                )
            )

            ToolCallResult(content = content, isError = false)
        } catch (e: Exception) {
            Logger.debug(TAG, "Error in MakeMoveTool: ${e.message}")

            val errorContent = listOf(
                ToolContent(
                    type = "text",
                    text = "Error making move: ${e.message ?: "Unknown error occurred"}"
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
