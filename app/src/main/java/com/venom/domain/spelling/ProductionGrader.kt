package com.venom.domain.spelling

/**
 * Grading outcome for a free-typed production attempt (ADR-0008). Pure judgment:
 * the grader records nothing and knows nothing about sessions or scheduling.
 */
enum class ProductionGrade { CORRECT, NEAR_MISS, INCORRECT }

/**
 * Grades a typed production attempt against the corpus contract's explicit targets
 * (ADR-0005: expectedForm + acceptedVariants, never inferred from the word form).
 *
 * Normalization: case-insensitive, whitespace collapsed and trimmed.
 * Exact match (expected or any variant) -> CORRECT.
 * Levenshtein distance exactly 1 -> NEAR_MISS ("قريب جدًا" + retry, unrecorded).
 * Anything else -> INCORRECT.
 */
object ProductionGrader {

    fun grade(input: String, expected: String, variants: List<String>): ProductionGrade {
        val typed = normalize(input)
        if (typed.isEmpty()) return ProductionGrade.INCORRECT
        if (typed == normalize(expected)) return ProductionGrade.CORRECT
        if (variants.any { typed == normalize(it) }) return ProductionGrade.CORRECT
        return if (levenshtein(typed, normalize(expected)) == 1 ||
            variants.any { levenshtein(typed, normalize(it)) == 1 }
        ) {
            ProductionGrade.NEAR_MISS
        } else {
            ProductionGrade.INCORRECT
        }
    }

    fun normalize(text: String): String =
        text.trim().lowercase().replace(whitespace, " ")

    private val whitespace = Regex("""\s+""")

    /** Classic two-row Levenshtein; inputs are short single words, so O(n·m) is fine. */
    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in a.indices) {
            current[0] = i + 1
            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                current[j + 1] = minOf(current[j] + 1, previous[j + 1] + 1, previous[j] + cost)
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }
}
