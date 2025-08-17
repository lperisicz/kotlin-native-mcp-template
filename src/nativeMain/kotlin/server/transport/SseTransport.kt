package server.transport

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.EngineMain
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveText
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import logger.Logger
import logger.debug
import server.util.extractSessionId
import kotlin.coroutines.resume

// TODO Servers MUST validate the Origin header on all incoming connections to prevent DNS rebinding attacks
// TODO Servers SHOULD implement proper authentication for all connections

private typealias SessionId = String

internal typealias RequestChannel = Channel<Request>

private fun Channel.Factory.newRequestChannel(): RequestChannel = Channel(capacity = CONFLATED)

// TODO ping/pong needed?
internal class SseTransport(
    private val port: Int,
) : MCPServerTransport {

    companion object {
        private const val TAG = "SseTransport"
    }

    // TODO probably remember mutable map of sessionId to eventChannel?
    private val sessionMutex = Mutex()
    private val sessionStore: MutableMap<SessionId, RequestChannel> = mutableMapOf()
    private var server: EmbeddedServer<*, *>? = null

    override suspend fun listen(handler: suspend (Request) -> Response?): Unit =
        suspendCancellableCoroutine { continuation ->
            Logger.debug(TAG, "Starting SSE server on port: $port")
            // TODO move ktor logs to Logger
            server = runBlocking { createServer(handler) }

            continuation.invokeOnCancellation { cause: Throwable? ->
                Logger.debug(TAG, "Stopping SSE transport due to cancellation: ${cause?.message}")

                server?.stop(gracePeriodMillis = 2000, timeoutMillis = 5000)
                continuation.resume(Unit)
            }

            server?.start(wait = true)
        }
    
    fun shutdown() {
        Logger.debug(TAG, "Shutting down SSE transport...")
        try {
            // Close all session channels first
            runBlocking {
                sessionMutex.withLock {
                    sessionStore.values.forEach { channel ->
                        try {
                            channel.close()
                        } catch (e: Exception) {
                            Logger.debug(TAG, "Error closing session channel: ${e.message}")
                        }
                    }
                    sessionStore.clear()
                }
                
                Logger.debug(TAG, "Starting server.stop() call...")
                // Use immediate stop with minimal grace period
                server?.stopSuspend()
                // server?.stop(gracePeriodMillis = 100, timeoutMillis = 200)
            }
            Logger.debug(TAG, "SSE transport shutdown completed successfully")
        } catch (e: Exception) {
            Logger.debug(TAG, "Error during SSE transport shutdown: ${e.message}")
        }
    }

    private fun createServer2(handler: suspend (Request) -> Response?) =
        EngineMain.createServer(arrayOf("-port=8080"))

    private suspend fun createServer(handler: suspend (Request) -> Response?) =
        coroutineScope {
            this.
            embeddedServer(
                factory = CIO,
                port = port,
            ) {
                install(SSE)

                routing {
                    post("/message") {
                        val request = call.receiveText()

                        // TODO provide error state to client
                        val sessionId = call.request.extractSessionId()

                        if (sessionId == null) {
                            Logger.debug(TAG, "No sessionId")
                            return@post
                        }

                        // TODO provide error state to client
                        val requestChannel = sessionMutex.withLock { sessionStore[sessionId] }

                        if (requestChannel == null) {
                            Logger.debug(TAG, "No requestChannel for sessionId: $sessionId")
                            return@post
                        }

                        requestChannel.send(request)

                        Logger.debug(TAG, "Received POST \"/message\" with: $request")

                        call.respond(
                            HttpStatusCode.OK,
                            """{"jsonrpc":"2.0","id":"ack","result":"Message received"}"""
                        )
                    }

                    // TODO clear session from store in case of an exception
                    sse("/sse") {
                        Logger.debug(TAG, "Received GET on \"/sse\" with: ${call.request.uri}")

                        // TODO assuming first query parameter is the sessionIdwith
                        val sessionId = call.request.extractSessionId()

                        // TODO provide error state to client
                        if (sessionId == null) {
                            Logger.debug(TAG, "No sessionId")

                            close()
                            return@sse
                        }

                        val event = ServerSentEvent(
                            // TODO provide pull path dynamically
                            data = "http://localhost:$port/message",
                            event = "endpoint"
                        )

                        val requestsChannel = Channel.newRequestChannel()
                        sessionMutex.withLock {
                            sessionStore.put(sessionId, requestsChannel)
                        }

                        Logger.debug(
                            TAG,
                            "SessionId: $sessionId - Sending initial endpoint event: $event"
                        )

                        send(event)

                        for (request in requestsChannel) {
                            Logger.debug(TAG, "Received request from channel: $request")
                            val response = handler(request)

                            if (response == null) {
                                Logger.debug(TAG, "Skipping null response")
                                continue
                            }

                            Logger.debug(TAG, "Sending response : $response")
                            send(
                                ServerSentEvent(
                                    data = response,
                                    event = "message",
                                )
                            )
                        }

                        // TODO check how to close correctly
                        Logger.debug(TAG, "Closing connection on \"/sse\"")
                    }
                }
            }
        }
}