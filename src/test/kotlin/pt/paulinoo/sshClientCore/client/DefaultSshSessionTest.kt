package pt.paulinoo.sshClientCore.client

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import pt.paulinoo.sshClientCore.connection.ConnectionState
import pt.paulinoo.sshClientCore.execution.CommandChunk
import pt.paulinoo.sshClientCore.support.FakeBackendClient
import pt.paulinoo.sshClientCore.support.FakeExecChannel
import pt.paulinoo.sshClientCore.support.FakeShellChannel
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DefaultSshSessionTest {
    @Test
    fun `execute delegates to backend exec channel`() =
        runBlocking {
            val backend =
                FakeBackendClient(
                    execFactory = {
                        FakeExecChannel(
                            stdout = ByteArrayInputStream("stdout".toByteArray()),
                            stderr = ByteArrayInputStream("stderr".toByteArray()),
                            exitCode = 0,
                        )
                    },
                    shellFactory = { FakeShellChannel() },
                )

            val session = DefaultSshSession(backend)
            val result = session.execute("ls")

            assertEquals("ls", backend.lastExecCommand)
            assertEquals("stdout", result.stdout)
            assertEquals("stderr", result.stderr)
            assertEquals(0, result.exitCode)
        }

    @Test
    fun `executeStreaming emits chunks from backend channel`() =
        runBlocking {
            val backend =
                FakeBackendClient(
                    execFactory = {
                        FakeExecChannel(
                            stdout = ByteArrayInputStream("one\n".toByteArray()),
                            stderr = ByteArrayInputStream("two\n".toByteArray()),
                            exitCode = 5,
                        )
                    },
                    shellFactory = { FakeShellChannel() },
                )

            val session = DefaultSshSession(backend)
            val chunks = session.executeStreaming("cmd").toList()

            assertTrue(chunks.contains(CommandChunk.Stdout("one\n")))
            assertTrue(chunks.contains(CommandChunk.Stderr("two\n")))
            assertTrue(chunks.contains(CommandChunk.ExitCode(5)))
        }

    @Test
    fun `disconnect callback updates state to error`() =
        runBlocking {
            val backend =
                FakeBackendClient(
                    execFactory = { FakeExecChannel() },
                    shellFactory = { FakeShellChannel() },
                )
            val session = DefaultSshSession(backend)

            backend.emitDisconnect(IllegalStateException("lost"))
            val state = session.connectionState.value

            assertIs<ConnectionState.Error>(state)
            assertEquals("lost", state.throwable.message)
        }

    @Test
    fun `disconnect marks disconnected and calls backend`() =
        runBlocking {
            val backend =
                FakeBackendClient(
                    execFactory = { FakeExecChannel() },
                    shellFactory = { FakeShellChannel() },
                )
            val session = DefaultSshSession(backend)

            session.disconnect()

            assertTrue(backend.disconnectCalled)
            assertIs<ConnectionState.Disconnected>(session.connectionState.value)
        }

    @Test
    fun `openShell returns a usable shell session`() =
        runBlocking {
            val backend =
                FakeBackendClient(
                    execFactory = { FakeExecChannel() },
                    shellFactory = { FakeShellChannel(input = ByteArrayInputStream(ByteArray(0))) },
                )
            val session = DefaultSshSession(backend)

            val shell = session.openShell()
            withTimeout(1_000) {
                shell.send("ping")
                shell.close()
            }
            val state = session.connectionState.first()

            assertIs<ConnectionState.Connected>(state)
        }
}

