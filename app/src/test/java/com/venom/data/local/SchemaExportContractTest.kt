package com.venom.data.local

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Ticket 07: the Room schema is exported on every build (migration policy baseline).
 * Runs from the module directory, where KSP writes app/schemas/.
 */
class SchemaExportContractTest {

    @Test
    fun `v1 schema is exported`() {
        val schema = File("schemas/com.venom.data.local.WordVibeDatabase/1.json")

        assertTrue(
            schema.isFile,
            "Room schema must be exported (exportSchema + schemaLocation); " +
                "every future version bump starts from this file"
        )
    }
}
