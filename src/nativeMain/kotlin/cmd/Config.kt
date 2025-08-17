package cmd

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import logger.LogLevel
import platform.posix.exit
import platform.posix.fflush
import platform.posix.fprintf
import platform.posix.stderr
import server.ServerConfig

internal data class Config(
    val logLevel: LogLevel = LogLevel.INFO,
    val logFile: String? = null,
    val logToStdout: Boolean = false,
    val serverTransport: ServerConfig.Transport? = null,
    val ssePort: Int = ServerConfig.DEFAULT_SSE_PORT,
)

@OptIn(ExperimentalCli::class, ExperimentalForeignApi::class)
internal fun parseArgs(args: Array<String>): Config {
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