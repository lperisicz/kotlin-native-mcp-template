package server.tool

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import logger.Logger
import logger.debug
import server.models.ToolCallParams
import server.models.ToolCallResult
import server.models.ToolContent

private val jokes = listOf(
    "Why do programmers prefer dark mode? Because light attracts bugs!",
    "A SQL query goes into a bar, walks up to two tables and asks, \"Can I join you?\"",
    "How many programmers does it take to change a light bulb? None. That's a hardware problem.",
    "Why do Java developers wear glasses? Because they can't C#!",
    "There are only 10 types of people in the world: those who understand binary and those who don't.",
    "A programmer is told to \"go to hell.\" He finds the worst part of that statement is the \"go to.\"",
    "Why did the programmer quit his job? He didn't get arrays.",
    "How do you comfort a JavaScript bug? You console it.",
    "Why do programmers hate nature? It has too many bugs.",
    "What's the object-oriented way to become wealthy? Inheritance.",
    "Why did the developer go broke? Because he used up all his cache.",
    "A byte walks into a bar looking miserable. The bartender asks, \"What's wrong?\" The byte replies, \"Parity error.\"",
    "Why do programmers prefer iOS development? Because it's Objective-C.",
    "What do you call a programmer from Finland? Nerdic.",
    "Why don't programmers like nature? It has too many bugs and not enough documentation.",
    "How do you generate a random string? Put a web designer in front of VIM and tell them to save and exit.",
    "Why did the programmer break up with his girlfriend? She had one too many bugs and he couldn't patch their relationship.",
    "What's a programmer's favorite hangout place? Foo Bar.",
    "Why do programmers always mix up Halloween and Christmas? Because Oct 31 equals Dec 25.",
    "A programmer's wife tells him: \"Run to the store and pick up a loaf of bread. If they have eggs, get a dozen.\" The programmer comes home with 12 loaves of bread.",
)

internal object ExampleTool : Tool {

    private const val TAG = "ExampleTool"

    override val toolDefinition = Tool.ToolDefinition(
        name = "get_random_it_joke",
        description = "Returns a random computer programming joke",
        inputSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {})
            put("required", JsonArray(emptyList()))
        },
    )

    override suspend fun invoke(params: ToolCallParams): ToolCallResult {
        Logger.debug(TAG, "Tool called with: $params")

        val randomJoke = jokes.random()

        val content = listOf(
            ToolContent(
                type = "text",
                text = randomJoke,
            )
        )

        return ToolCallResult(content = content, isError = false)
    }
}