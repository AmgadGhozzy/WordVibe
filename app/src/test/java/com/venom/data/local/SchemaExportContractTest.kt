package com.venom.data.local

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Ticket 07: the Room schema is exported on every build (migration policy baseline).
 * Runs from the module directory, where KSP writes app/schemas/.
 */
class SchemaExportContractTest {

    private val v1File = File("schemas/com.venom.data.local.WordVibeDatabase/1.json")
    private val v2File = File("schemas/com.venom.data.local.WordVibeDatabase/2.json")

    @Test
    fun `v1 schema is exported`() {
        assertTrue(
            v1File.isFile,
            "Room schema must be exported (exportSchema + schemaLocation); " +
                "every future version bump starts from this file"
        )
    }

    @Test
    fun `v2 schema is exported and strictly validates against v1 baseline`() {
        assertTrue(
            v2File.isFile,
            "Room schema v2 must be exported (exportSchema + schemaLocation)"
        )

        val v1Json = Json.parseToJsonElement(v1File.readText()).jsonObject["database"]!!.jsonObject
        val v2Json = Json.parseToJsonElement(v2File.readText()).jsonObject["database"]!!.jsonObject

        // Version assertion
        assertEquals(2, v2Json["version"]!!.jsonPrimitive.int)

        val v1Entities = v1Json["entities"]!!.jsonArray.associateBy { it.jsonObject["tableName"]!!.jsonPrimitive.content }
        val v2Entities = v2Json["entities"]!!.jsonArray.associateBy { it.jsonObject["tableName"]!!.jsonPrimitive.content }

        // All entities from v1 exist in v2
        assertEquals(v1Entities.keys, v2Entities.keys)

        // For all tables other than wordsMaster, entity definitions must be identical
        for ((tableName, v1Entity) in v1Entities) {
            if (tableName != "wordsMaster") {
                assertEquals(v1Entity, v2Entities[tableName], "Entity $tableName must be identical between v1 and v2")
            }
        }

        // Validate wordsMaster in v2 has expected indexes: composite (cefrLevel, rank) and rank
        val wordsMasterV2 = v2Entities["wordsMaster"]!!.jsonObject
        val indices = wordsMasterV2["indices"]!!.jsonArray.map { it.jsonObject["name"]!!.jsonPrimitive.content }.toSet()
        val expectedIndices = setOf("index_wordsMaster_cefrLevel_rank", "index_wordsMaster_rank")
        assertEquals(expectedIndices, indices, "wordsMaster in v2 must contain exactly the composite and rank indices")
    }
}

