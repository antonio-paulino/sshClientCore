package pt.paulinoo.sshClientCore.api

import kotlin.test.Test
import kotlin.test.assertEquals

class SshConfigTest {
    @Test
    fun `backend defaults to sshj`() {
        val config =
            SshConfig(
                host = "127.0.0.1",
                username = "user",
                password = "pass",
            )

        assertEquals(SshBackend.SSHJ, config.backend)
    }

    @Test
    fun `backend can be set to jsch`() {
        val config =
            SshConfig(
                host = "127.0.0.1",
                username = "user",
                password = "pass",
                backend = SshBackend.JSCH,
            )

        assertEquals(SshBackend.JSCH, config.backend)
    }
}

