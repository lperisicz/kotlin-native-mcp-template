package game

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object GameServer {

    private val mutex = Mutex()

    suspend fun joinGame(): String = mutex.withLock {
        val player = GameState.availablePlayers.first()
        GameState.availablePlayers = GameState.availablePlayers.drop(1)
        return@withLock player
    }

    suspend fun getState(): List<String> = mutex.withLock { GameState.state }

    suspend fun updateState(newState: List<String>) = mutex.withLock {
        GameState.state = newState
    }
}