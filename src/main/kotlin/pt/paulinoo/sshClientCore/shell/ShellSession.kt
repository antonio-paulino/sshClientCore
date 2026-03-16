package pt.paulinoo.sshClientCore.shell

import kotlinx.coroutines.flow.Flow
import pt.paulinoo.sshClientCore.utils.TerminalKey

interface ShellSession {
    val output: Flow<ByteArray>

    suspend fun send(data: ByteArray)

    suspend fun send(text: String)

    suspend fun sendKey(key: TerminalKey)

    suspend fun resize(
        cols: Int,
        rows: Int,
    )

    suspend fun close()
}
