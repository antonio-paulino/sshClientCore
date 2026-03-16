package pt.paulinoo.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import net.schmizz.sshj.userauth.UserAuthException
import pt.paulinoo.api.SshClient
import pt.paulinoo.api.SshConfig
import pt.paulinoo.api.SshSession
import pt.paulinoo.exception.AuthenticationException
import pt.paulinoo.exception.ConnectionException
import pt.paulinoo.exception.UnknownHostKeyException
import pt.paulinoo.exception.UnknownHostKeyRuntimeException
import pt.paulinoo.internal.SshjClientWrapper

/**
 * Default implementation of [SshClient] using SSHJ library.
 * Handles connection, authentication, and error mapping to custom exceptions.
 * All operations are performed on the IO dispatcher to avoid blocking the main thread.
 */
internal class DefaultSshClient : SshClient {
    override suspend fun connect(config: SshConfig): SshSession =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(config.timeoutMillis) {
                    val wrapper = SshjClientWrapper()

                    wrapper.connect(
                        host = config.host,
                        port = config.port,
                        keepAliveSeconds = config.keepAliveIntervalSeconds,
                        timeoutMillis = config.timeoutMillis,
                        verification = config.hostKeyVerification,
                    )

                    when {
                        config.password != null -> wrapper.authPassword(config.username, config.password)
                        config.privateKey != null -> wrapper.authKey(config.username, config.privateKey)
                        else -> error("No auth method")
                    }

                    return@withTimeout DefaultSshSession(wrapper)
                }
            } catch (e: UnknownHostKeyRuntimeException) {
                throw UnknownHostKeyException(e.hostname, e.fingerprint, e.algorithm)
            } catch (e: TimeoutCancellationException) {
                throw ConnectionException(Exception("Connection timed out after ${config.timeoutMillis}ms"))
            } catch (e: UserAuthException) {
                throw AuthenticationException(e)
            } catch (e: Exception) {
                throw ConnectionException(e)
            }
        }
}
