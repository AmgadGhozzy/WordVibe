package com.venom.domain.model

enum class KnownState {
    NEW,
    SEEN,
    LEARNING,
    KNOWN,
    MASTERED
}

data class UserWordProgress(
    val userId: String,
    val wordId: Int,

    // Exposure
    val firstSeen: Long = System.currentTimeMillis(),

    // Interaction
    val bookmarked: Boolean = false,

    // Practice totals
    val productionSuccess: Int = 0,
    val recallSuccess: Int = 0,
    val totalAttempts: Int = 0,

    // Learning state
    val knownState: KnownState = KnownState.NEW,

    // SRS
    val difficulty: Int = 5,
    val stability: Float = 0f,
    val lapsesCount: Int = 0,
    val lastReview: Long? = null,
    val nextReview: Long? = null
)
