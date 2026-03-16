package pt.paulinoo.shell

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import org.slf4j.LoggerFactory
import pt.paulinoo.exception.ShellException
import pt.paulinoo.utils.TerminalKey

class DefaultShellSession(
    ssh: SSHClient,
) : ShellSession {
    private val logger = LoggerFactory.getLogger(DefaultShellSession::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _output =
        MutableSharedFlow<ByteArray>(
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.SUSPEND,
        )
    override val output: Flow<ByteArray> = _output

    private val session: Session = ssh.startSession()
    private val shell: Session.Shell

    init {
        try {
            logger.info("Allocating PTY and starting shell session...")
            session.allocatePTY("xterm-256color", 80, 24, 0, 0, emptyMap())
            shell = session.startShell()
        } catch (e: Exception) {
            logger.error("Failed to initialize shell", e)
            throw ShellException(e)
        }

        scope.launch {
            val buffer = ByteArray(8192)
            shell.inputStream.use { input ->
                while (isActive) {
                    val read = withContext(Dispatchers.IO) { input.read(buffer) }
                    if (read == -1) break
                    _output.emit(buffer.copyOf(read))
                }
            }
        }
    }

    override suspend fun send(data: ByteArray) =
        withContext(Dispatchers.IO) {
            try {
                shell.outputStream.write(data)
                shell.outputStream.flush()
                logger.trace("Sent {} bytes to shell", data.size)
            } catch (e: Exception) {
                logger.error("Failed to send data to shell", e)
                throw ShellException(e)
            }
        }

    override suspend fun send(text: String) = send(text.toByteArray())

    override suspend fun sendKey(key: TerminalKey) {
        send(key.bytes)
    }

    override suspend fun resize(
        cols: Int,
        rows: Int,
    ) = withContext(Dispatchers.IO) {
        try {
            shell.changeWindowDimensions(cols, rows, 0, 0)
            logger.debug("Resized shell to {} x {}", cols, rows)
        } catch (e: Exception) {
            logger.error("Failed to resize shell", e)
            throw ShellException(e)
        }
    }

    override suspend fun close() =
        withContext(Dispatchers.IO) {
            scope.cancel()
            shell.close()
            session.close()
        }
}
