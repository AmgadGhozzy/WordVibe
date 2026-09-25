package com.venom.domain.srs

import com.venom.domain.model.KnownState
import com.venom.domain.model.UserWordProgress

/**
 * Spaced Repetition System engine.
 * Calculates stability and schedules reviews based on learning science.
 */
class SrsEngine(
    private val clock: () -> Long = System::currentTimeMillis
) {
    
    companion object {
        private const val MASTERY_PRODUCTION_THRESHOLD = 3
        private const val MASTERY_RECALL_THRESHOLD = 3
        private const val MASTERY_STABILITY_THRESHOLD = 2.0f

        /**
         * Frozen quality magnitudes (04c): recall failure is the stronger negative.
         * Exact values live with their golden tests; change only by reopening the freeze.
         */
        private const val RECALL_SUCCESS_WEIGHT = 1.2f
        private const val PRODUCTION_SUCCESS_WEIGHT = 2.0f
        private const val LAPSE_PENALTY = 1.5f
        private const val RECALL_FAIL_STABILITY_DELTA = -2.0f
        private const val PRODUCTION_FAIL_STABILITY_DELTA = -1.0f
    }
    
    /**
     * Calculate word stability based on static and dynamic factors.
     */
    fun calculateStability(
        rank: Int,
        frequency: Int,
        productionSuccess: Int,
        recallSuccess: Int,
        lapsesCount: Int
    ): Float {
        val base = (1000f - rank.coerceAtMost(1000)) / 1000f * 0.3f + 
                   frequency.coerceIn(1, 6) / 6f * 0.2f
        
        return (base +
            productionSuccess * PRODUCTION_SUCCESS_WEIGHT +
            recallSuccess * RECALL_SUCCESS_WEIGHT -
            lapsesCount * LAPSE_PENALTY)
            .coerceAtLeast(0f)
    }
    
    /**
     * Calculate next review time in milliseconds.
     */
    fun calculateNextReview(stability: Float, difficultyScore: Int): Long {
        val now = clock()
        val baseIntervalHours = when {
            stability < 1f -> 4       // 4 hours
            stability < 2f -> 24      // 1 day
            stability < 4f -> 72      // 3 days
            stability < 6f -> 168     // 1 week
            else -> 336               // 2 weeks
        }
        
        val difficultyFactor = 1.0f - (difficultyScore.coerceIn(1, 10) / 20f)
        val intervalMs = (baseIntervalHours * 60 * 60 * 1000 * difficultyFactor).toLong().coerceAtLeast(1L)
        
        return now + intervalMs
    }
    
    /**
     * Determine new learning state based on progress.
     */
    fun updateKnownState(progress: UserWordProgress): KnownState {
        return when {
            progress.productionSuccess >= MASTERY_PRODUCTION_THRESHOLD &&
            progress.recallSuccess >= MASTERY_RECALL_THRESHOLD &&
            progress.stability >= MASTERY_STABILITY_THRESHOLD -> KnownState.MASTERED
            
            progress.productionSuccess >= 1 || progress.recallSuccess >= 1 -> KnownState.KNOWN
            
            progress.totalAttempts > 0 -> KnownState.LEARNING
            
            progress.firstSeen > 0 -> KnownState.SEEN
            
            else -> KnownState.NEW
        }
    }
    
    /**
     * Process a successful recall event.
     * V2: uses progress.difficulty instead of hardcoded 5.
     */
    fun onRecallSuccess(progress: UserWordProgress, rank: Int, frequency: Int): UserWordProgress {
        val newRecallSuccess = progress.recallSuccess + 1
        val newTotalAttempts = progress.totalAttempts + 1
        val newStability = calculateStability(
            rank, frequency, progress.productionSuccess, newRecallSuccess, progress.lapsesCount
        )
        val now = clock()
        
        val updated = progress.copy(
            recallSuccess = newRecallSuccess,
            totalAttempts = newTotalAttempts,
            stability = newStability,
            lastReview = now,
            nextReview = calculateNextReview(newStability, progress.difficulty)
        )
        return updated.copy(knownState = updateKnownState(updated))
    }
    
    /**
     * Process a failed recall event.
     */
    fun onRecallFail(progress: UserWordProgress): UserWordProgress =
        onFail(progress, RECALL_FAIL_STABILITY_DELTA)

    /**
     * Process a failed production event (spelling, writing).
     * Mirrors recall failure; exact quality magnitudes freeze with their tests in 04c.
     */
    fun onProductionFail(progress: UserWordProgress): UserWordProgress =
        onFail(progress, PRODUCTION_FAIL_STABILITY_DELTA)

    private fun onFail(progress: UserWordProgress, stabilityDelta: Float): UserWordProgress {
        val newLapses = progress.lapsesCount + 1
        val newTotalAttempts = progress.totalAttempts + 1
        val reducedStability = (progress.stability + stabilityDelta).coerceAtLeast(0f)
        // Increase difficulty on failure (max 10)
        val newDifficulty = (progress.difficulty + 1).coerceAtMost(10)
        val now = clock()

        val updated = progress.copy(
            totalAttempts = newTotalAttempts,
            lapsesCount = newLapses,
            stability = reducedStability,
            difficulty = newDifficulty,
            lastReview = now,
            nextReview = calculateNextReview(reducedStability, newDifficulty),
            knownState = if (progress.knownState == KnownState.MASTERED) KnownState.KNOWN else progress.knownState
        )
        return updated
    }

    /**
     * Process production success (spelling, writing).
     * V2: uses progress.difficulty instead of hardcoded 4.
     */
    fun onProductionSuccess(progress: UserWordProgress, rank: Int, frequency: Int): UserWordProgress {
        val newProductionSuccess = progress.productionSuccess + 1
        val newTotalAttempts = progress.totalAttempts + 1
        val newStability = calculateStability(
            rank, frequency, newProductionSuccess, progress.recallSuccess, progress.lapsesCount
        )
        // Decrease difficulty on success (min 1)
        val newDifficulty = (progress.difficulty - 1).coerceAtLeast(1)
        val now = clock()
        
        val updated = progress.copy(
            productionSuccess = newProductionSuccess,
            totalAttempts = newTotalAttempts,
            stability = newStability,
            difficulty = newDifficulty,
            lastReview = now,
            nextReview = calculateNextReview(newStability, newDifficulty)
        )
        return updated.copy(knownState = updateKnownState(updated))
    }
}
