package pt.paulinoo.utils


sealed class TerminalKey(val bytes: ByteArray) {
    object CtrlC : TerminalKey(byteArrayOf(0x03))
    object CtrlD : TerminalKey(byteArrayOf(0x04))
    object CtrlZ : TerminalKey(byteArrayOf(0x1A))
    object Enter : TerminalKey(byteArrayOf(0x0D, 0x0A))
    object Escape : TerminalKey(byteArrayOf(0x1B))
    object Backspace : TerminalKey(byteArrayOf(0x7F))
    object Tab : TerminalKey(byteArrayOf(0x09))

    object ArrowUp : TerminalKey(byteArrayOf(0x1B, 0x5B, 0x41))
    object ArrowDown : TerminalKey(byteArrayOf(0x1B, 0x5B, 0x42))
    object ArrowRight : TerminalKey(byteArrayOf(0x1B, 0x5B, 0x43))
    object ArrowLeft : TerminalKey(byteArrayOf(0x1B, 0x5B, 0x44))


    class Custom(bytes: ByteArray) : TerminalKey(bytes)

    companion object {
        fun ctrl(char: Char): TerminalKey {
            val code = char.uppercaseChar().code - 64
            return Custom(byteArrayOf(code.toByte()))
        }
    }
}