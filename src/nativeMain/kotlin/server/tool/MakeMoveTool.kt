package server.tool

import game.GameServer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
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
        description = "Make a strategic move on the tic-tac-toe game board by providing the complete new 3x3 matrix state. LLM analyzes current GameState.state array, identifies player symbol from JoinGame, and provides new board state with exactly one new move added. Cannot overwrite occupied positions. Use optimal game theory: 1) Win immediately, 2) Block opponent's win, 3) Create fork, 4) Block opponent's fork, 5) Take center [1,1], 6) Take corners [0,0],[0,2],[2,0],[2,2], 7) Take sides. Matrix positions: Row 0:[0,0],[0,1],[0,2], Row 1:[1,0],[1,1],[1,2], Row 2:[2,0],[2,1],[2,2].",
        inputSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("playerId", buildJsonObject {
                    put("type", "string")
                    put("description", "Your player ID - either 'X' or 'O'. You can only place your own symbol.")
                    put("enum", JsonArray(listOf(JsonPrimitive("X"), JsonPrimitive("O"))))
                })
                put("newBoardState", buildJsonObject {
                    put("type", "array")
                    put("description", "Complete new 3x3 board state as 9-element array. Must have exactly one new move added to current state. Array represents positions: [0,1,2,3,4,5,6,7,8] = [[0,0],[0,1],[0,2],[1,0],[1,1],[1,2],[2,0],[2,1],[2,2]] in 3x3 matrix.")
                    put("items", buildJsonObject {
                        put("type", "string")
                        put("enum", JsonArray(listOf(JsonPrimitive(""), JsonPrimitive("X"), JsonPrimitive("O"))))
                    })
                    put("minItems", 9)
                    put("maxItems", 9)
                })
            })
            put("required", JsonArray(listOf(JsonPrimitive("playerId"), JsonPrimitive("newBoardState"))))
        },
    )

    override suspend fun invoke(params: ToolCallParams): ToolCallResult {
        Logger.debug(TAG, "Tool called with: $params")

        return try {
            // Extract parameters
            val playerId = params.arguments?.get("playerId")?.jsonPrimitive?.content
            val newBoardStateJson = params.arguments?.get("newBoardState")

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

            // Extract new board state array
            if (newBoardStateJson == null) {
                return ToolCallResult(
                    content = listOf(
                        ToolContent(
                            type = "text",
                            text = "Error: newBoardState is required. Provide complete 9-element array for 3x3 matrix."
                        )
                    ),
                    isError = true
                )
            }

            val newBoardState = try {
                newBoardStateJson.jsonArray.map { element -> element.jsonPrimitive.content }
            } catch (e: Exception) {
                return ToolCallResult(
                    content = listOf(
                        ToolContent(
                            type = "text",
                            text = "Error: Invalid newBoardState format. Must be array of 9 strings."
                        )
                    ),
                    isError = true
                )
            }

            // Validate array size (3x3 = 9 elements)
            if (newBoardState.size != 9) {
                return ToolCallResult(
                    content = listOf(
                        ToolContent(
                            type = "text",
                            text = "Error: newBoardState must have exactly 9 elements for 3x3 matrix. Got ${newBoardState.size} elements."
                        )
                    ),
                    isError = true
                )
            }

            // Get current game state to validate move
            val currentState = GameServer.getState()

            // Validate that only one new move was added and no occupied positions were changed
            var changesCount = 0
            var newMovePosition = -1

            for (i in 0 until 9) {
                if (currentState[i] != newBoardState[i]) {
                    if (currentState[i].isNotEmpty()) {
                        return ToolCallResult(
                            content = listOf(
                                ToolContent(
                                    type = "text",
                                    text = "Error: Cannot overwrite occupied position $i. Current: '${currentState[i]}', Attempted: '${newBoardState[i]}'"
                                )
                            ),
                            isError = true
                        )
                    }
                    if (newBoardState[i] != playerId) {
                        return ToolCallResult(
                            content = listOf(
                                ToolContent(
                                    type = "text",
                                    text = "Error: Can only place your own symbol '$playerId' at position $i. Got: '${newBoardState[i]}'"
                                )
                            ),
                            isError = true
                        )
                    }
                    changesCount++
                    newMovePosition = i
                }
            }

            if (changesCount != 1) {
                return ToolCallResult(
                    content = listOf(
                        ToolContent(
                            type = "text",
                            text = "Error: Must make exactly one move. Found $changesCount changes."
                        )
                    ),
                    isError = true
                )
            }

            // Apply the new board state
            GameServer.updateState(newBoardState)

            // Convert position to 3x3 matrix coordinates for display
            val row = newMovePosition / 3
            val col = newMovePosition % 3

            // Format response with updated board
            val boardVisualization = formatGameBoard(newBoardState)

            val content = listOf(
                ToolContent(
                    type = "text",
                    text = "✅ Move successful! Player '$playerId' placed at matrix position [$row,$col] (array index $newMovePosition).\n\nUpdated 3x3 Game Board:\n```\n$boardVisualization\n```\n\nNext player can make their move!"
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
 ${gameState[0].ifEmpty { "0" }} | ${gameState[1].ifEmpty { "1" }} | ${gameState[2].ifEmpty { "2" }}    # Row 0: [0,0] [0,1] [0,2]
-----------
 ${gameState[3].ifEmpty { "3" }} | ${gameState[4].ifEmpty { "4" }} | ${gameState[5].ifEmpty { "5" }}    # Row 1: [1,0] [1,1] [1,2]
-----------
 ${gameState[6].ifEmpty { "6" }} | ${gameState[7].ifEmpty { "7" }} | ${gameState[8].ifEmpty { "8" }}    # Row 2: [2,0] [2,1] [2,2]
        """.trimIndent()
    }
}
