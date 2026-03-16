package pt.paulinoo.api

import pt.paulinoo.client.DefaultSshClient
import pt.paulinoo.exception.AuthenticationException
import pt.paulinoo.exception.ConnectionException
import pt.paulinoo.exception.UnknownHostKeyException

/**
 * Represents an SSH client that can establish connections to remote servers.
 *
 */
interface SshClient {
    /**
     * Establishes an SSH connection to a remote server using the provided configuration.
     * @param config The SSH configuration containing connection details such as host, port, username, and
     * authentication method.
     * @return An [SshSession] representing the established SSH session.
     * @throws ConnectionException If the connection to the remote server fails.
     * @throws AuthenticationException If authentication with the remote server fails.
     * @throws UnknownHostKeyException If the host key verification fails due to an unknown host key.
     */
    suspend fun connect(config: SshConfig): SshSession

    companion object {
        fun create(): SshClient = DefaultSshClient()

    }
}
