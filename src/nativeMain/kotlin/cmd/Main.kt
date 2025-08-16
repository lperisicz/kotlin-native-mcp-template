package cmd

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.coroutines.runBlocking
import logger.LogLevel
import logger.Logger
import logger.info
import platform.posix.SIGINT
import platform.posix.exit
import platform.posix.fflush
import platform.posix.fprintf
import platform.posix.signal
import platform.posix.stderr
import server.MCPServer
import server.ServerConfig
import server.tool.ExampleTool
import server.tool.game.GetGameStateTool
import server.tool.game.JoinGameTool
import server.tool.game.MakeMoveTool

private const val TAG = "Main"

public fun main(args: Array<String>) {
    handleExit()

    val config = parseArgs(args)

    Logger.applyConfig(config)

    Logger.info(TAG, "Starting server with config $config")

    val server = MCPServer(
        transportConfig = config.serverTransport!!,
        port = config.ssePort,
        tools = listOf(
            ExampleTool,
            JoinGameTool,
            GetGameStateTool,
            MakeMoveTool,
        )
    )

    runBlocking {
        server.start()
    }

    Logger.info(TAG, "Server stopped")
}

private data class Config(
    val logLevel: LogLevel = LogLevel.INFO,
    val logFile: String? = null,
    val logToStdout: Boolean = false,
    val serverTransport: ServerConfig.Transport? = null,
    val ssePort: Int = ServerConfig.DEFAULT_SSE_PORT,
)

@OptIn(ExperimentalForeignApi::class)
private fun handleExit() {

    fun handleSignal(@Suppress("UNUSED_PARAMETER") signalNumber: Int) {
        Logger.info(TAG, "Caught SIGINT, shutting down...")
        Logger.close()
        exit(0)
    }

    signal(SIGINT, staticCFunction(::handleSignal))
}

@OptIn(ExperimentalCli::class, ExperimentalForeignApi::class)
private fun parseArgs(args: Array<String>): Config {
    var config = Config()
    val parser = ArgParser(programName = "KotlinNativeMCPTemplate")

    val logLevel by parser.option(
        type = ArgType.String,
        fullName = "log-level",
        description = "Set the log verbosity level (default: ${config.logLevel})"
    )

    val logFile by parser.option(
        type = ArgType.String,
        fullName = "log-file",
        description = "Path to a log file (default: ${config.logFile})"
    )

    val logToStdout by parser.option(
        type = ArgType.Boolean,
        fullName = "log-stdout",
        description = "Set stdout log enabled (default: ${config.logToStdout})"
    )

    class StdioCommand : Subcommand(
        name = "stdio",
        actionDescription = "Run server using stdio transport"
    ) {
        override fun execute() {
            config = config.copy(serverTransport = ServerConfig.Transport.STDIO)
        }
    }

    class SseCommand : Subcommand(
        name = "sse",
        actionDescription = "Run server using SSE transport"
    ) {

        val port by option(
            type = ArgType.Int,
            fullName = "port",
            shortName = "p",
            description = "Port to listen on for SSE (default: ${config.ssePort})"
        )

        override fun execute() {
            config = config.copy(
                serverTransport = ServerConfig.Transport.SSE,
                ssePort = port ?: config.ssePort,
            )
        }
    }

    parser.subcommands(StdioCommand(), SseCommand())
    parser.parse(args)

    if (config.serverTransport == null) {
        fprintf(stderr, "%s\n", "Error: No transport selected. See --help for usage.")
        fflush(stderr)
        // exit with non-zero code to indicate failure
        exit(1)
    }

    return config.copy(
        logLevel = logLevel?.let(LogLevel::valueOf) ?: config.logLevel,
        logFile = logFile ?: config.logFile,
        logToStdout = logToStdout ?: config.logToStdout
    )
}

private fun Logger.applyConfig(config: Config) {
    setLogLevel(config.logLevel)
    setLogToStdout(config.logToStdout)
    setLogFile(config.logFile)
}
