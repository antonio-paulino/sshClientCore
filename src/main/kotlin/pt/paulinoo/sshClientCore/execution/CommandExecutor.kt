package pt.paulinoo.sshClientCore.execution

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import net.schmizz.sshj.connection.channel.direct.Session
import org.slf4j.LoggerFactory
import pt.paulinoo.sshClientCore.exception.CommandExecutionException

internal class CommandExecutor {
    private val logger = LoggerFactory.getLogger(CommandExecutor::class.java)

    fun executeStreaming(
        session: Session,
        command: String,
    ): Flow<CommandChunk> =
        channelFlow {
            logger.debug("Starting streaming command: {}", command)

            val cmd =
                try {
                    session.exec(command)
                } catch (e: Exception) {
                    logger.error("Failed to start streaming command: {}", command, e)
                    throw CommandExecutionException(e)
                }

            // Stream de Stdout
            launch(Dispatchers.IO) {
                cmd.inputStream.bufferedReader().use { reader ->
                    while (true) {
                        val line = reader.readLine() ?: break
                        send(CommandChunk.Stdout(line + "\n"))
                    }
                }
            }

            // Stream de Stderr
            launch(Dispatchers.IO) {
                cmd.errorStream.bufferedReader().use { reader ->
                    while (true) {
                        val line = reader.readLine() ?: break
                        send(CommandChunk.Stderr(line + "\n"))
                    }
                }
            }

            launch(Dispatchers.IO) {
                cmd.join()
                send(CommandChunk.ExitCode(cmd.exitStatus ?: -1))
            }
        }.flowOn(Dispatchers.IO)

    suspend fun executeBlocking(
        session: Session,
        command: String,
    ): CommandResult =
        coroutineScope {
            logger.debug("Starting blocking command: {}", command)

            val cmd =
                try {
                    session.exec(command)
                } catch (e: Exception) {
                    logger.error("Failed to execute blocking command: {}", command, e)
                    throw CommandExecutionException(e)
                }

            val stdoutDeferred =
                async(Dispatchers.IO) {
                    cmd.inputStream.bufferedReader().use { it.readText() }
                }
            val stderrDeferred =
                async(Dispatchers.IO) {
                    cmd.errorStream.bufferedReader().use { it.readText() }
                }

            val stdout = stdoutDeferred.await()
            val stderr = stderrDeferred.await()
            cmd.join()

            CommandResult(stdout, stderr, cmd.exitStatus ?: -1)
        }
}
