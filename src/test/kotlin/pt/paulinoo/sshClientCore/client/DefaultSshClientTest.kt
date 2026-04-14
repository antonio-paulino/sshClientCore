package pt.paulinoo.sshClientCore.client

import kotlinx.coroutines.runBlocking
import pt.paulinoo.sshClientCore.api.SshBackend
import pt.paulinoo.sshClientCore.api.SshConfig
import pt.paulinoo.sshClientCore.exception.AuthenticationException
import pt.paulinoo.sshClientCore.exception.ConnectionException
import pt.paulinoo.sshClientCore.exception.UnknownHostKeyException
import pt.paulinoo.sshClientCore.exception.UnknownHostKeyRuntimeException
import pt.paulinoo.sshClientCore.internal.ExecChannel
import pt.paulinoo.sshClientCore.internal.ShellChannel
import pt.paulinoo.sshClientCore.internal.SshBackendClient
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DefaultSshClientTest {
    @Test
    fun `connect uses selected backend`(): Unit =
        runBlocking {
            var selectedBackend: SshBackend? = null
            val backend = noOpBackend()
            val client =
                DefaultSshClient(
                    backendFactory = {
                        selectedBackend = it
                        backend
                    },
                )

            val session =
                client.connect(
                    baseConfig().copy(backend = SshBackend.JSCH),
                )

            assertEquals(SshBackend.JSCH, selectedBackend)
            assertNotNull(session)
        }

    @Test
    fun `connect maps unknown host runtime exception`() =
        runBlocking {
            val client =
                DefaultSshClient(
                    backendFactory = {
                        failingBackend {
                            throw UnknownHostKeyRuntimeException("host", "fp", "rsa")
                        }
                    },
                )

            val error = assertFailsWith<UnknownHostKeyException> { client.connect(baseConfig()) }
            assertEquals("host", error.hostname)
            assertEquals("fp", error.fingerprint)
            assertEquals("rsa", error.algorithm)
        }

    @Test
    fun `connect keeps authentication exception type`() =
        runBlocking {
            val client =
                DefaultSshClient(
                    backendFactory = {
                        failingBackend {
                            throw AuthenticationException(IllegalArgumentException("bad credentials"))
                        }
                    },
                )

            val error = assertFailsWith<AuthenticationException> { client.connect(baseConfig()) }
            assertTrue(error.cause != null)
        }

    @Test
    fun `connect maps generic errors to connection exception`() =
        runBlocking {
            val client =
                DefaultSshClient(
                    backendFactory = {
                        failingBackend {
                            throw IllegalStateException("boom")
                        }
                    },
                )

            val error = assertFailsWith<ConnectionException> { client.connect(baseConfig()) }
            assertTrue(error.cause != null)
        }

    private fun baseConfig(): SshConfig =
        SshConfig(
            host = "127.0.0.1",
            username = "user",
            password = "pass",
            timeoutMillis = 250,
        )

    private fun noOpBackend(): SshBackendClient =
        object : SshBackendClient {
            override fun connect(config: SshConfig) {
            }

            override fun startExec(command: String): ExecChannel =
                object : ExecChannel {
                    override val stdout = ByteArrayInputStream(ByteArray(0))
                    override val stderr = ByteArrayInputStream(ByteArray(0))

                    override fun join() {
                    }

                    override fun exitStatus(): Int = 0

                    override fun close() {
                    }
                }

            override fun startShell(
                cols: Int,
                rows: Int,
            ): ShellChannel =
                object : ShellChannel {
                    override val input = ByteArrayInputStream(ByteArray(0))
                    override val output = ByteArrayOutputStream()

                    override fun resize(
                        cols: Int,
                        rows: Int,
                    ) {
                    }

                    override fun close() {
                    }
                }

            override fun onDisconnect(listener: (Throwable?) -> Unit) {
            }

            override fun disconnect() {
            }
        }

    private fun failingBackend(onConnect: () -> Unit): SshBackendClient =
        object : SshBackendClient by noOpBackend() {
            override fun connect(config: SshConfig) {
                onConnect()
            }
        }
}

