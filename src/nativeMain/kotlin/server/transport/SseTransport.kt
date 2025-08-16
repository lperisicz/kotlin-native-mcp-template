package server.transport

import game.ApiGameState
import game.GameState
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receiveText
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import logger.Logger
import logger.debug
import server.util.extractSessionId

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

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun listen(handler: suspend (Request) -> Response?) {
        Logger.debug(TAG, "Starting SSE server on port: $port")
        // TODO move ktor logs to Logger
        embeddedServer(
            factory = CIO,
            port = port,
        ) {
            install(SSE)

            install(CORS) {
                anyHost()
                allowHeader(HttpHeaders.ContentType)
                allowMethod(HttpMethod.Get)
            }

            routing {
                get("/game-state") {
                    call.respond(
                        HttpStatusCode.OK,
                        json.encodeToString(ApiGameState(GameState.state))
                    )
                }

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
        }.startSuspend(wait = true)
    }
}