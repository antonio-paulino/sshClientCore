package pt.paulinoo.api

import pt.paulinoo.client.DefaultSshClient

object Ssh {
    fun createClient(): SshClient = DefaultSshClient()
}
