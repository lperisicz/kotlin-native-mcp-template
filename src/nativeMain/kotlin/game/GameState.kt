package game

import kotlinx.serialization.Serializable

internal object GameState {

    internal var availablePlayers: List<String> = listOf("X", "O")
    internal var state = listOf(
        "", "", "",
        "", "", "",
        "", "", "",
    )
}

@Serializable
internal data class ApiGameState(
    val gameState: List<String>,
)