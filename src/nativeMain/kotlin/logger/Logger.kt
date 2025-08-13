package logger

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import platform.posix.FILE
import platform.posix.fclose
import platform.posix.fflush
import platform.posix.fopen
import platform.posix.fprintf
import platform.posix.perror
import platform.posix.stderr
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

@OptIn(ExperimentalForeignApi::class)
internal object Logger {

    private var logLevel: LogLevel = LogLevel.DEBUG
    private var logToStdout: Boolean = false
    private var logFile: CPointer<FILE>? = null

    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        if (level.ordinal < logLevel.ordinal) return

        val formattedMessage = formatLogMessage(level, tag, message)

        if (logFile != null) {
            writeToFile(formattedMessage)
        }

        if (logToStdout) {
            println(formattedMessage)
        }

        // write to stderr to keep stdout clean
        if (level == LogLevel.ERROR || level == LogLevel.WARN) {
            fprintf(stderr, "%s\n", formattedMessage)
            fflush(stderr)
        }

        if (throwable != null) {
            val stackTrace = formatStackTrace(throwable)

            if (logFile != null) {
                writeToFile(stackTrace)
            }

            if (level == LogLevel.ERROR || level == LogLevel.WARN) {
                fprintf(stderr, "%s\n", stackTrace)
                fflush(stderr)
            }
        }
    }

    fun setLogLevel(level: LogLevel) {
        this.logLevel = level
    }

    fun setLogToStdout(logToStdout: Boolean) {
        this.logToStdout = logToStdout
    }

    fun setLogFile(logFile: String?) {
        // close already opened file
        if (this.logFile != null) {
            fclose(this.logFile)
        }

        if (logFile == null) {
            this.logFile = null
            return
        }

        this.logFile = fopen(logFile, "a")
        if (this.logFile == null) {
            perror("Failed to open log file")
        }
    }

    fun close() {
        if (this.logFile != null) {
            fclose(this.logFile)
            this.logFile = null
        }
    }

    private fun writeToFile(message: String) =
        this.logFile?.let { file ->
            fprintf(file, "%s\n", message)
        }
}

@OptIn(ExperimentalTime::class)
private fun formatLogMessage(level: LogLevel, tag: String, message: String): String {
    val timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val formattedTime = "${timestamp.date} ${timestamp.time.hour.toString().padStart(2, '0')}:" +
            "${timestamp.time.minute.toString().padStart(2, '0')}:" +
            "${timestamp.time.second.toString().padStart(2, '0')}." +
            (timestamp.time.nanosecond / 1000000).toString().padStart(3, '0')
    return "$formattedTime [main] ${level.name.padEnd(5)} $tag - $message"
}

private fun formatStackTrace(throwable: Throwable): String =
    "Exception: ${throwable.message ?: throwable::class.simpleName}"

internal fun Logger.debug(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)

internal fun Logger.info(tag: String, message: String) = log(LogLevel.INFO, tag, message)

internal fun Logger.warn(tag: String, message: String) = log(LogLevel.WARN, tag, message)

internal fun Logger.error(tag: String, message: String, throwable: Throwable? = null) =
    log(LogLevel.ERROR, tag, message, throwable)
