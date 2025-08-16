package server.tool.game

import game.ApiGameState
import game.GameServer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import logger.Logger
import logger.debug
import server.models.ToolCallParams
import server.models.ToolCallResult
import server.models.ToolContent
import server.tool.Tool

internal object MakeMoveTool : Tool {

    private const val TAG = "MakeMoveTool"

    private val json by lazy { Json { ignoreUnknownKeys = true } }

    override val toolDefinition = Tool.ToolDefinition(
        name = "tictactoe_make_move",
        // TODO improve
        description = "Make move by updating the game state. list of \"O\" or \"X\"",
        inputSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("gameState", buildJsonObject {
                    put("type", "array")
                    put("items", buildJsonObject {
                        put("type", "string")
                        put(
                            "enum",
                            JsonArray(
                                listOf(
                                    JsonPrimitive("O"),
                                    JsonPrimitive("X"),
                                    JsonPrimitive("")
                                )
                            )
                        )
                    })
                })
            })
            put("required", JsonArray(listOf(JsonPrimitive("gameState"))))
        },
    )

    override suspend fun invoke(params: ToolCallParams): ToolCallResult {
        Logger.debug(TAG, "Tool called with: $params")

        // TODO add better error handling
        val params = params.arguments?.let {
            json.decodeFromJsonElement<ApiGameState>(it)
        } ?: throw RuntimeException("error updating state")

        GameServer.updateState(params.gameState)

        val content = listOf(
            ToolContent(
                type = "text",
                text = "done",
            )
        )

        return ToolCallResult(content = content, isError = false)
    }
}