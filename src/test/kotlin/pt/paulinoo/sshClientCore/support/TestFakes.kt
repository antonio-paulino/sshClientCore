package pt.paulinoo.sshClientCore.support

import pt.paulinoo.sshClientCore.api.SshConfig
import pt.paulinoo.sshClientCore.internal.ExecChannel
import pt.paulinoo.sshClientCore.internal.ShellChannel
import pt.paulinoo.sshClientCore.internal.SshBackendClient
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

internal class FakeExecChannel(
    override val stdout: InputStream = ByteArrayInputStream(ByteArray(0)),
    override val stderr: InputStream = ByteArrayInputStream(ByteArray(0)),
    private val exitCode: Int = 0,
    private val joinError: Throwable? = null,
) : ExecChannel {
    var closeCalled: Boolean = false
        private set

    override fun join() {
        joinError?.let { throw it }
    }

    override fun exitStatus(): Int = exitCode

    override fun close() {
        closeCalled = true
    }
}

internal class FakeShellChannel(
    override val input: InputStream = ByteArrayInputStream(ByteArray(0)),
    override val output: OutputStream = ByteArrayOutputStream(),
    private val resizeError: Throwable? = null,
    private val closeError: Throwable? = null,
) : ShellChannel {
    var resizeCalls: Int = 0
        private set
    var lastCols: Int = 0
        private set
    var lastRows: Int = 0
        private set
    var closeCalled: Boolean = false
        private set

    override fun resize(
        cols: Int,
        rows: Int,
    ) {
        resizeCalls++
        lastCols = cols
        lastRows = rows
        resizeError?.let { throw it }
    }

    override fun close() {
        closeCalled = true
        closeError?.let { throw it }
    }
}

internal class FakeBackendClient(
    var execFactory: (String) -> ExecChannel,
    var shellFactory: () -> ShellChannel,
) : SshBackendClient {
    var connectCalled: Boolean = false
        private set
    var disconnectCalled: Boolean = false
        private set
    var lastConnectConfig: SshConfig? = null
        private set
    var lastExecCommand: String? = null
        private set

    private var disconnectListener: ((Throwable?) -> Unit)? = null

    override fun connect(config: SshConfig) {
        connectCalled = true
        lastConnectConfig = config
    }

    override fun startExec(command: String): ExecChannel {
        lastExecCommand = command
        return execFactory(command)
    }

    override fun startShell(
        cols: Int,
        rows: Int,
    ): ShellChannel = shellFactory()

    override fun onDisconnect(listener: (Throwable?) -> Unit) {
        disconnectListener = listener
    }

    override fun disconnect() {
        disconnectCalled = true
    }

    fun emitDisconnect(error: Throwable?) {
        disconnectListener?.invoke(error)
    }
}
