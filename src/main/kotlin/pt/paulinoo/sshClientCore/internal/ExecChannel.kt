package pt.paulinoo.sshClientCore.internal

import java.io.Closeable
import java.io.InputStream

internal interface ExecChannel : Closeable {
    val stdout: InputStream
    val stderr: InputStream

    fun join()

    fun exitStatus(): Int
}
