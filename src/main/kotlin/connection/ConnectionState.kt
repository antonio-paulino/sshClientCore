package pt.paulinoo.connection

/**
 * Represents the state of a connection.
 * This sealed class can be used to model the different states of a connection, such as connected, disconnected, or
 * error.
 */
sealed class ConnectionState {
    /**
     * Represents a successful connection state.
     */
    object Connected : ConnectionState()

    /**
     * Represents a disconnected state.
     */
    object Disconnected : ConnectionState()

    /**
     * Represents an error state, containing the throwable that caused the error.
     * @param throwable The error that occurred during the connection process.
     */
    data class Error(
        val throwable: Throwable,
    ) : ConnectionState()
}
