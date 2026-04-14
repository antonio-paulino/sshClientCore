package pt.paulinoo.sshClientCore.api

import pt.paulinoo.sshClientCore.execution.CommandChunk

/**
 * Runs a command with live callbacks for stdout/stderr and returns final exit code.
 * Useful for app UIs that want streaming output without manually collecting chunks.
 */
suspend fun SshSession.executeLive(
    command: String,
    onStdout: (String) -> Unit = {},
    onStderr: (String) -> Unit = {},
): Int {
    var exitCode = -1
    executeStreaming(command).collect { chunk ->
        when (chunk) {
            is CommandChunk.Stdout -> onStdout(chunk.data)
            is CommandChunk.Stderr -> onStderr(chunk.data)
            is CommandChunk.ExitCode -> exitCode = chunk.code
        }
    }
    return exitCode
}
