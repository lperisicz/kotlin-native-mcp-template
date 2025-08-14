package server.util

import io.ktor.server.request.ApplicationRequest

// TODO define rules for this
// TODO check if authorisation is provided client side or server side on initial request?
internal fun ApplicationRequest.extractSessionId(): String? =
    headers["Authorization"]
        ?.substringAfter("Bearer ")
        ?: queryParameters
            .entries()
            .firstOrNull()
            ?.value
            ?.firstOrNull()
            ?.toString()
