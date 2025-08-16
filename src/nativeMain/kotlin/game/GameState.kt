package game

import kotlinx.serialization.Serializable

internal object GameState {

    internal var player1: String = ""
    internal var player2: String = ""
    internal var state = listOf(
        "", "O", "",
        "", "", "",
        "", "", "",
    )
}

@Serializable
internal data class ApiGameState(
    val gameState: List<String>,
)
