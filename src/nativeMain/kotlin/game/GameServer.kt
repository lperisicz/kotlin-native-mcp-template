package game

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object GameServer {

    private val mutex = Mutex()

        // Note: joinGame logic moved to JoinGameTool for better control

    suspend fun getState(): List<String> = mutex.withLock { GameState.state }

    suspend fun updateState(newState: List<String>) = mutex.withLock {
        GameState.state = newState
    }
}
