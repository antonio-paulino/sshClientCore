package pt.paulinoo.sshClientCore.internal

import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream

internal interface ShellChannel : Closeable {
    val input: InputStream
    val output: OutputStream

    fun resize(
        cols: Int,
        rows: Int,
    )
}

