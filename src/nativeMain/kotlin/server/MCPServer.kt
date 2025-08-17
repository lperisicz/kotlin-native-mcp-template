package server

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import logger.Logger
import logger.debug
import logger.error
import logger.info
import server.models.InitializeParams
import server.models.InitializeResult
import server.models.JsonRpcRequest
import server.models.JsonRpcResponse
import server.models.ServerInfo
import server.models.ToolCallParams
import server.models.ToolsListResult
import server.tool.Tool
import server.tool.toToolItem
import server.transport.MCPServerTransport
import server.transport.Request
import server.transport.SseTransport
import server.transport.StdioTransport

internal class MCPServer(
    private val transportConfig: ServerConfig.Transport,
    private val tools: List<Tool> = emptyList(),
    port: Int = ServerConfig.DEFAULT_SSE_PORT,
) {

    companion object {
        private const val TAG = "MCPServer"
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val transport: MCPServerTransport = when (transportConfig) {
        ServerConfig.Transport.STDIO -> StdioTransport()
        ServerConfig.Transport.SSE -> SseTransport(port = port)
    }
    
    fun shutdown() {
        Logger.info(TAG, "Shutting down MCP server...")
        if (transport is SseTransport) {
            transport.shutdown()
        }
    }

    // TODO maybe there is a bug where crtl C not working? ctrl Z works, sometimes process still
    // exists because PORT already taken after restart
    suspend fun start() {
        Logger.info(TAG, "Started listening on transport: $transportConfig")

        transport.listen { request ->
            Logger.debug(TAG, "Received request from transport: $request")

            val decodedRequest = decodeRequest(request)

            if (decodedRequest == null) {
                // TODO return appropriate malformed exception response
                Logger.debug(TAG, "Decoded request is null, request: $request")
                return@listen "Invalid request"
            }

            val response = handleRequest(decodedRequest)

            if (response == null) {
                Logger.debug(TAG, "Response is null")
                return@listen null
            }

            val encodedResponse = encodeResponse(response)

            if (encodedResponse == null) {
                // TODO return appropriate internal error response
                Logger.debug(TAG, "Encoded response is null, response: $response")
                return@listen "Invalid request"
            }

            Logger.debug(TAG, "Returning response to transport: $response")

            return@listen encodedResponse
        }
    }

    private fun decodeRequest(request: Request): JsonRpcRequest? =
        try {
            json.decodeFromString<JsonRpcRequest>(request)
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to decode json rpc request", e)
            null
        }

    private fun encodeResponse(response: JsonRpcResponse): String? =
        try {
            json.encodeToString(response)
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to encode json rpc response", e)
            null
        }

    private suspend fun handleRequest(request: JsonRpcRequest): JsonRpcResponse? =
        try {
            when (request.method) {
                "initialize" -> {
                    Logger.info(TAG, "Handling initialize request")
                    handleInitialize(request)
                }

                "notifications/initialized" -> {
                    Logger.info(
                        TAG,
                        "Received initialized notification"
                    )
                    // TODO handle initialized status, maybe block some actions for sessionId
                    // until initialized
                    // either save sessionId to initialized status on a server or in the transport?
                    // initialized = true
                    null
                }

                "tools/list" -> {
                    Logger.info(TAG, "Handling tools list request")
                    handleToolList(request)
                }

                "tools/call" -> {
                    Logger.info(TAG, "Handling tools call")
                    handleToolCall(request)
                }

                else -> {
                    Logger.info(TAG, "Handling unknown request method: ${request.method}")
                    null
                }
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to handle json rpc request", e)
            null
        }

    private fun handleInitialize(request: JsonRpcRequest): JsonRpcResponse {

        // TODO add better error handling
        val params = request.params?.let {
            json.decodeFromJsonElement<InitializeParams>(it)
        } ?: return createErrorResponse(request.id, -32602, "Invalid params")

        // TODO could be a separate model
        val capabilities = buildJsonObject {
            put("tools", buildJsonObject {
                // TODO check if notifications should be implemented for this
                put("listChanged", false)
            })
        }

        val serverInfo = ServerInfo(
            name = "KotlinNativeMCPTemplate",
            version = "0.0.1"
        )

        val result = InitializeResult(
            // TODO check which versions are supported
            protocolVersion = params.protocolVersion,
            capabilities = capabilities,
            serverInfo = serverInfo
        )

        Logger.info(
            TAG,
            "Server initialized successfully with protocol version ${result.protocolVersion}"
        )

        return JsonRpcResponse(
            jsonrpc = "2.0",
            id = request.id,
            result = json.encodeToJsonElement(result)
        )
    }

    private fun handleToolList(request: JsonRpcRequest): JsonRpcResponse {
        Logger.debug(TAG, "Listing available tools")

        val result = ToolsListResult(
            tools = tools.map(Tool::toToolItem)
        )

        Logger.debug(TAG, "Available tools: ${result.tools.joinToString { it.name }}")

        return JsonRpcResponse(
            jsonrpc = "2.0",
            id = request.id,
            result = json.encodeToJsonElement(result)
        )
    }

    private suspend fun handleToolCall(request: JsonRpcRequest): JsonRpcResponse {
        val params = request.params?.let {
            json.decodeFromJsonElement<ToolCallParams>(it)
        } ?: return createErrorResponse(request.id, -32602, "Invalid params")

        Logger.debug(TAG, "Calling tool: ${params.name}")

        val targetTool = tools.firstOrNull { it.toolDefinition.name == params.name }

        if (targetTool == null) {
            return createErrorResponse(request.id, -32602, "Invalid params")
        }

        val result = targetTool.invoke(params)

        Logger.debug(TAG, "Returning tool result: $result")

        return JsonRpcResponse(
            jsonrpc = "2.0",
            id = request.id,
            result = json.encodeToJsonElement(result)
        )
    }

    private fun createErrorResponse(id: JsonElement?, code: Int, message: String): JsonRpcResponse {
        Logger.error(TAG, "Creating error response - Code: $code, Message: $message")
        val error = buildJsonObject {
            put("code", code)
            put("message", message)
        }

        return JsonRpcResponse(
            jsonrpc = "2.0",
            id = id,
            error = error
        )
    }
}

