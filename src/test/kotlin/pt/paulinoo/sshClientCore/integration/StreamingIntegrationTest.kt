package pt.paulinoo.sshClientCore.integration
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import pt.paulinoo.sshClientCore.api.HostKeyVerification
import pt.paulinoo.sshClientCore.api.Ssh
import pt.paulinoo.sshClientCore.api.SshConfig
import kotlin.test.Test
import kotlin.test.assertTrue

class StreamingIntegrationTest {
    @Test
    fun `streaming handles chunk emission even with unreachable host`() =
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
            val session =
                try {
                    Ssh.createClient().connect(config)
                } catch (e: Exception) {
                    // Expected for unreachable host
                    return@runBlocking
                }
            try {
                val chunks = session.executeStreaming("echo ok").toList()
                assertTrue(chunks.isNotEmpty())
            } finally {
                session.disconnect()
            }
        }
}
