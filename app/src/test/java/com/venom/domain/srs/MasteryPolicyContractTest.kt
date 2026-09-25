package com.venom.domain.srs

import com.venom.domain.model.KnownState
import com.venom.domain.model.UserWordProgress
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Ticket 04e golden contracts: the frozen Definition of Mastery enforced in code.
 * MASTERED ⟺ cumulative recall≥3 AND production≥3 AND stability≥2.0; any real failure
 * demotes to KNOWN without erasing history. No windows, no weights, no exceptions.
 */
class MasteryPolicyContractTest {

    private val engine = SrsEngine(clock = { 1_000_000L })

    private fun progress(
        recall: Int = 0,
        production: Int = 0,
        stability: Float = 0f,
        state: KnownState = KnownState.NEW
    ) = UserWordProgress(
        userId = "u",
        wordId = 1,
        recallSuccess = recall,
        productionSuccess = production,
        totalAttempts = recall + production,
        knownState = state,
        stability = stability
    )

    @Test
    fun `mastery requires all three conjuncts`() {
        assertEquals(KnownState.MASTERED, engine.updateKnownState(progress(3, 3, 2.0f)))
        assertEquals(
            KnownState.KNOWN, engine.updateKnownState(progress(3, 3, 1.9f)),
            "below-stability 3/3 must not master"
        )
        assertEquals(
            KnownState.KNOWN, engine.updateKnownState(progress(2, 3, 5.0f)),
            "2/3 recalls must not master at any stability"
        )
        assertEquals(
            KnownState.KNOWN, engine.updateKnownState(progress(3, 2, 5.0f)),
            "3/2 productions must not master at any stability"
        )
    }

    @Test
    fun `single-mode practice never masters`() {
        assertEquals(
            KnownState.KNOWN, engine.updateKnownState(progress(0, 5, 9.0f)),
            "spelling-only practice must never master"
        )
        assertEquals(
            KnownState.KNOWN, engine.updateKnownState(progress(5, 0, 9.0f)),
            "recall-only practice must never master"
        )
    }

    @Test
    fun `counters accumulate across transitions without windows`() {
        var state = UserWordProgress(userId = "u", wordId = 1)

        repeat(3) { n ->
            state = engine.onRecallSuccess(state, rank = 1000, frequency = 1)
            assertEquals(n + 1, state.recallSuccess, "success ${n + 1} accumulates")
        }

        assertEquals(KnownState.KNOWN, engine.updateKnownState(state))
        assertEquals(0, state.lapsesCount, "no failure occurred in between")
    }

    @Test
    fun `any real failure demotes mastered to known`() {
        val mastered = progress(3, 3, 3.0f, KnownState.MASTERED)

        assertEquals(KnownState.KNOWN, engine.onRecallFail(mastered).knownState)
        assertEquals(KnownState.KNOWN, engine.onProductionFail(mastered).knownState)
    }

    @Test
    fun `demotion preserves history`() {
        val mastered = progress(3, 3, 3.0f, KnownState.MASTERED)

        val demoted = engine.onRecallFail(mastered)

        assertEquals(3, demoted.recallSuccess, "counters survive demotion")
        assertEquals(3, demoted.productionSuccess, "counters survive demotion")
        assertEquals(1, demoted.lapsesCount, "the failure itself is recorded")
        assertEquals(7, demoted.totalAttempts, "attempt history survives demotion")
        assertEquals(1.0f, demoted.stability, "stability carries forward minus the frozen delta")
    }
}
