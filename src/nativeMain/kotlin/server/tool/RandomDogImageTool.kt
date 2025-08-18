package server.tool

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.curl.Curl
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import logger.Logger
import logger.debug
import logger.error
import server.models.ToolCallParams
import server.models.ToolCallResult
import server.models.ToolContent

@Serializable
private data class DogApiResponse(
    val message: String,
    val status: String
)

internal object RandomDogImageTool : Tool {

    private const val TAG = "RandomDogImageTool"
    private const val DOG_API_URL = "https://dog.ceo/api/breeds/image/random"

    private val httpClient = HttpClient(Curl) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    override val toolDefinition = Tool.ToolDefinition(
        name = "get_random_dog_image",
        description = "Fetches a random dog image URL from the Dog CEO API",
        inputSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {})
            put("required", JsonArray(emptyList()))
        },
    )

    override suspend fun invoke(params: ToolCallParams): ToolCallResult {
        Logger.debug(TAG, "Tool called with: $params")

        return try {
            val response = httpClient.get(DOG_API_URL).body<DogApiResponse>()

            if (response.status == "success") {
                val content = listOf(
                    ToolContent(
                        type = "text",
                        text = "Random dog image: ${response.message}",
                    )
                )
                ToolCallResult(content = content, isError = false)
            } else {
                Logger.error(TAG, "Dog API returned non-success status: ${response.status}")
                val content = listOf(
                    ToolContent(
                        type = "text",
                        text = "Failed to fetch dog image: API returned status ${response.status}",
                    )
                )
                ToolCallResult(content = content, isError = true)
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to fetch random dog image", e)
            val content = listOf(
                ToolContent(
                    type = "text",
                    text = "Failed to fetch random dog image: ${e.message}",
                )
            )
            ToolCallResult(content = content, isError = true)
        }
    }
}