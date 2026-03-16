package pt.paulinoo.sshClientCore.execution

/**
 * Represents a chunk of output from a command execution, which can be either standard output, standard error, or an exit code.
 */
sealed class CommandChunk {
    /**
     * Represents a chunk of standard output from the command execution.
     *
     * @param data The standard output data as a string.
     */
    data class Stdout(
        val data: String,
    ) : CommandChunk()

    /**
     * Represents a chunk of standard error from the command execution.
     *
     * @param data The standard error data as a string.
     */
    data class Stderr(
        val data: String,
    ) : CommandChunk()

    /**
     * Represents the exit code of the command execution.
     *
     * @param code The exit code as an integer.
     */
    data class ExitCode(
        val code: Int,
    ) : CommandChunk()
}
