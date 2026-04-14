package pt.paulinoo.sshClientCore.integration
import kotlinx.coroutines.runBlocking
import pt.paulinoo.sshClientCore.api.HostKeyVerification
import pt.paulinoo.sshClientCore.api.Ssh
import pt.paulinoo.sshClientCore.api.SshBackend
import pt.paulinoo.sshClientCore.api.SshConfig
import pt.paulinoo.sshClientCore.exception.ConnectionException
import kotlin.test.Test
import kotlin.test.assertIs

class BackendParityIntegrationTest {
    @Test
    fun `both backends handle unreachable host with same error type`(): Unit =
        runBlocking {
            val config =
                SshConfig(
                    host = "192.0.2.1",
                    port = 22,
                    username = "testuser",
                    password = "testpass",
                    timeoutMillis = 500,
                    hostKeyVerification = HostKeyVerification.Promiscuous,
                )
            var sshjError: Exception? = null
            var jschError: Exception? = null
            try {
                Ssh.createClient().connect(config.copy(backend = SshBackend.SSHJ))
            } catch (e: Exception) {
                sshjError = e
            }
            try {
                Ssh.createClient().connect(config.copy(backend = SshBackend.JSCH))
            } catch (e: Exception) {
                jschError = e
            }
            // Both should produce connection errors
            assertIs<ConnectionException>(sshjError)
            assertIs<ConnectionException>(jschError)
        }
}
