# Kotlin/Native MCP Server

A Model Context Protocol (MCP) server implementation written in Kotlin/Native that compiles to a native executable.

## Building

```bash
./gradlew build
```

The executable will be created at `build/bin/native/releaseExecutable/KotlinNativeMCPTemplate.kexe`

## Usage

The server supports two transport modes: stdio and SSE (Server-Sent Events).

### Stdio Transport

```bash
./KotlinNativeMCPTemplate.kexe stdio --log-file mcp-server.log --log-level DEBUG
```

### SSE Transport  

```bash
./KotlinNativeMCPTemplate.kexe sse --port 8080 --log-file mcp-server.log --log-level DEBUG
```
### Options

- `--log-level`: Set log verbosity (DEBUG, INFO, WARN, ERROR)
- `--log-file`: Path to log file
- `--log-stdout`: Enable console logging
- `--port, -p`: SSE server port (default: 8080)

### Help

```bash
./KotlinNativeMCPTemplate.kexe --help
./KotlinNativeMCPTemplate.kexe sse --help
```

## Features

- **Native executable**: Single binary with no runtime dependencies
- **MCP Protocol**: Implements Model Context Protocol for tool integration
- **Multiple transports**: Stdio and SSE support (StreamableHttp TBD)
- **Example tool**: Includes a joke generator tool for testing

## Project Structure

- `src/nativeMain/kotlin/cmd/` - CLI argument parsing and main entry point
- `src/nativeMain/kotlin/server/` - MCP server implementation
- `src/nativeMain/kotlin/server/tool/` - Tool definitions
- `src/nativeMain/kotlin/logger/` - Logging utilities
