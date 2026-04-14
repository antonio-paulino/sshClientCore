package pt.paulinoo.sshClientCore.utils

import kotlin.test.Test
import kotlin.test.assertContentEquals

class TerminalKeyTest {
    @Test
    fun `ctrl helper maps letters to control bytes`() {
        assertContentEquals(byteArrayOf(0x01), TerminalKey.ctrl('a').bytes)
        assertContentEquals(byteArrayOf(0x03), TerminalKey.ctrl('C').bytes)
        assertContentEquals(byteArrayOf(0x1A), TerminalKey.ctrl('z').bytes)
    }

    @Test
    fun `named keys keep expected byte sequences`() {
        assertContentEquals(byteArrayOf(0x03), TerminalKey.CtrlC.bytes)
        assertContentEquals(byteArrayOf(0x0D, 0x0A), TerminalKey.Enter.bytes)
        assertContentEquals(byteArrayOf(0x1B, 0x5B, 0x41), TerminalKey.ArrowUp.bytes)
    }
}

