# sshClientCore

The core for the kotlin version of `sshClient`.

`sshClientCore` is designed to serve as the foundation for terminal emulators and automation tools. It focuses on **Coroutines** and **Flows**, ensuring your UI never freezes and data streaming remains instantaneous.

---

## ✨ Features

- **Reactive Terminal (PTY)**  
  Full Pseudo-Terminal support with pure ANSI byte streams, dynamic window resizing, and support for control keys (Ctrl+C, Tab, Arrows).

- **Data Streaming with Flows**  
  Execute commands with real-time output via `Flow<CommandChunk>`, natively separating `stdout`, `stderr`, and `ExitCode`.

- **Enterprise Security**  
  Configurable Host Key Verification (`Strict`, `KnownHosts`, `Promiscuous`).

- **Resilience**  
  Integrated Keep-Alive system and precise timeouts managed via Coroutines to prevent hung sockets.

---

## 📦 Installation

Add to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("pt.paulinoo:sshClientCore:0.0.1")

    // Recommended for development logging
    implementation("org.slf4j:slf4j-simple:2.0.12")
}
```

---

## 🛠️ Quick Usage Example

```kotlin
fun main() = runBlocking {
    val client = SshClient.create()

    val config = SshConfig(
        host = "<ip>",
        username = "<username>",
        password = "<password>",
        hostKeyVerification = HostKeyVerification.Promiscuous
    )

    try {
        val session = client.connect(config)
        val terminal = session.openShell()

        // Regex to remove ANSI escape codes
        val ansiEscapeRegex = Regex("\x1B\[[0-9;]*[a-zA-Z]|\x1B\][0-9];.*?\x07")

        val outputJob = terminal.output
            .onEach { bytes ->
                val cleanText = String(bytes).replace(ansiEscapeRegex, "")
                print(cleanText)
            }
            .launchIn(CoroutineScope(Dispatchers.IO))

        val scanner = Scanner(System.`in`)
        try {
            while (isActive && scanner.hasNextLine()) {
                val input = scanner.nextLine()

                when (input.trim()) {
                    "exit" -> {
                        println("Closing session...")
                        break
                    }
                    "!c" -> {
                        terminal.sendKey(TerminalKey.CtrlC)
                        continue
                    }
                    else -> terminal.send(input + "\n")
                }
            }
        } finally {
            outputJob.cancel()
            terminal.close()
            session.disconnect()
        }

    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }
}
```

---

## 🏗️ Architecture

`sshClientCore` - Exposes an API purely based on **suspension functions** and **flows**, making it easy to integrate modern patterns like **MVVM** or **MVI** in Android, Desktop, or Backend environments.

---

## 📄 License

Distributed under the **MIT License**. See [LICENSE](LICENSE) for details.
