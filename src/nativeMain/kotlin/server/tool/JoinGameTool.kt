package server.tool

import game.GameServer
import game.GameState
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

internal object JoinGameTool : Tool {

    private const val TAG = "JoinGameTool"

    override val toolDefinition = Tool.ToolDefinition(
        name = "join_game",
        description = "Join a tic-tac-toe game by checking available player slots and assigning yourself a player symbol (X or O). The tool examines player1 and player2 values: if both are empty strings, you can choose to be either the first or second player and get assigned X or O accordingly. If one player slot is already taken, you automatically get the opposite symbol (if player1 has X, you get O, and vice versa). Once you pick a symbol, you become that player for the current game session. If you already have an assigned player value, you can keep your existing assignment.",
        inputSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("player", buildJsonObject {
                    put("type", "string")
                    put("description", "Optional player symbol to join as (X or O). If not provided, will auto-assign next available player.")
                    put("enum", JsonArray(listOf(JsonPrimitive("X"), JsonPrimitive("O"))))
                })
            })
            put("required", JsonArray(emptyList()))
        },
    )

    override suspend fun invoke(params: ToolCallParams): ToolCallResult {
        Logger.debug(TAG, "Tool called with: $params")

                return try {
            val requestedPlayer = params.arguments?.get("player")?.jsonPrimitive?.content

            // Examine player1 and player2 values as described
            val player1Status = GameState.player1
            val player2Status = GameState.player2

            val assignedPlayer = if (requestedPlayer != null) {
                // Validate requested player
                if (requestedPlayer != "X" && requestedPlayer != "O") {
                    return ToolCallResult(
                        content = listOf(
                            ToolContent(
                                type = "text",
                                text = "Error: Invalid player symbol. Must be 'X' or 'O'."
                            )
                        ),
                        isError = true
                    )
                }

                // Check if requested player slot is available
                when (requestedPlayer) {
                    "X" -> if (player1Status.isEmpty()) {
                        GameState.player1 = "X"
                        "X"
                    } else {
                        return ToolCallResult(
                            content = listOf(
                                ToolContent(
                                    type = "text",
                                    text = "Error: Player X slot is already taken. Try joining as O or start a new game."
                                )
                            ),
                            isError = true
                        )
                    }
                    "O" -> if (player2Status.isEmpty()) {
                        GameState.player2 = "O"
                        "O"
                    } else {
                        return ToolCallResult(
                            content = listOf(
                                ToolContent(
                                    type = "text",
                                    text = "Error: Player O slot is already taken. Try joining as X or start a new game."
                                )
                            ),
                            isError = true
                        )
                    }
                    else -> throw IllegalStateException("Invalid player symbol")
                }
            } else {
                // Auto-assign: if one player slot is already taken, get the opposite symbol
                when {
                    player1Status.isEmpty() && player2Status.isEmpty() -> {
                        // Both empty - assign X (first player)
                        GameState.player1 = "X"
                        "X"
                    }
                    player1Status.isNotEmpty() && player2Status.isEmpty() -> {
                        // Player1 taken, assign opposite (O)
                        GameState.player2 = "O"
                        "O"
                    }
                    player1Status.isEmpty() && player2Status.isNotEmpty() -> {
                        // Player2 taken, assign opposite (X)
                        GameState.player1 = "X"
                        "X"
                    }
                    else -> {
                        return ToolCallResult(
                            content = listOf(
                                ToolContent(
                                    type = "text",
                                    text = "Error: Game is full - both player slots are already taken. Start a new game to join."
                                )
                            ),
                            isError = true
                        )
                    }
                }
            }

            val content = listOf(
                ToolContent(
                    type = "text",
                    text = "Successfully joined game as player '$assignedPlayer'. You can now make moves on the game board."
                )
            )

            ToolCallResult(content = content, isError = false)
        } catch (e: Exception) {
            Logger.debug(TAG, "Error in JoinGameTool: ${e.message}")

            val errorContent = listOf(
                ToolContent(
                    type = "text",
                    text = "Error joining game: ${e.message ?: "Unknown error occurred"}"
                )
            )

            ToolCallResult(content = errorContent, isError = true)
        }
    }
}
