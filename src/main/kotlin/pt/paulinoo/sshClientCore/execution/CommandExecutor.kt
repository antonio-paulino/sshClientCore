package pt.paulinoo.sshClientCore.execution

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import pt.paulinoo.sshClientCore.exception.CommandExecutionException
import pt.paulinoo.sshClientCore.internal.ExecChannel

internal class CommandExecutor {
    private val logger = LoggerFactory.getLogger(CommandExecutor::class.java)

    fun executeStreaming(
        channel: ExecChannel,
        command: String,
    ): Flow<CommandChunk> =
        channelFlow {
            logger.debug("Starting streaming command: {}", command)

            // Stream de Stdout
            launch(Dispatchers.IO) {
                channel.stdout.bufferedReader().use { reader ->
                    while (true) {
                        val line = reader.readLine() ?: break
                        send(CommandChunk.Stdout(line + "\n"))
                    }
                }
            }

            // Stream de Stderr
            launch(Dispatchers.IO) {
                channel.stderr.bufferedReader().use { reader ->
                    while (true) {
                        val line = reader.readLine() ?: break
                        send(CommandChunk.Stderr(line + "\n"))
                    }
                }
            }

            launch(Dispatchers.IO) {
                channel.join()
                send(CommandChunk.ExitCode(channel.exitStatus()))
                channel.close()
            }
        }.flowOn(Dispatchers.IO)

    suspend fun executeBlocking(
        channel: ExecChannel,
        command: String,
    ): CommandResult =
        coroutineScope {
            logger.debug("Starting blocking command: {}", command)

            try {
                val stdoutDeferred =
                    async(Dispatchers.IO) {
                        channel.stdout.bufferedReader().use { it.readText() }
                    }
                val stderrDeferred =
                    async(Dispatchers.IO) {
                        channel.stderr.bufferedReader().use { it.readText() }
                    }
                val joinDeferred =
                    async(Dispatchers.IO) {
                        runCatching { channel.join() }
                    }

                // Some backends (notably JSch) may only close streams once the channel is finished.
                joinDeferred.await().getOrThrow()
                // Force EOF on streams for backends that keep stderr/stdout open until channel close.
                channel.close()
                val stdout = stdoutDeferred.await()
                val stderr = stderrDeferred.await()

                CommandResult(stdout, stderr, channel.exitStatus())
            } catch (e: Exception) {
                logger.error("Failed to execute blocking command: {}", command, e)
                throw CommandExecutionException(e)
            } finally {
                channel.close()
            }
        }
}
