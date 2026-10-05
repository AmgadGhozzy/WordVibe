package com.venom.quiz.placement

import com.venom.domain.model.PlacementLevels
import com.venom.domain.model.WordLevels
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * ADR-0010 scoring contracts over the approved corpus-floor ladder: the adaptive
 * cutoff must only react to levels that were actually attempted. Levels below the
 * ladder start are never asked, so breaking on them would assign a user who passed
 * the first floor level straight back to Beginner.
 */
class PlacementScoringOnFloorLadderContractTest {

    @Test
    fun `passing the ladder start assigns that level, not the one below it`() {
        val (level, cefr) = PlacementLevels.calculateAssignedLevel(mapOf("A2" to true))

        assertEquals(WordLevels.Elementary, level, "a passed floor level is honored")
        assertEquals("A2", cefr)
    }

    @Test
    fun `passing a mid-ladder level skips the unattempted levels below it`() {
        val (level, cefr) = PlacementLevels.calculateAssignedLevel(mapOf("B1" to true))

        assertEquals(WordLevels.Intermediate, level)
        assertEquals("B1", cefr)
    }

    @Test
    fun `adaptive cutoff still cuts at the first attempted failure`() {
        val (level, _) = PlacementLevels.calculateAssignedLevel(mapOf("A2" to true, "B1" to false))

        assertEquals(WordLevels.Elementary, level, "attempted failure still cuts")
    }

    @Test
    fun `failing the ladder start assigns the level below it`() {
        val (level, cefr) = PlacementLevels.calculateAssignedLevel(mapOf("A2" to false))

        assertEquals(WordLevels.Beginner, level)
        assertEquals("A1", cefr)
    }

    @Test
    fun `nothing attempted still assigns Beginner`() {
        val (level, cefr) = PlacementLevels.calculateAssignedLevel(emptyMap())

        assertEquals(WordLevels.Beginner, level)
        assertEquals("A1", cefr)
    }

    @Test
    fun `cap at B2 is unchanged`() {
        val (level, cefr) = PlacementLevels.calculateAssignedLevel(mapOf("C2" to true))

        assertEquals(WordLevels.UpperIntermediate, level, "C1/C2 stay progression-only")
        assertEquals("B2", cefr)
    }
}
