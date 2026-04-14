package pt.paulinoo.sshClientCore.internal

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.DisconnectReason
import net.schmizz.sshj.common.SecurityUtils
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.DisconnectListener
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.UserAuthException
import pt.paulinoo.sshClientCore.api.HostKeyVerification
import pt.paulinoo.sshClientCore.api.SshConfig
import pt.paulinoo.sshClientCore.exception.AuthenticationException
import pt.paulinoo.sshClientCore.exception.UnknownHostKeyRuntimeException
import java.security.PublicKey

/**
 * Wrapper around SSHJ's SSHClient to handle connection setup, host key verification,
 * authentication, and disconnection events. This class abstracts away the SSHJ specifics
 * and provides a simpler interface for the rest of the application.
 * It also maps SSHJ's host key verification mechanism to our custom HostKeyVerification strategies.
 * The disconnect listener is set up to differentiate between intentional disconnections (BY_APPLICATION)
 * and unexpected connection losses, allowing the application to react accordingly.
 */
internal class SshjClientWrapper : SshBackendClient {
    private val ssh = SSHClient()

    override fun connect(config: SshConfig) {
        when (config.hostKeyVerification) {
            is HostKeyVerification.Promiscuous -> {
                ssh.addHostKeyVerifier(PromiscuousVerifier())
            }
            is HostKeyVerification.Fingerprint -> {
                ssh.addHostKeyVerifier(config.hostKeyVerification.expected)
            }
            is HostKeyVerification.KnownHosts -> {
                ssh.loadKnownHosts(config.hostKeyVerification.file)
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

                            if (config.hostKeyVerification.acceptedFingerprints.contains(fingerprint)) {
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

        ssh.connectTimeout = config.timeoutMillis.toInt()
        ssh.timeout = config.timeoutMillis.toInt()

        ssh.connect(config.host, config.port)

        if (config.keepAliveIntervalSeconds > 0) {
            ssh.connection.keepAlive.keepAliveInterval = config.keepAliveIntervalSeconds
        }

        try {
            when {
                config.password != null -> ssh.authPassword(config.username, config.password)
                config.privateKey != null -> ssh.authPublickey(config.username, config.privateKey.absolutePath)
                else -> error("No auth method")
            }
        } catch (e: UserAuthException) {
            throw AuthenticationException(e)
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
    override fun onDisconnect(listener: (Throwable?) -> Unit) {
        ssh.transport.disconnectListener =
            DisconnectListener { reason, message ->
                if (reason == DisconnectReason.BY_APPLICATION) {
                    listener(null)
                } else {
                    listener(Exception("Connection lost: $reason - $message"))
                }
            }
    }

    override fun startExec(command: String): ExecChannel {
        val session = ssh.startSession()
        val commandChannel = session.exec(command)
        return object : ExecChannel {
            override val stdout = commandChannel.inputStream
            override val stderr = commandChannel.errorStream

            override fun join() {
                commandChannel.join()
            }

            override fun exitStatus(): Int = commandChannel.exitStatus ?: -1

            override fun close() {
                commandChannel.close()
                session.close()
            }
        }
    }

    override fun startShell(
        cols: Int,
        rows: Int,
    ): ShellChannel {
        val session: Session = ssh.startSession()
        session.allocatePTY("xterm-256color", cols, rows, 0, 0, emptyMap())
        val shell = session.startShell()

        return object : ShellChannel {
            override val input = shell.inputStream
            override val output = shell.outputStream

            override fun resize(
                cols: Int,
                rows: Int,
            ) {
                shell.changeWindowDimensions(cols, rows, 0, 0)
            }

            override fun close() {
                shell.close()
                session.close()
            }
        }
    }

    /**
     * Disconnects the SSH client, closing the underlying connection to the SSH server. This method should be called
     * when the SSH session is no longer needed to free up resources and ensure a clean shutdown of the connection.
     * After calling this method, the onDisconnect listener will be triggered with a null parameter, indicating an
     * intentional disconnection.
     * @throws Exception if an error occurs during disconnection, such as network issues or server problems, which
     * should be handled by the caller to provide appropriate feedback or take corrective action.
     */
    override fun disconnect() = ssh.disconnect()
}
