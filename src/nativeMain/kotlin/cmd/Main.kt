package cmd

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import logger.Logger
import logger.info
import platform.posix.SIGINT
import platform.posix.exit
import platform.posix.signal
import server.MCPServer
import server.tool.ExampleTool
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "Main"

private lateinit var mainScope: CoroutineScope

public fun main(args: Array<String>) {
    val config = parseArgs(args)

    Logger.applyConfig(config)

    Logger.info(TAG, "Starting server with config $config")

    val server = MCPServer(
        transportConfig = config.serverTransport!!,
        port = config.ssePort,
        tools = listOf(
            ExampleTool,
        )
    )

    // TODO provide custom scope
    runBlocking {
        handleExit()
        server.start()
    }

    Logger.info(TAG, "Server stopped")

    Logger.close()
}

@OptIn(ExperimentalForeignApi::class)
private fun CoroutineScope.handleExit() {
    mainScope = this

    fun handleSignal(@Suppress("UNUSED_PARAMETER") signalNumber: Int) {
        Logger.info(TAG, "Caught SIGINT, shutting down...")
        println("Caunght exception")
        mainScope.cancel(CancellationException("SIGINT received"))
        Logger.close()
        exit(0)
    }

    signal(SIGINT, staticCFunction(::handleSignal))
}

private fun Logger.applyConfig(config: Config) {
    setLogLevel(config.logLevel)
    setLogToStdout(config.logToStdout)
    setLogFile(config.logFile)
}
