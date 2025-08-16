package game

import kotlinx.serialization.Serializable

internal object GameState {

    internal var availablePlayers: List<String> = listOf("O", "X")
    internal var state = listOf(
        "X", "O", "",
        "", "O", "",
        "", "O", "X",
    )
}

@Serializable
internal data class ApiGameState(
    val gameState: List<String>,
)