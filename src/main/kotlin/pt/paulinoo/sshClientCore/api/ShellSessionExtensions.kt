package pt.paulinoo.sshClientCore.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pt.paulinoo.sshClientCore.shell.ShellSession

/**
 * Starts collecting shell output in the provided scope.
 * Returns the Job so callers can cancel collection with their lifecycle.
 */
fun ShellSession.consumeOutput(
    scope: CoroutineScope,
    onBytes: (ByteArray) -> Unit,
): Job =
    scope.launch {
        output.collect { onBytes(it) }
    }

/**
 * Sends a line with a trailing newline (`\n`).
 */
suspend fun ShellSession.sendLine(line: String) {
    send(line + "\n")
}
