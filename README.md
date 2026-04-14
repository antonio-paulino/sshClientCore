# sshClientCore

[![](https://jitpack.io/v/antonio-paulino/sshClientCore.svg)](https://jitpack.io/#antonio-paulino/sshClientCore)

The reactive SSH engine for the Kotlin ecosystem.

`sshClientCore` is a lightweight, idiomatic library designed to serve as the foundation for terminal emulators and automation tools. Unlike traditional libraries, it focuses on **Coroutines** and **Flows**, ensuring your UI never freezes and data streaming remains instantaneous.

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

- **Dual SSH Engines**  
  Choose between **SSHJ** and **mwiede/jsch** with the same public API.

---

## 📦 Installation

Add to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("pt.paulinoo:ssh-client-core:0.0.2")

    // Recommended for development logging
    implementation("org.slf4j:slf4j-simple:2.0.17")
}
```

### Bundled library jar

If you want a single distributable jar that already contains this library runtime dependencies
(`sshj`, `mwiede/jsch`, `kotlinx-coroutines`, `slf4j-api`, and their transitives), build the `uber` artifact:

```powershell
.\gradlew.bat shadowJar
```

This produces an additional library jar with classifier `uber` under `build/libs/`.
By default, the file name will be `sshClientCore-<version>-uber.jar`.
It is **not** an executable jar; it is intended for distribution as a self-contained library artifact.
If you publish it, it is exposed as a separate Maven artifact named `sshClientCore-uber`.

### Example Code And Dependency Scope

This repo separates **library code** from **example/terminal code**:

- Library source: `src/main/kotlin`
- Example source: `src/examples/kotlin`

Dependency scope is intentional:

- `api(...)` / `implementation(...)` in `main` are part of the library artifact behavior.
- `examplesImplementation(...)` is only for local examples and does **not** belong to consumer API.
- `testImplementation(...)` / `testRuntimeOnly(...)` are test-only.

Useful tasks:

```powershell
# Run the interactive terminal example (desktop)
.\gradlew.bat runTerminal

# Build library uber jar (library + runtime dependencies)
.\gradlew.bat shadowJar
```

Notes:

- `runTerminal` uses the `examples` source set.
- `shadowJar` packages the library artifact from `main`; examples are not intended as public API surface.

---

## 🛠️ Quick Usage Example

```kotlin
fun main() = runBlocking {
    val client = SshClient.create()

    val config = SshConfig(
        host = "<ip>",
        username = "<username>",
        password = "<password>",
        hostKeyVerification = HostKeyVerification.Promiscuous,
        backend = SshBackend.SSHJ // or SshBackend.JSCH
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

`sshClientCore` abstracts the complexity of **SSHJ**, **mwiede/jsch**, and Java's blocking I/O.  
It exposes an API purely based on **suspension functions** and **flows**, making it easy to integrate modern patterns like **MVVM** or **MVI** in Android, Desktop, or Backend environments.

---

## 📚 Documentation

- API reference: [`docs/API.md`](docs/API.md)
- Practical use cases: [`docs/USE_CASES.md`](docs/USE_CASES.md)
- Android integration notes: [`docs/ANDROID.md`](docs/ANDROID.md)
- Design decisions (ADR-style): [`docs/DECISIONS.md`](docs/DECISIONS.md)
- Testing guide: [`docs/TESTING.md`](docs/TESTING.md)

---

## 📄 License

Distributed under the **MIT License**. See [LICENSE](LICENSE) for details.
