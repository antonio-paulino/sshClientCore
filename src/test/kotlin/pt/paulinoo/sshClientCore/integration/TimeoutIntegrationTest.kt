package pt.paulinoo.sshClientCore.integration
import kotlinx.coroutines.runBlocking
import pt.paulinoo.sshClientCore.api.HostKeyVerification
import pt.paulinoo.sshClientCore.api.Ssh
import pt.paulinoo.sshClientCore.api.SshConfig
import pt.paulinoo.sshClientCore.exception.ConnectionException
import kotlin.test.Test
import kotlin.test.assertIs

class TimeoutIntegrationTest {
    @Test
    fun `connection timeout fires for unreachable host`(): Unit =
        runBlocking {
            val config =
                SshConfig(
                    host = "192.0.2.1",
                    port = 22,
                    username = "user",
                    password = "pass",
                    timeoutMillis = 300,
                    hostKeyVerification = HostKeyVerification.Promiscuous,
                )
            val error =
                try {
                    Ssh.createClient().connect(config)
                    null
                } catch (e: Exception) {
                    e
                }
            assertIs<ConnectionException>(error)
        }
}
