package server

import kotlinx.serialization.json.Json
import logger.Logger
import logger.debug
import logger.info

internal class MCPServer(
    private val transportConfig: ServerConfig.Transport,
    private val port: Int,
) {

    companion object {
        private const val TAG = "MCPServer"
    }

    private val json = Json { ignoreUnknownKeys = true }

    // TODO maybe there is a bug where crtl C not working? ctrl Z works, sometimes process still
    // exists because PORT already taken after restart
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
