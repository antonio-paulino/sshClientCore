package pt.paulinoo.sshClientCore.exception

/**
 * Base exception class for SSH-related errors.
 *
 * @param message The error message describing the exception.
 * @param cause The underlying cause of the exception, if any.
 */
sealed class SshException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Exception thrown when an SSH connection fails.
 *
 * @param cause The underlying cause of the connection failure.
 */
class ConnectionException(
    cause: Throwable,
) : SshException("SSH connection failed", cause)

/**
 * Exception thrown when SSH authentication fails.
 *
 * @param cause The underlying cause of the authentication failure.
 */
class AuthenticationException(
    cause: Throwable,
) : SshException("SSH authentication failed", cause)

/**
 * Exception thrown when SSH command execution fails.
 *
 * @param cause The underlying cause of the command execution failure.
 */
class CommandExecutionException(
    cause: Throwable,
) : SshException("SSH command execution failed", cause)

/**
 * Exception thrown when an SSH shell session encounters an error.
 *
 * @param cause The underlying cause of the shell error.
 */
class ShellException(
    cause: Throwable,
) : SshException("SSH shell error", cause)

/**
 * Exception thrown when an unknown host key is encountered during SSH connection.
 *
 * @param hostname The hostname of the SSH server.
 * @param fingerprint The fingerprint of the unknown host key.
 * @param algorithm The algorithm of the unknown host key.
 */
class UnknownHostKeyException(
    val hostname: String,
    val fingerprint: String,
    val algorithm: String,
) : SshException("Unknown host key for $hostname. Fingerprint: $fingerprint")

/**
 * Runtime exception used internally to signal an unknown host key during SSH connection.
 * This is not exposed to the public API and is used to map SSHJ's host key verification failure to a custom exception.
 *
 * @param hostname The hostname of the SSH server.
 * @param fingerprint The fingerprint of the unknown host key.
 * @param algorithm The algorithm of the unknown host key.
 */
internal class UnknownHostKeyRuntimeException(
    val hostname: String,
    val fingerprint: String,
    val algorithm: String,
) : RuntimeException("Unknown host key")
