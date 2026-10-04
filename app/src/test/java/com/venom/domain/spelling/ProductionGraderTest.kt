package com.venom.domain.spelling

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * ADR-0008 grading contracts (RED before implementation): free-typed input is
 * normalized (case + whitespace), graded exact against expected + variants,
 * Levenshtein-1 is a near-miss, everything else is a full error. Pure judgment —
 * the grader records nothing and knows nothing about sessions.
 */
class ProductionGraderTest {

    @Test
    fun `exact match is correct`() {
        assertEquals(
            ProductionGrade.CORRECT,
            ProductionGrader.grade("substantial", "substantial", emptyList())
        )
    }

    @Test
    fun `normalization ignores case and whitespace`() {
        assertEquals(
            ProductionGrade.CORRECT,
            ProductionGrader.grade("  SUBstantial  ", "substantial", emptyList())
        )
        assertEquals(
            ProductionGrade.CORRECT,
            ProductionGrader.grade("give up", "Give   Up", emptyList())
        )
    }

    @Test
    fun `accepted variant counts as correct`() {
        assertEquals(
            ProductionGrade.CORRECT,
            ProductionGrader.grade("substantially", "substantial", listOf("substantially"))
        )
    }

    @Test
    fun `single edit distance is a near miss`() {
        assertEquals(
            ProductionGrade.NEAR_MISS,
            ProductionGrader.grade("substantal", "substantial", emptyList())
        )
        // One substitution, one insertion, one deletion — all distance 1.
        assertEquals(ProductionGrade.NEAR_MISS, ProductionGrader.grade("substantiam", "substantial", emptyList()))
        assertEquals(ProductionGrade.NEAR_MISS, ProductionGrader.grade("obtan", "obtain", emptyList()))
    }

    @Test
    fun `distance two or more is a full error`() {
        assertEquals(
            ProductionGrade.INCORRECT,
            ProductionGrader.grade("substan", "substantial", emptyList())
        )
        assertEquals(ProductionGrade.INCORRECT, ProductionGrader.grade("totally", "substantial", emptyList()))
    }

    @Test
    fun `blank input is never correct`() {
        assertEquals(ProductionGrade.INCORRECT, ProductionGrader.grade("", "substantial", emptyList()))
        assertEquals(ProductionGrade.INCORRECT, ProductionGrader.grade("   ", "substantial", emptyList()))
    }
}
