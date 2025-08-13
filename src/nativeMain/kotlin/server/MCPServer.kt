package server

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import logger.Logger
import logger.debug
import logger.error
import logger.info

internal class MCPServer(
    private val transportConfig: ServerConfig.Transport,
    private val port: Int,
) {

    companion object {
        private const val TAG = "MCPServer"
    }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun start() {
        val transport: MCPServerTransport = when (transportConfig) {
            ServerConfig.Transport.STDIO -> StdioTransport()
            ServerConfig.Transport.SSE -> SseTransport(port = port)
        }

        Logger.info(TAG, "Started listening on transport: $transportConfig")

        transport.listen { request ->
            Logger.debug(TAG, "Received request from transport: $request")

            val response = "dummy response"
            // TODO handle requests

            Logger.debug(TAG, "Returning response to transport: $response")

            return@listen response
        }
    }
}

private typealias Request = String
private typealias Response = String

private interface MCPServerTransport {

    suspend fun listen(handler: suspend (Request) -> Response)
}

private class StdioTransport : MCPServerTransport {

    companion object {
        private const val TAG = "StdioTransport"
    }

    override suspend fun listen(handler: suspend (Request) -> Response) =
        coroutineScope {
            while (coroutineContext.isActive) {
                try {
                    val request = readlnOrNull() ?: break

                    Logger.debug(TAG, "Received request: $request")

                    if (request.isBlank()) continue

                    val response = handler(request)

                    println(response)
                } catch (e: Exception) {
                    Logger.error(TAG, "Error processing request line: ${e.message}", e)
                }
            }
        }
}

private class SseTransport(port: Int) : MCPServerTransport {

    companion object {
        private const val TAG = "SseTransport"
    }

    override suspend fun listen(handler: suspend (Request) -> Response) {
        // TODO
    }
}
