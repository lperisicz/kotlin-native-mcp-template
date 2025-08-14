package server

internal object ServerConfig {

    const val DEFAULT_SSE_PORT = 8080

    // TODO support StreamableHttp transport, maybe as a separate command
    internal enum class Transport {
        STDIO, SSE
    }
}
