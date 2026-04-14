package pt.paulinoo.sshClientCore.internal

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.HostKey
import com.jcraft.jsch.HostKeyRepository
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import pt.paulinoo.sshClientCore.api.HostKeyVerification
import pt.paulinoo.sshClientCore.api.SshConfig
import pt.paulinoo.sshClientCore.exception.AuthenticationException
import pt.paulinoo.sshClientCore.exception.UnknownHostKeyRuntimeException
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.Base64

internal class JschClientWrapper : SshBackendClient {
    private val jsch = JSch()
    private var session: Session? = null

    override fun connect(config: SshConfig) {
        val createdSession = jsch.getSession(config.username, config.host, config.port)
        createdSession.timeout = config.timeoutMillis.toInt()

        when (config.hostKeyVerification) {
            is HostKeyVerification.Promiscuous -> {
                createdSession.setConfig("StrictHostKeyChecking", "no")
            }
            is HostKeyVerification.Fingerprint -> {
                createdSession.setConfig("StrictHostKeyChecking", "yes")
                jsch.hostKeyRepository =
                    FingerprintHostKeyRepository(
                        host = config.host,
                        acceptedFingerprints = setOf(config.hostKeyVerification.expected),
                    )
            }
            is HostKeyVerification.KnownHosts -> {
                createdSession.setConfig("StrictHostKeyChecking", "yes")
                jsch.setKnownHosts(config.hostKeyVerification.file.absolutePath)
            }
            is HostKeyVerification.Strict -> {
                createdSession.setConfig("StrictHostKeyChecking", "yes")
                jsch.hostKeyRepository =
                    FingerprintHostKeyRepository(
                        host = config.host,
                        acceptedFingerprints = config.hostKeyVerification.acceptedFingerprints,
                    )
            }
        }

        if (config.password != null) {
            createdSession.setPassword(config.password)
        }

        if (config.privateKey != null) {
            jsch.addIdentity(config.privateKey.absolutePath)
        }

        if (config.keepAliveIntervalSeconds > 0) {
            createdSession.serverAliveInterval = config.keepAliveIntervalSeconds * 1000
        }

        try {
            createdSession.connect(config.timeoutMillis.toInt())
            session = createdSession
        } catch (e: JSchException) {
            if (e.message?.contains("Auth fail", ignoreCase = true) == true) {
                throw AuthenticationException(e)
            }
            throw e
        }
    }

    override fun startExec(command: String): ExecChannel {
        val activeSession = session ?: error("Session is not connected")
        val channel = activeSession.openChannel("exec") as ChannelExec
        channel.setCommand(command)
        channel.connect(activeSession.timeout)

        val stderrInput =
            try {
                channel.extInputStream
            } catch (_: Exception) {
                ByteArrayInputStream(ByteArray(0))
            }

        return object : ExecChannel {
            override val stdout = channel.inputStream
            override val stderr = stderrInput

            override fun join() {
                while (!channel.isClosed) {
                    Thread.sleep(25)
                }
            }

            override fun exitStatus(): Int = channel.exitStatus

            override fun close() {
                channel.disconnect()
            }
        }
    }

    override fun startShell(
        cols: Int,
        rows: Int,
    ): ShellChannel {
        val activeSession = session ?: error("Session is not connected")
        val channel = activeSession.openChannel("shell") as ChannelShell
        channel.setPtyType("xterm-256color", cols, rows, 0, 0)
        channel.connect(activeSession.timeout)

        return object : ShellChannel {
            override val input = channel.inputStream
            override val output = channel.outputStream

            override fun resize(
                cols: Int,
                rows: Int,
            ) {
                channel.setPtySize(cols, rows, 0, 0)
            }

            override fun close() {
                channel.disconnect()
            }
        }
    }

    override fun onDisconnect(listener: (Throwable?) -> Unit) {
        // JSch does not expose a disconnect callback; state updates happen on explicit disconnect.
    }

    override fun disconnect() {
        session?.disconnect()
        session = null
    }

    private class FingerprintHostKeyRepository(
        private val host: String,
        private val acceptedFingerprints: Set<String>,
    ) : HostKeyRepository {
        override fun check(
            host: String,
            key: ByteArray,
        ): Int {
            if (host != this.host) {
                return HostKeyRepository.NOT_INCLUDED
            }

            val sha256 = sha256Fingerprint(key)
            val md5 = md5Fingerprint(key)

            if (acceptedFingerprints.contains(sha256) || acceptedFingerprints.contains(md5)) {
                return HostKeyRepository.OK
            }

            throw UnknownHostKeyRuntimeException(host, sha256, "unknown")
        }

        override fun add(
            hostkey: HostKey,
            ui: com.jcraft.jsch.UserInfo?,
        ) {
        }

        override fun remove(
            host: String,
            type: String,
        ) {
        }

        override fun remove(
            host: String,
            type: String,
            key: ByteArray,
        ) {
        }

        override fun getHostKey(): Array<HostKey> = emptyArray()

        override fun getHostKey(
            host: String,
            type: String,
        ): Array<HostKey> = emptyArray()

        override fun getKnownHostsRepositoryID(): String = "memory"

        private fun sha256Fingerprint(key: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(key)
            return "SHA256:${Base64.getEncoder().withoutPadding().encodeToString(digest)}"
        }

        private fun md5Fingerprint(key: ByteArray): String {
            val digest = MessageDigest.getInstance("MD5").digest(key)
            return digest.joinToString(":") { "%02x".format(it) }
        }
    }
}
