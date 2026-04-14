package pt.paulinoo.sshClientCore.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import pt.paulinoo.sshClientCore.api.SshBackend
import pt.paulinoo.sshClientCore.api.SshClient
import pt.paulinoo.sshClientCore.api.SshConfig
import pt.paulinoo.sshClientCore.api.SshSession
import pt.paulinoo.sshClientCore.exception.AuthenticationException
import pt.paulinoo.sshClientCore.exception.ConnectionException
import pt.paulinoo.sshClientCore.exception.UnknownHostKeyException
import pt.paulinoo.sshClientCore.exception.UnknownHostKeyRuntimeException
import pt.paulinoo.sshClientCore.internal.JschClientWrapper
import pt.paulinoo.sshClientCore.internal.SshBackendClient
import pt.paulinoo.sshClientCore.internal.SshjClientWrapper

/**
 * Default implementation of [SshClient] using SSHJ library.
 * Handles connection, authentication, and error mapping to custom exceptions.
 * All operations are performed on the IO dispatcher to avoid blocking the main thread.
 */
internal class DefaultSshClient(
    private val backendFactory: (SshBackend) -> SshBackendClient = ::defaultBackendFactory,
) : SshClient {
    override suspend fun connect(config: SshConfig): SshSession =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(config.timeoutMillis) {
                    val wrapper = backendFactory(config.backend)
                    wrapper.connect(config)

                    return@withTimeout DefaultSshSession(wrapper)
                }
            } catch (e: UnknownHostKeyRuntimeException) {
                throw UnknownHostKeyException(e.hostname, e.fingerprint, e.algorithm)
            } catch (e: TimeoutCancellationException) {
                throw ConnectionException(Exception("Connection timed out after ${config.timeoutMillis}ms"))
            } catch (e: AuthenticationException) {
                throw e
            } catch (e: Exception) {
                throw ConnectionException(e)
            }
        }

    private companion object {
        fun defaultBackendFactory(backend: SshBackend): SshBackendClient =
            when (backend) {
                SshBackend.SSHJ -> SshjClientWrapper()
                SshBackend.JSCH -> JschClientWrapper()
            }
    }
}
