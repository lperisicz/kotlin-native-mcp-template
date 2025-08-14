package server.transport

internal typealias Request = String
internal typealias Response = String

internal interface MCPServerTransport {

    // TODO inspect the possibility of where to perform parallel executions
    suspend fun listen(handler: suspend (Request) -> Response?)
}
