package pt.paulinoo.sshClientCore

import com.jcraft.jsch.JSch
import com.jcraft.jsch.UserInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jline.terminal.TerminalBuilder
import pt.paulinoo.sshClientCore.api.HostKeyVerification
import pt.paulinoo.sshClientCore.api.Ssh
import pt.paulinoo.sshClientCore.api.SshBackend
import pt.paulinoo.sshClientCore.api.SshConfig
import pt.paulinoo.sshClientCore.api.SshSession
import pt.paulinoo.sshClientCore.execution.CommandChunk
import pt.paulinoo.sshClientCore.exception.UnknownHostKeyException
import java.io.Console
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

fun main() =
    runBlocking {
        val host = System.getenv("SSH_HOST") ?: ""
        val user = System.getenv("SSH_USER") ?: ""
        val password = System.getenv("SSH_PASS")
        val privateKeyPath = System.getenv("SSH_KEY")
        val port = System.getenv("SSH_PORT")?.toIntOrNull() ?: 22
        val backend = System.getenv("SSH_BACKEND")?.uppercase()?.let { runCatching { SshBackend.valueOf(it) }.getOrNull() } ?: SshBackend.JSCH
        val trustOnFirstUse = (System.getenv("SSH_TOFU") ?: "true").toBoolean()
        val knownHostsFile = ensureKnownHostsFile()

        if (host.isBlank() || user.isBlank() || (password.isNullOrBlank() && privateKeyPath.isNullOrBlank())) {
            println("sshClientCore Terminal Example")
            println("Set env vars: SSH_HOST, SSH_USER and SSH_PASS or SSH_KEY")
            return@runBlocking
        }

        val config =
            SshConfig(
                host = host,
                port = port,
                username = user,
                password = password,
                privateKey = privateKeyPath?.let { File(it) },
                backend = backend,
                hostKeyVerification = HostKeyVerification.KnownHosts(knownHostsFile),
                timeoutMillis = 30000,
            )

        val client = Ssh.createClient()
        var session: SshSession? = null

        try {
            println("Connecting to $host:$port with backend=$backend...")
            session = connectWithKnownHostsHandshake(client, config, trustOnFirstUse)
            println("Connected. Type 'help' for commands.")
            interactiveTerminal(session, config, trustOnFirstUse, client)
        } finally {
            session?.disconnect()
        }
    }

private suspend fun interactiveTerminal(
    initialSession: SshSession,
    baseConfig: SshConfig,
    trustOnFirstUse: Boolean,
    client: pt.paulinoo.sshClientCore.api.SshClient,
) {
    var session = initialSession
    var config = baseConfig

    while (true) {
        print("sshClientCore> ")
        val input = readlnOrNull()?.trim().orEmpty()
        if (input.isEmpty()) continue

        val parts = input.split(" ", limit = 2)
        val command = parts[0]
        val args = parts.getOrNull(1).orEmpty()

        when (command) {
            "exec" -> {
                if (args.isBlank()) {
                    println("Usage: exec <command>")
                    continue
                }
                val effectiveCommand =
                    if (config.backend == SshBackend.JSCH) {
                        val escaped = args.replace("'", "'\"'\"'")
                        "sh -lc '$escaped'"
                    } else {
                        args
                    }

                var exitCode = -1
                val completed = kotlinx.coroutines.withTimeoutOrNull(30000.milliseconds) {
                    session.executeStreaming(effectiveCommand).collect { chunk ->
                        when (chunk) {
                            is CommandChunk.Stdout -> print(stripAnsi(chunk.data))
                            is CommandChunk.Stderr -> System.err.print(stripAnsi(chunk.data))
                            is CommandChunk.ExitCode -> exitCode = chunk.code
                        }
                    }
                    true
                }

                if (completed == null) {
                    println("\nCommand timed out after 30s")
                } else {
                    println("\nExit code: $exitCode")
                }
            }

            "shell" -> {
                val shell = session.openShell()
                try {
                    println("Line mode shell. Type 'exit' to return.")
                    shellInteractiveLineMode(shell)
                } finally {
                    shell.close()
                }
            }

            "shellraw" -> {
                val shell = session.openShell()
                try {
                    println("Raw mode shell. Type 'exit' or Ctrl+] to return.")
                    shellInteractiveRawMode(shell)
                } finally {
                    shell.close()
                }
            }

            "backend" -> {
                val newBackend = runCatching { SshBackend.valueOf(args.uppercase()) }.getOrNull()
                if (newBackend == null) {
                    println("Usage: backend <SSHJ|JSCH>")
                    continue
                }
                session.disconnect()
                config = config.copy(backend = newBackend)
                session = connectWithKnownHostsHandshake(client, config, trustOnFirstUse)
                println("Switched to $newBackend")
            }

            "status" -> println("${config.host}:${config.port} user=${config.username} backend=${config.backend}")
            "help" -> println("Commands: exec, shell, shellraw, backend, status, exit")
            "exit" -> return
            else -> println("Unknown command")
        }
    }
}

private suspend fun shellInteractiveRawMode(shell: pt.paulinoo.sshClientCore.shell.ShellSession) {
    if (System.console() == null) {
        println("Raw mode unavailable in this console. Use shell instead.")
        shellInteractiveLineMode(shell)
        return
    }

    kotlinx.coroutines.coroutineScope {
        val terminal = TerminalBuilder.builder().system(true).build()
        val previousAttributes = terminal.enterRawMode()
        val finished = CompletableDeferred<Unit>()
        val typedBuffer = StringBuilder()

        val inputJob = launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                shell.output.collect { bytes ->
                    System.out.write(bytes)
                    System.out.flush()
                }
            } finally {
                if (!finished.isCompleted) finished.complete(Unit)
            }
        }

        val keyJob = launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val input = terminal.input()
                while (true) {
                    val b = input.read()
                    if (b == -1 || b == 29) break
                    shell.send(byteArrayOf(b.toByte()))

                    when (b) {
                        10, 13 -> {
                            if (typedBuffer.toString().trim() == "exit") break
                            typedBuffer.setLength(0)
                        }
                        8, 127 -> if (typedBuffer.isNotEmpty()) typedBuffer.setLength(typedBuffer.length - 1)
                        else -> if (b in 32..126 && typedBuffer.length < 64) typedBuffer.append(b.toChar())
                    }
                }
            } finally {
                if (!finished.isCompleted) finished.complete(Unit)
            }
        }

        try {
            finished.await()
        } finally {
            keyJob.cancel()
            inputJob.cancel()
            terminal.setAttributes(previousAttributes)
            runCatching { terminal.close() }
        }
    }
}

private suspend fun shellInteractiveLineMode(shell: pt.paulinoo.sshClientCore.shell.ShellSession) {
    kotlinx.coroutines.coroutineScope {
        val inputJob = launch(kotlinx.coroutines.Dispatchers.IO) {
            shell.output.collect { bytes -> print(stripAnsi(String(bytes))) }
        }
        while (true) {
            val input = readlnOrNull() ?: break
            if (input.trim() == "exit") break
            shell.send(input + "\n")
        }
        inputJob.cancel()
    }
}

private suspend fun connectWithKnownHostsHandshake(
    client: pt.paulinoo.sshClientCore.api.SshClient,
    config: SshConfig,
    trustOnFirstUse: Boolean,
): SshSession =
    try {
        client.connect(config)
    } catch (e: UnknownHostKeyException) {
        if (!trustOnFirstUse) throw e
        println("First connection to ${config.host}:${config.port}. Fingerprint: ${e.fingerprint}")
        if (!confirmHostTrust()) throw IllegalStateException("Host key rejected")
        val knownHosts = (config.hostKeyVerification as? HostKeyVerification.KnownHosts)?.file
            ?: throw IllegalStateException("KnownHosts required")
        bootstrapKnownHostWithJsch(config, knownHosts)
        client.connect(config)
    }

private fun ensureKnownHostsFile(): File {
    val sshDir = File(System.getProperty("user.home"), ".ssh")
    if (!sshDir.exists()) sshDir.mkdirs()
    val knownHosts = File(sshDir, "known_hosts")
    if (!knownHosts.exists()) knownHosts.createNewFile()
    return knownHosts
}

private fun confirmHostTrust(): Boolean {
    val console: Console? = System.console()
    val answer =
        if (console != null) {
            console.readLine("Trust and save host key? (yes/no): ")
        } else {
            print("Trust and save host key? (yes/no): ")
            readlnOrNull()
        }
    return answer.equals("yes", true) || answer.equals("y", true)
}

private fun bootstrapKnownHostWithJsch(config: SshConfig, knownHostsFile: File) {
    val jsch = JSch()
    jsch.setKnownHosts(knownHostsFile.absolutePath)
    config.privateKey?.let { jsch.addIdentity(it.absolutePath) }

    val session = jsch.getSession(config.username, config.host, config.port)
    session.setConfig("StrictHostKeyChecking", "ask")
    session.timeout = config.timeoutMillis.toInt()
    config.password?.let { session.setPassword(it) }
    session.userInfo =
        object : UserInfo {
            override fun getPassphrase(): String? = null
            override fun getPassword(): String? = config.password
            override fun promptPassword(message: String?): Boolean = config.password != null
            override fun promptPassphrase(message: String?): Boolean = false
            override fun promptYesNo(message: String?): Boolean = true
            override fun showMessage(message: String?) { if (!message.isNullOrBlank()) println(message) }
        }

    try {
        session.connect(config.timeoutMillis.toInt())
    } finally {
        session.disconnect()
    }
}

private fun stripAnsi(text: String): String {
    val csiRegex = Regex("\\u001B\\[[0-9;?]*[ -/]*[@-~]")
    val oscRegex = Regex("\\u001B\\].*?(\\u0007|\\u001B\\\\)")
    return text.replace(csiRegex, "").replace(oscRegex, "")
}

