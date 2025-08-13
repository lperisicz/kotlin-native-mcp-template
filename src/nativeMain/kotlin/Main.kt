import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import logger.Logger
import logger.debug
import logger.error
import logger.info
import logger.warn

@Serializable
private data class Message(
    val topic: String,
    val content: String,
)

private val PrettyPrintJson = Json {
    prettyPrint = true
}

public fun main() {
    val message = Message(
        topic = "Kotlin/Native",
        content = "Hello!"
    )
    println(PrettyPrintJson.encodeToString(message))

    Logger.setLogFile("build/someFile.log")

    Logger.debug("TAG", "some message for debug")
    Logger.info("TAG", "some message for info")
    Logger.warn("TAG", "some message for warn")
    Logger.error(
        "TAG",
        "Some message for error log level",
        IllegalStateException("Some exception message")
    )
}
