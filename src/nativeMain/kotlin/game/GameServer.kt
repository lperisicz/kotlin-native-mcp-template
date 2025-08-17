package game

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object GameServer {

    private val mutex = Mutex()
    private var oToAssign = false

    suspend fun joinGame(): String = mutex.withLock {
        val index = 0.takeIf { oToAssign } ?: 1
        oToAssign = oToAssign.not()
        val player = GameState.availablePlayers[index]
        return@withLock player
    }

    suspend fun getState(): List<String> = mutex.withLock { GameState.state }

    suspend fun updateState(newState: List<String>) = mutex.withLock {
        GameState.state = newState
    }
}