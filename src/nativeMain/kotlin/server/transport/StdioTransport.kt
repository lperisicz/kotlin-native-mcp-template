package server.transport

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import logger.Logger
import logger.debug
import logger.error

internal class StdioTransport : MCPServerTransport {

    companion object {
        private const val TAG = "StdioTransport"
    }

    override suspend fun listen(handler: suspend (Request) -> Response?) =
        coroutineScope {
            while (coroutineContext.isActive) {
                try {
                    val request = readlnOrNull() ?: break

                    Logger.debug(TAG, "Received request: $request")

                    if (request.isBlank()) continue

                    val response = handler(request)

                    if (response == null) {
                        Logger.debug(TAG, "Skipping null response")
                        return@coroutineScope
                    }

                    println(response)
                } catch (e: Exception) {
                    Logger.error(TAG, "Error processing request line: ${e.message}", e)
                }
            }
        }
}