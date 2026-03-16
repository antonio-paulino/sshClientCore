package pt.paulinoo.sshClientCore.internal

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.DisconnectReason
import net.schmizz.sshj.common.SecurityUtils
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.DisconnectListener
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import pt.paulinoo.sshClientCore.api.HostKeyVerification
import pt.paulinoo.sshClientCore.exception.UnknownHostKeyRuntimeException
import java.io.File
import java.security.PublicKey

/**
 * Wrapper around SSHJ's SSHClient to handle connection setup, host key verification,
 * authentication, and disconnection events. This class abstracts away the SSHJ specifics
 * and provides a simpler interface for the rest of the application.
 * It also maps SSHJ's host key verification mechanism to our custom HostKeyVerification strategies.
 * The disconnect listener is set up to differentiate between intentional disconnections (BY_APPLICATION)
 * and unexpected connection losses, allowing the application to react accordingly.
 */
internal class SshjClientWrapper {
    private val ssh = SSHClient()

    /**
     * Connects to the SSH server with the specified parameters and host key verification strategy.
     * The host key verification is set up based on the provided strategy:
     * - Promiscuous: Accepts all host keys without verification.
     * - Fingerprint: Verifies the host key against a specific expected fingerprint.
     * - KnownHosts: Verifies the host key against entries in a known_hosts file.
     * - Strict: Verifies the host key against a set of accepted fingerprints, rejecting unknown keys.
     * The connection and read timeouts are configured according to the provided timeoutMillis parameter.
     *
     * @param host The SSH server hostname or IP address.
     * @param port The SSH server port.
     * @param keepAliveSeconds The interval in seconds for sending keep-alive messages.
     * @param timeoutMillis The connection and read timeout in milliseconds.
     * @param verification The host key verification strategy to use for this connection.
     * @throws UnknownHostKeyRuntimeException if the host key verification fails in Strict mode, containing details
     * about the unknown key.
     */
    fun connect(
        host: String,
        port: Int,
        keepAliveSeconds: Int,
        timeoutMillis: Long,
        verification: HostKeyVerification,
    ) {
        when (verification) {
            is HostKeyVerification.Promiscuous -> {
                ssh.addHostKeyVerifier(PromiscuousVerifier())
            }
            is HostKeyVerification.Fingerprint -> {
                ssh.addHostKeyVerifier(verification.expected)
            }
            is HostKeyVerification.KnownHosts -> {
                ssh.loadKnownHosts(verification.file)
            }

            is HostKeyVerification.Strict -> {
                ssh.addHostKeyVerifier(
                    object : HostKeyVerifier {
                        override fun verify(
                            hostname: String,
                            port: Int,
                            key: PublicKey,
                        ): Boolean {
                            val fingerprint = SecurityUtils.getFingerprint(key)

                            if (verification.acceptedFingerprints.contains(fingerprint)) {
                                return true
                            } else {
                                throw UnknownHostKeyRuntimeException(hostname, fingerprint, key.algorithm)
                            }
                        }

                        override fun findExistingAlgorithms(
                            hostname: String?,
                            port: Int,
                        ): List<String> = emptyList()
                    },
                )
            }
        }

        ssh.connectTimeout = timeoutMillis.toInt()
        ssh.timeout = timeoutMillis.toInt()

        ssh.connect(host, port)

        if (keepAliveSeconds > 0) {
            ssh.connection.keepAlive.keepAliveInterval = keepAliveSeconds
        }
    }

    /**
     * Sets a disconnect listener to handle disconnection events from the SSH server. The listener will be called with:
     * - null if the disconnection was intentional (BY_APPLICATION), allowing the application to clean
     * up resources without treating it as an error.
     * - an Exception with details about the disconnection reason and message if the connection was lost
     * unexpectedly, allowing the application to react to connection losses (e.g., by attempting to reconnect or
     * notifying the user).
     *
     * @param listener A callback function that takes a Throwable? parameter, which will be null for intentional
     * disconnections and an Exception for unexpected connection losses.
     */
    fun onDisconnect(listener: (Throwable?) -> Unit) {
        ssh.transport.disconnectListener =
            DisconnectListener { reason, message ->
                if (reason == DisconnectReason.BY_APPLICATION) {
                    listener(null)
                } else {
                    listener(Exception("Connection lost: $reason - $message"))
                }
            }
    }

    /**
     * Authenticates with the SSH server using the provided username and password. This method will attempt to
     * authenticate using SSHJ's password authentication mechanism. If the authentication fails, an exception will be
     * thrown, which should be handled by the caller to provide appropriate feedback to the user or take corrective
     * action.
     *
     * @param user The username to authenticate with.
     * @param pass The password to authenticate with.
     * @throws Exception if the authentication fails, such as due to incorrect credentials or connection issues
     */
    fun authPassword(
        user: String,
        pass: String,
    ) = ssh.authPassword(user, pass)

    /**
     * Authenticates with the SSH server using the provided username and private key file. This method will attempt to
     * authenticate using SSHJ's public key authentication mechanism. The private key file should be in a format
     * supported by SSHJ (e.g., OpenSSH format). If the authentication fails, an exception will be thrown, which should
     * be handled by the caller to provide appropriate feedback to the user or take corrective action.
     *
     * @param user The username to authenticate with.
     * @param key The private key file to authenticate with.
     * @throws Exception if the authentication fails, such as due to incorrect credentials, unsupported key format, or
     * connection issues
     */
    fun authKey(
        user: String,
        key: File,
    ) = ssh.authPublickey(user, key.absolutePath)

    /**
     * Starts a new SSH session on the established connection. This method will create and return a new Session object
     * from SSHJ, which can be used to execute commands, open shells, and perform other SSH operations. The caller is
     * responsible for managing the lifecycle of the session, including closing it when finished to free up resources
     * on the server.
     *
     * @return A new Session object representing the SSH session that can be used for executing commands and other
     * operations.
     * @throws Exception if an error occurs while starting the session, such as connection issues or server problems,
     * which should be handled by the caller to provide appropriate feedback or take corrective action.
     */
    fun startSession(): Session = ssh.startSession()

    /**
     * Disconnects the SSH client, closing the underlying connection to the SSH server. This method should be called
     * when the SSH session is no longer needed to free up resources and ensure a clean shutdown of the connection.
     * After calling this method, the onDisconnect listener will be triggered with a null parameter, indicating an
     * intentional disconnection.
     * @throws Exception if an error occurs during disconnection, such as network issues or server problems, which
     * should be handled by the caller to provide appropriate feedback or take corrective action.
     */
    fun disconnect() = ssh.disconnect()

    /**
     * Returns the underlying SSHClient instance from SSHJ. This method is intended for internal use within the library
     * to allow access to SSHJ's features that may not be directly exposed through the wrapper. Consumers of the
     * library should not need to interact with the raw SSHClient instance, and it is recommended to use the provided
     * methods in the wrapper for most operations to ensure proper handling of connections, authentication, and error
     * management.
     */
    fun raw(): SSHClient = ssh
}
