package server

internal object ServerConfig {

    const val DEFAULT_SSE_PORT = 8080

    internal enum class Transport {
        STDIO, SSE
    }
}
