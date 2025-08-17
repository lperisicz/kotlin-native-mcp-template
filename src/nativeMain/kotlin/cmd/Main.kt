package cmd

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import logger.Logger
import logger.info
import platform.posix.SIGINT
import platform.posix.SIGTERM
import platform.posix.SIGTSTP
import platform.posix.exit
import platform.posix.signal
import platform.posix.sleep
import server.MCPServer
import server.tool.ExampleTool
import kotlin.coroutines.cancellation.CancellationException


private const val TAG = "Main"

private lateinit var mainScope: CoroutineScope
private lateinit var server: MCPServer

public fun main(args: Array<String>) {
    val config = parseArgs(args)

    Logger.applyConfig(config)

    Logger.info(TAG, "Starting server with config $config")

    server = MCPServer(
        transportConfig = config.serverTransport!!,
        port = config.ssePort,
        tools = listOf(
            ExampleTool,
        )
    )

    // TODO provide custom scope
    runBlocking(CoroutineExceptionHandler { coroutineContext, throwable ->
        println("Exception handler called: ${throwable.message?.lines()?.firstOrNull()}")
    }) {
        handleExit()
        server.start()

        println("Server start finished")

        while (coroutineContext.isActive) {
            delay(3000)
            println("PING")
        }
    }

    Logger.info(TAG, "Server stopped")

    Logger.close()
}

@OptIn(ExperimentalForeignApi::class)
private fun CoroutineScope.handleExit() {
    mainScope = this

    fun handleSignal(signalNumber: Int) {
        val signalName = when (signalNumber) {
            SIGINT -> "SIGINT (Ctrl+C)"
            SIGTERM -> "SIGTERM"
            SIGTSTP -> "SIGTSTP (Ctrl+Z)"
            else -> "Unknown signal ($signalNumber)"
        }

        Logger.info(TAG, "Caught $signalName, shutting down gracefully...")
        println("Shutting down server...")

        // Shutdown the server gracefully
        // TODO revert
        server.shutdown()

        println("Cancelling scope...")
        // Cancel the coroutine scope to stop the server
        mainScope.cancel(CancellationException("$signalName received"))

        println("Sleeping...")
        // Give some time for graceful shutdown
        sleep(3u)

        println("Closing logger....")
        Logger.close()

        println("Exiting....")
        // exit(0)
    }

    // Handle multiple signals
    signal(SIGINT, staticCFunction(::handleSignal))   // Ctrl+C
    signal(SIGTERM, staticCFunction(::handleSignal))  // Termination
    signal(SIGTSTP, staticCFunction(::handleSignal))  // Ctrl+Z
}

private fun Logger.applyConfig(config: Config) {
    setLogLevel(config.logLevel)
    setLogToStdout(config.logToStdout)
    setLogFile(config.logFile)
}
