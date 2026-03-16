package pt.paulinoo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import pt.paulinoo.api.HostKeyVerification
import pt.paulinoo.api.SshClient
import pt.paulinoo.api.SshConfig
import pt.paulinoo.utils.TerminalKey
import java.util.Scanner

fun main() =
    runBlocking {
        val client = SshClient.create()

        val config =
            SshConfig(
                host = "<ip>",
                username = "<username>",
                password = "<password>",
                hostKeyVerification = HostKeyVerification.Promiscuous,
            )

        try {
            val session = client.connect(config)

            val terminal = session.openShell()

            // Regex to remove ANSI escape codes (colors, cursor movements, etc.) for cleaner output
            val ansiEscapeRegex = Regex("\\x1B\\[[0-9;]*[a-zA-Z]|\\x1B\\][0-9];.*?\\x07")

            val outputJob =
                terminal.output
                    .onEach { bytes ->
                        val cleanText = String(bytes).replace(ansiEscapeRegex, "")
                        print(cleanText)
                    }.launchIn(CoroutineScope(Dispatchers.IO))

            val scanner = Scanner(System.`in`)

            try {
                while (isActive && scanner.hasNextLine()) {
                    val input = scanner.nextLine()

                    if (input.trim() == "exit") {
                        println("Closing session...")
                        break
                    }

                    if (input.trim() == "!c") {
                        terminal.sendKey(TerminalKey.CtrlC)
                        continue
                    }

                    terminal.send(input + "\n")
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
