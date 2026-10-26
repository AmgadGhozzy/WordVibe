package com.venom.wordvibe.ui

import com.venom.data.local.entity.WordMasterEntity
import com.venom.harness.VmSessionHarness
import com.venom.stackcard.session.UnitGate
import com.venom.wordvibe.R
import com.venom.wordvibe.ui.screen.unitTopicTitleRes
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UnitsUiStateAndLocalizationTest {

    private fun makeWord(
        id: Int,
        topic: String,
        cefr: String = "B1",
        rank: Int = 1000
    ): WordMasterEntity = WordMasterEntity(
        id = id,
        wordEn = "word$id",
        pos = "noun",
        cefrLevel = cefr,
        rank = rank,
        frequency = 3,
        difficultyScore = 5,
        phoneticUs = "wɜːd",
        phoneticAr = "وورد",
        definitionEn = "Definition $id",
        arabicAr = "معنى $id",
        ieltsTopic = topic
    )

    @Test
    fun `getUnitsStatusForUser returns ten units in EDITORIAL_CHAIN order with unit 1 open and others locked`() = runTest {
        val words = UnitGate.EDITORIAL_CHAIN.flatMapIndexed { idx, topic ->
            listOf(
                makeWord(idx * 2 + 1, topic),
                makeWord(idx * 2 + 2, topic)
            )
        }
        val harness = VmSessionHarness(userId = "u1", words = words)
        try {
            val statusList = harness.progressRepository.getUnitsStatusForUser("u1", null)
            assertEquals(10, statusList.size)
            assertEquals(UnitGate.EDITORIAL_CHAIN, statusList.map { it.topic })

            // Unit 1 is open
            val unit1 = statusList[0]
            assertEquals("Education", unit1.topic)
            assertTrue(unit1.isOpen)
            assertNull(unit1.lockReason)
            assertEquals(2, unit1.usableCount)
            assertEquals(0, unit1.knownCount)

            // Remaining 9 units are locked with non-null lockReason and remaining > 0
            for (i in 1..9) {
                val unit = statusList[i]
                assertFalse(unit.isOpen, "Unit ${unit.topic} should be locked")
                assertNotNull(unit.lockReason, "Unit ${unit.topic} must have non-null lockReason")
                assertTrue(unit.lockReason!!.remaining > 0, "Remaining for ${unit.topic} must be > 0")
            }
        } finally {
            harness.close()
        }
    }

    @Test
    fun `UnitStatus and LockReason do not contain Arabic script or prebaked UI text`() = runTest {
        val words = listOf(
            makeWord(1, "Education"),
            makeWord(2, "Work")
        )
        val harness = VmSessionHarness(userId = "u1", words = words)
        try {
            val statusList = harness.progressRepository.getUnitsStatusForUser("u1", null)
            val arabicRegex = Regex("""[\u0600-\u06FF]""")
            for (status in statusList) {
                // Topic should be standard English identifier
                assertFalse(
                    arabicRegex.containsMatchIn(status.topic),
                    "Topic '${status.topic}' must not contain Arabic script"
                )
                // LockReason contains only numbers, no localized text
                status.lockReason?.let { reason ->
                    assertTrue(reason.requiredKnownCount >= 0)
                    assertTrue(reason.knownCount >= 0)
                    assertTrue(reason.remaining >= 0)
                }
            }
        } finally {
            harness.close()
        }
    }

    @Test
    fun `unitTopicTitleRes covers all ten EDITORIAL_CHAIN topics with distinct resource IDs`() {
        val mappedRes = UnitGate.EDITORIAL_CHAIN.map { topic ->
            topic to unitTopicTitleRes(topic)
        }

        // All 10 topics return a valid resource ID
        assertEquals(10, mappedRes.size)

        // All 10 resource IDs must be distinct
        val distinctRes = mappedRes.map { it.second }.toSet()
        assertEquals(
            10,
            distinctRes.size,
            "Each of the 10 topics must map to a unique R.string.unit_topic_* resource"
        )

        // Verify specific mappings
        assertEquals(R.string.unit_topic_education, unitTopicTitleRes("Education"))
        assertEquals(R.string.unit_topic_work, unitTopicTitleRes("Work"))
        assertEquals(R.string.unit_topic_health, unitTopicTitleRes("Health"))
        assertEquals(R.string.unit_topic_travel, unitTopicTitleRes("Travel"))
        assertEquals(R.string.unit_topic_technology, unitTopicTitleRes("Technology"))
        assertEquals(R.string.unit_topic_environment, unitTopicTitleRes("Environment"))
        assertEquals(R.string.unit_topic_culture, unitTopicTitleRes("Culture"))
        assertEquals(R.string.unit_topic_society, unitTopicTitleRes("Society"))
        assertEquals(R.string.unit_topic_economy, unitTopicTitleRes("Economy"))
        assertEquals(R.string.unit_topic_science, unitTopicTitleRes("Science"))
    }
}
