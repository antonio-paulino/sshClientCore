package pt.paulinoo.sshClientCore.execution

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import pt.paulinoo.sshClientCore.exception.CommandExecutionException
import pt.paulinoo.sshClientCore.support.FakeExecChannel
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CommandExecutorTest {
    private val executor = CommandExecutor()

    @Test
    fun `executeBlocking returns stdout stderr and exit code`() =
        runBlocking {
            val channel =
                FakeExecChannel(
                    stdout = ByteArrayInputStream("ok".toByteArray()),
                    stderr = ByteArrayInputStream("warn".toByteArray()),
                    exitCode = 7,
                )

            val result = executor.executeBlocking(channel, "echo")

            assertEquals("ok", result.stdout)
            assertEquals("warn", result.stderr)
            assertEquals(7, result.exitCode)
            assertTrue(channel.closeCalled)
        }

    @Test
    fun `executeBlocking wraps failures and still closes channel`() =
        runBlocking {
            val channel =
                FakeExecChannel(
                    stdout = ByteArrayInputStream("".toByteArray()),
                    stderr = ByteArrayInputStream("".toByteArray()),
                    joinError = IllegalStateException("boom"),
                )

            assertFailsWith<CommandExecutionException> {
                executor.executeBlocking(channel, "echo")
            }
            assertTrue(channel.closeCalled)
        }

    @Test
    fun `executeStreaming emits stdout stderr and exit code`() =
        runBlocking {
            val channel =
                FakeExecChannel(
                    stdout = ByteArrayInputStream("line-1\nline-2\n".toByteArray()),
                    stderr = ByteArrayInputStream("err-1\n".toByteArray()),
                    exitCode = 3,
                )

            val chunks = executor.executeStreaming(channel, "echo").toList()

            assertTrue(chunks.contains(CommandChunk.Stdout("line-1\n")))
            assertTrue(chunks.contains(CommandChunk.Stdout("line-2\n")))
            assertTrue(chunks.contains(CommandChunk.Stderr("err-1\n")))
            assertTrue(chunks.contains(CommandChunk.ExitCode(3)))
            assertTrue(channel.closeCalled)
        }
}
