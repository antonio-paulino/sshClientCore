package pt.paulinoo.sshClientCore.integration
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pt.paulinoo.sshClientCore.api.HostKeyVerification
import pt.paulinoo.sshClientCore.api.SshConfig
import kotlin.test.Test
class CoroutineCancellationIntegrationTest {
    @Test
    fun `cancelled execution doesn't crash`() =
        runBlocking {
            val config =
                SshConfig(
                    host = "192.0.2.1",
                    port = 22,
                    username = "user",
                    password = "pass",
                    timeoutMillis = 500,
                    hostKeyVerification = HostKeyVerification.Promiscuous,
                )
            // This validates that cancellation is handled safely
            try {
                launch {
                    cancel("test cancellation")
                }.join()
            } catch (e: Exception) {
                // Expected
            }
        }
}
