package pt.paulinoo.sshClientCore.api

import pt.paulinoo.sshClientCore.client.DefaultSshClient

object Ssh {
    fun createClient(): SshClient = DefaultSshClient()
}
