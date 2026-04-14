package pt.paulinoo.sshClientCore.internal

import pt.paulinoo.sshClientCore.api.SshConfig

internal interface SshBackendClient {
    fun connect(config: SshConfig)

    fun startExec(command: String): ExecChannel

    fun startShell(
        cols: Int = 80,
        rows: Int = 24,
    ): ShellChannel

    fun onDisconnect(listener: (Throwable?) -> Unit)

    fun disconnect()
}
