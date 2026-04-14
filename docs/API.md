# API Reference

This document describes the public API of `sshClientCore`.

## Main Entry Points

### `Ssh.createClient()`
Creates a new `SshClient` instance.

### `SshClient.create()`
Alternative factory available from the `SshClient` companion object.

## Core Models

### `SshConfig`
Connection configuration.

Main fields:
- `host: String`
- `port: Int = 22`
- `username: String`
- `password: String?`
- `privateKey: File?`
- `timeoutMillis: Long = 10000`
- `keepAliveIntervalSeconds: Int = 15`
- `hostKeyVerification: HostKeyVerification = Promiscuous`
- `backend: SshBackend = SSHJ`

### `SshBackend`
Selects the SSH engine:
- `SSHJ`
- `JSCH`

### `HostKeyVerification`
Host key policies:
- `Promiscuous`
- `Fingerprint(expected)`
- `KnownHosts(file)`
- `Strict(acceptedFingerprints)`

## Client and Session

### `SshClient`
- `suspend fun connect(config: SshConfig): SshSession`

Expected exceptions:
- `ConnectionException`
- `AuthenticationException`
- `UnknownHostKeyException`

### `SshSession`
- `val connectionState: StateFlow<ConnectionState>`
- `suspend fun execute(command: String): CommandResult`
- `fun executeStreaming(command: String): Flow<CommandChunk>`
- `suspend fun openShell(): ShellSession`
- `suspend fun disconnect()`

### `SshSession` extensions
- `suspend fun executeLive(command, onStdout, onStderr): Int`

## Command Execution Types

### `CommandResult`
- `stdout: String`
- `stderr: String`
- `exitCode: Int`

### `CommandChunk`
Streaming output variants:
- `Stdout(data: String)`
- `Stderr(data: String)`
- `ExitCode(code: Int)`

## Interactive Shell

### `ShellSession`
- `val output: Flow<ByteArray>`
- `suspend fun send(data: ByteArray)`
- `suspend fun send(text: String)`
- `suspend fun sendKey(key: TerminalKey)`
- `suspend fun resize(cols: Int, rows: Int)`
- `suspend fun close()`

### `ShellSession` extensions
- `fun consumeOutput(scope, onBytes): Job`
- `suspend fun sendLine(line: String)`

### `TerminalKey`
Built-in control keys (`CtrlC`, arrows, `Enter`, etc.) and `TerminalKey.ctrl('C')` helper.

