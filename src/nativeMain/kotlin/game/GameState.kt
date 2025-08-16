package game

import kotlinx.serialization.Serializable

internal object GameState {

    internal var player1: String = ""
    internal var player2: String = ""
    internal var state = listOf(
        "", "", "",  // Row 0: [0,0] [0,1] [0,2]
        "", "", "",  // Row 1: [1,0] [1,1] [1,2]
        "", "", "",  // Row 2: [2,0] [2,1] [2,2]
    )
}

@Serializable
internal data class ApiGameState(
    val gameState: List<String>,
)
