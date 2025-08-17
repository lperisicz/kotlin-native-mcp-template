package server.util

import io.ktor.server.request.ApplicationRequest
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.refTo
import platform.posix.closedir
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen
import platform.posix.opendir
import kotlinx.cinterop.toKString
import platform.posix.readdir
import platform.posix.readlink

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



internal fun ApplicationRequest.extractSessionId2(): String? {
    val clientPort = this.local.remotePort  // should be remotePort, not local
    val pid = findPidByPort(clientPort)
    return pid
}


@OptIn(ExperimentalForeignApi::class)
internal fun readFile(path: String): List<String> {
    val file = fopen(path, "r") ?: return emptyList()
    val result = mutableListOf<String>()
    memScoped {
        val bufSize = 4096
        val buffer = allocArray<ByteVar>(bufSize)
        while (true) {
            val line = fgets(buffer, bufSize, file) ?: break
            result.add(line.toKString().trim())
        }
    }
    fclose(file)
    return result
}

@OptIn(ExperimentalForeignApi::class)
internal fun findPidByPort(localPort: Int): String? {
    val hexPort = localPort.toString(16).uppercase().padStart(4, '0')
    val lines = readFile("/proc/net/tcp").drop(1)

    // Look for socket with matching local port
    val entry = lines.firstOrNull { it.contains(":$hexPort ") } ?: return null
    val inode = entry.trim().split(Regex("\\s+"))[9]

    val procDir = opendir("/proc") ?: return null
    try {
        while (true) {
            val dir = readdir(procDir) ?: break
            val pidName = dir.pointed.d_name.toKString()
            if (!pidName.all { it.isDigit() }) continue

            val fdPath = "/proc/$pidName/fd"
            val fdDir = opendir(fdPath) ?: continue
            try {
                while (true) {
                    val fdEntry = readdir(fdDir) ?: break
                    val fdName = fdEntry.pointed.d_name.toKString()
                    if (fdName == "." || fdName == "..") continue

                    val fullFdPath = "$fdPath/$fdName"
                    memScoped {
                        val bufSize = 256
                        val cbuf = allocArray<ByteVar>(bufSize)
                        val bytes = readlink(fullFdPath, cbuf, bufSize.toULong())
                        if (bytes > 0) {
                            val target = cbuf.toKString()
                            if (target.contains("socket:[$inode]")) {
                                return pidName
                            }
                        }
                    }
                }
            } finally {
                closedir(fdDir)
            }
        }
    } finally {
        closedir(procDir)
    }
    return null
}