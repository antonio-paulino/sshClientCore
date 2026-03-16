package pt.paulinoo.sshClientCore.client

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pt.paulinoo.sshClientCore.api.SshSession
import pt.paulinoo.sshClientCore.connection.ConnectionState
import pt.paulinoo.sshClientCore.execution.CommandChunk
import pt.paulinoo.sshClientCore.execution.CommandExecutor
import pt.paulinoo.sshClientCore.execution.CommandResult
import pt.paulinoo.sshClientCore.internal.SshjClientWrapper
import pt.paulinoo.sshClientCore.shell.DefaultShellSession
import pt.paulinoo.sshClientCore.shell.ShellSession

/**
 * Default implementation of [SshSession] using SSHJ library.
 * Manages the SSH connection state and provides methods to execute commands and open shell sessions.
 * The connection state is exposed as a [StateFlow] to allow observers to react to changes in the connection status.
 * @param wrapper The SSHJ client wrapper to manage the SSH connection.
 */
internal class DefaultSshSession(
    private val wrapper: SshjClientWrapper,
) : SshSession {
    private val state = MutableStateFlow<ConnectionState>(ConnectionState.Connected)
    override val connectionState: StateFlow<ConnectionState> = state
    private val executor = CommandExecutor()

    init {
        wrapper.onDisconnect { error ->
            if (error != null) {
                state.value = ConnectionState.Error(error)
            } else {
                state.value = ConnectionState.Disconnected
            }
        }
    }

    override suspend fun execute(command: String): CommandResult =
        wrapper.startSession().use { session ->
            executor.executeBlocking(session, command)
        }

    override fun executeStreaming(command: String): Flow<CommandChunk> {
        val session = wrapper.startSession()
        return executor.executeStreaming(session, command)
    }

    override suspend fun openShell(): ShellSession = DefaultShellSession(wrapper.raw())

    override suspend fun disconnect() {
        wrapper.disconnect()
        state.value = ConnectionState.Disconnected
    }
}
