package pt.paulinoo.sshClientCore.api

import java.io.File

/**
 * Represents the SSH host key verification strategy.
 * This is used to determine how the SSH client should verify the server's host key during connection.
 * - [Promiscuous]: Accepts all host keys without verification (not recommended for production).
 * - [Fingerprint]: Verifies the host key against a specific expected fingerprint.
 * - [KnownHosts]: Verifies the host key against entries in a known_hosts file.
 * - [Strict]: Verifies the host key against a set of accepted fingerprints, rejecting unknown keys.
 */
sealed class HostKeyVerification {
    object Promiscuous : HostKeyVerification()

    data class Fingerprint(
        val expected: String,
    ) : HostKeyVerification()

    data class KnownHosts(
        val file: File,
    ) : HostKeyVerification()

    data class Strict(
        val acceptedFingerprints: Set<String>,
    ) : HostKeyVerification()
}

/**
 * Data class representing the configuration for an SSH connection.
 * Includes connection details, authentication methods, timeouts, and host key verification strategy.
 * @param host The SSH server hostname or IP address.
 * @param port The SSH server port (default is 22).
 * @param username The username for SSH authentication.
 * @param password The password for SSH authentication (optional if using private key).
 * @param privateKey The private key file for SSH authentication (optional if using password).
 * @param timeoutMillis The connection timeout in milliseconds (default is 10000ms).
 * @param keepAliveIntervalSeconds The interval in seconds for sending keep-alive messages (default is 15s).
 * @param hostKeyVerification The strategy for verifying the SSH server's host key (default is Promiscuous).
 */
data class SshConfig(
    val host: String,
    val port: Int = 22,
    val username: String,
    val password: String? = null,
    val privateKey: File? = null,
    val timeoutMillis: Long = 10000,
    val keepAliveIntervalSeconds: Int = 15,
    val hostKeyVerification: HostKeyVerification = HostKeyVerification.Promiscuous,
    val backend: SshBackend = SshBackend.SSHJ,
)

enum class SshBackend {
    SSHJ,
    JSCH,
}
