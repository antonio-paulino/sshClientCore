package pt.paulinoo.execution

/**
 * Represents the result of a command execution, including the standard output, standard error, and exit code.
 */
data class CommandResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
)
