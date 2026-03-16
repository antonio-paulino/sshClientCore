package pt.paulinoo.sshClientCore.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import pt.paulinoo.sshClientCore.connection.ConnectionState
import pt.paulinoo.sshClientCore.execution.CommandChunk
import pt.paulinoo.sshClientCore.execution.CommandResult
import pt.paulinoo.sshClientCore.shell.ShellSession

/**
 * Represents an active SSH session. It provides methods to execute commands, open a shell, and monitor the connection
 * state.
 * The session is designed to be used within a coroutine context, allowing for asynchronous operations and streaming
 * command outputs.
 * The connection state can be observed through the [connectionState] flow, which emits updates on the connection
 * status, including any errors that may occur.
 * The [execute] method allows for executing a command and waiting for its completion, returning the result.
 * The [executeStreaming] method provides a flow of command output chunks, allowing for real-time processing of command
 * output. The [openShell] method opens an interactive shell session, and the [disconnect] method cleanly closes the
 * SSH connection.
 * Implementations of this interface should handle the underlying SSH connection management, including error handling
 * and resource cleanup, to ensure a robust and reliable SSH client experience.
 */
interface SshSession {
    /**
     * A flow that emits updates on the connection state of the SSH session. It can emit [ConnectionState.Connected],
     * [ConnectionState.Disconnected], or [ConnectionState.Error] with an associated error message.
     * Consumers can collect this flow to react to changes in the connection state, such as handling disconnections
     * or errors gracefully.
     */
    val connectionState: StateFlow<ConnectionState>

    /**
     * Executes a command on the remote SSH server and waits for its completion. The result of the command execution,
     * including the exit code, standard output, and standard error, is returned as a [CommandResult].
     * This method is designed for commands that are expected to complete in a reasonable amount of time and where the
     * full output is needed after execution. For long-running commands or when real-time output processing is desired,
     * consider using the [executeStreaming] method instead.
     * @param command The command to be executed on the remote server.
     * @return A [CommandResult] containing the exit code, standard output, and standard error of the executed command.
     * @throws Exception If an error occurs during command execution, such as connection issues or command failures, an
     * exception will be thrown. Implementations should provide meaningful error messages to aid in debugging and error
     * handling.
     */
    suspend fun execute(command: String): CommandResult

    /**
     * Executes a command on the remote SSH server and returns a flow of [CommandChunk] that emits chunks of the
     * command's output in real-time. This allows for processing the output as it is generated, which is particularly
     * useful for long-running commands or when immediate feedback is desired.
     * The flow will emit [CommandChunk.Stdout] for standard output, [CommandChunk.Stderr] for standard error, and a
     * final [CommandChunk.ExitCode] when the command execution completes. Consumers can collect this flow to handle
     * the command output incrementally and react to it as needed.
     * @param command The command to be executed on the remote server.
     * @return A flow of [CommandChunk] that emits the command's output in real-time, including standard output,
     * standard error, and the exit code upon completion.
     * @throws Exception If an error occurs during command execution, such as connection issues or command failures,
     * an exception will be thrown. Implementations should provide meaningful error messages to aid in debugging and
     * error handling.
     */
    fun executeStreaming(command: String): Flow<CommandChunk>

    /**
     * Opens an interactive shell session on the remote SSH server.
     * This allows for sending input to the shell and receiving output in real-time, enabling a more dynamic
     * interaction with the remote environment.
     * The returned [ShellSession] provides methods to send data, resize the terminal, and close the session when
     * finished.
     * This method is useful for scenarios where a persistent shell session is needed, such as when running multiple
     * commands in sequence or when an interactive session is required.
     * Consumers should ensure that the shell session is properly closed after use to free up resources on the remote
     * server.
     * @return A [ShellSession] that represents the interactive shell session on the remote server, allowing for
     * sending input and receiving output in real-time.
     * @throws Exception If an error occurs while opening the shell session, such as connection issues or server
     * limitations, an exception will be thrown. Implementations should provide meaningful error messages to aid in
     * debugging and error handling.
     */
    suspend fun openShell(): ShellSession

    /**
     * Disconnects the SSH session, closing the underlying connection to the remote server. This method should be
     * called when the session is no longer needed to free up resources and ensure a clean shutdown of the connection.
     * After calling this method, the [connectionState] flow will emit a [ConnectionState.Disconnected] state, and any
     * further attempts to execute commands or open a shell will result in an error. Consumers should handle the
     * disconnection gracefully, ensuring that any ongoing operations are completed or canceled as needed before
     * disconnecting.
     * @throws Exception If an error occurs during disconnection, such as network issues or server problems, an
     * exception will be thrown. Implementations should provide meaningful error messages to aid in debugging and
     * error handling.
     */
    suspend fun disconnect()
}
