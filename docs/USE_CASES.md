# Use Cases

This guide shows common ways to use `sshClientCore`.

## 1) One-shot command execution

```kotlin
val client = Ssh.createClient()
val session = client.connect(
    SshConfig(
        host = "10.0.0.10",
        username = "ubuntu",
        password = "secret",
        backend = SshBackend.SSHJ,
        hostKeyVerification = HostKeyVerification.Promiscuous,
    ),
)

val result = session.execute("uname -a")
println(result.stdout)
println("exit=${result.exitCode}")
session.disconnect()
```

## 2) Real-time command streaming

```kotlin
session.executeStreaming("tail -f /var/log/syslog").collect { chunk ->
    when (chunk) {
        is CommandChunk.Stdout -> print(chunk.data)
        is CommandChunk.Stderr -> System.err.print(chunk.data)
        is CommandChunk.ExitCode -> println("done: ${chunk.code}")
    }
}
```

## 3) Interactive shell / terminal

```kotlin
val shell = session.openShell()

val job = launch {
    shell.output.collect { bytes -> print(String(bytes)) }
}

shell.send("ls -la\n")
shell.sendKey(TerminalKey.Enter)
shell.resize(120, 40)

shell.close()
job.cancel()
```

## 4) Switch backend without changing your app code

```kotlin
val base = SshConfig(
    host = "server",
    username = "user",
    password = "pass",
)

val sshjSession = SshClient.create().connect(base.copy(backend = SshBackend.SSHJ))
val jschSession = SshClient.create().connect(base.copy(backend = SshBackend.JSCH))
```

## 5) Host key strategies

- `Promiscuous`: easiest for local tests; not recommended in production.
- `KnownHosts(file)`: recommended for OpenSSH-compatible workflows.
- `Fingerprint(expected)` / `Strict(set)`: explicit fingerprint pinning.

## 6) Android-friendly live usage (ViewModel style)

```kotlin
class TerminalViewModel : ViewModel() {
    private var session: SshSession? = null
    private var shell: ShellSession? = null
    private var shellJob: Job? = null

    val output = MutableStateFlow("")

    suspend fun connect(config: SshConfig) {
        session = SshClient.create().connect(config)
        shell = session!!.openShell()
        shellJob = shell!!.consumeOutput(viewModelScope) { bytes ->
            output.value += String(bytes)
        }
    }

    suspend fun sendLine(text: String) {
        shell?.sendLine(text)
    }

    override fun onCleared() {
        viewModelScope.launch {
            shellJob?.cancel()
            shell?.close()
            session?.disconnect()
        }
    }
}
```

## Notes

- Always `disconnect()` sessions and `close()` shell sessions.
- For CLI apps, run collection and send operations in coroutines.
- Prefer `KnownHosts` or strict pinning in production environments.

