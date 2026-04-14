package pt.paulinoo.sshClientCore.shell

import kotlinx.coroutines.runBlocking
import pt.paulinoo.sshClientCore.exception.ShellException
import pt.paulinoo.sshClientCore.support.FakeShellChannel
import pt.paulinoo.sshClientCore.utils.TerminalKey
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DefaultShellSessionTest {
    @Test
    fun `send writes bytes and terminal key to output`() =
        runBlocking {
            val output = ByteArrayOutputStream()
            val shellChannel = FakeShellChannel(output = output)
            val shellSession = DefaultShellSession(shellChannel)

            shellSession.send("abc")
            shellSession.sendKey(TerminalKey.CtrlC)
            shellSession.close()

            assertContentEquals(
                "abc".toByteArray() + TerminalKey.CtrlC.bytes,
                output.toByteArray(),
            )
        }

    @Test
    fun `resize delegates to channel`() =
        runBlocking {
            val shellChannel = FakeShellChannel()
            val shellSession = DefaultShellSession(shellChannel)

            shellSession.resize(120, 40)
            shellSession.close()

            assertEquals(1, shellChannel.resizeCalls)
            assertEquals(120, shellChannel.lastCols)
            assertEquals(40, shellChannel.lastRows)
        }

    @Test
    fun `send wraps write failures as shell exception`() =
        runBlocking {
            val throwingOutput =
                object : OutputStream() {
                    override fun write(b: Int) {
                        throw IllegalStateException("write failed")
                    }
                }
            val shellChannel =
                FakeShellChannel(
                    input = ByteArrayInputStream(ByteArray(0)),
                    output = throwingOutput,
                )
            val shellSession = DefaultShellSession(shellChannel)

            val error = assertFailsWith<ShellException> { shellSession.send("x") }
            shellSession.close()

            assertTrue(error.cause != null)
        }

    @Test
    fun `resize wraps failures as shell exception`() =
        runBlocking {
            val shellChannel = FakeShellChannel(resizeError = IllegalArgumentException("bad size"))
            val shellSession = DefaultShellSession(shellChannel)

            val error = assertFailsWith<ShellException> { shellSession.resize(0, 0) }
            shellSession.close()

            assertTrue(error.cause != null)
        }
}

