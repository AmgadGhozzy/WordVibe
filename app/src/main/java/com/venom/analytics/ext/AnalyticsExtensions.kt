package com.venom.analytics.ext

import com.venom.analytics.AnalyticsEvent
import com.venom.analytics.AnalyticsManager
import com.venom.analytics.AnalyticsParam
import com.venom.analytics.CrashlyticsManager

/**
 * Extension functions for common analytics operations.
 * 
 * WHY: Extensions provide:
 * - Cleaner call sites in ViewModels and other classes
 * - Domain-specific convenience methods
 * - Type-safe event logging for WordVibe features
 */

// ═══════════════════════════════════════════════════════════════════
// LEARNING-SPECIFIC EXTENSIONS
// ═══════════════════════════════════════════════════════════════════

/**
 * Log when a word is learned/mastered.
 */
fun AnalyticsManager.logWordLearned(
    wordId: String,
    wordText: String,
    masteryLevel: Int
) {
    logEvent(
        AnalyticsEvent.WORD_LEARNED,
        mapOf(
            AnalyticsParam.WORD_ID to wordId,
            AnalyticsParam.WORD_TEXT to wordText,
            AnalyticsParam.MASTERY_LEVEL to masteryLevel
        )
    )
}

/**
 * Log a flashcard swipe action.
 */
fun AnalyticsManager.logFlashcardSwipe(
    wordId: String,
    direction: String, // "left", "right", "up"
    screenName: String = "Flashcard"
) {
    logEvent(
        AnalyticsEvent.FLASHCARD_SWIPED,
        mapOf(
            AnalyticsParam.WORD_ID to wordId,
            AnalyticsParam.SWIPE_DIRECTION to direction,
            AnalyticsParam.SCREEN_NAME to screenName
        )
    )
}

/**
 * Log quiz start.
 */
fun AnalyticsManager.logQuizStarted(
    quizType: String,
    wordCount: Int
) {
    logEvent(
        AnalyticsEvent.QUIZ_STARTED,
        mapOf(
            AnalyticsParam.CONTENT_TYPE to quizType,
            "word_count" to wordCount
        )
    )
}

/**
 * Log quiz completion.
 */
fun AnalyticsManager.logQuizCompleted(
    quizType: String,
    score: Int,
    totalQuestions: Int,
    durationMs: Long
) {
    logEvent(
        AnalyticsEvent.QUIZ_COMPLETED,
        mapOf(
            AnalyticsParam.CONTENT_TYPE to quizType,
            AnalyticsParam.QUIZ_SCORE to score,
            "total_questions" to totalQuestions,
            AnalyticsParam.DURATION_MS to durationMs
        )
    )
}

/**
 * Log translation action (basic version for backwards compatibility).
 */
fun AnalyticsManager.logTranslation(
    sourceLanguage: String,
    targetLanguage: String,
    textLength: Int
) {
    logEvent(
        AnalyticsEvent.TRANSLATION_PERFORMED,
        mapOf(
            "source_language" to sourceLanguage,
            "target_language" to targetLanguage,
            "text_length" to textLength
        )
    )
}

/**
 * Log translation action with full details including provider, duration, and result.
 */
fun AnalyticsManager.logTranslation(
    sourceLang: String,
    targetLang: String,
    provider: String,
    textLength: Int,
    durationMs: Long,
    success: Boolean
) {
    logEvent(
        AnalyticsEvent.TRANSLATION_PERFORMED,
        mapOf(
            "source_language" to sourceLang,
            "target_language" to targetLang,
            "provider" to provider,
            "text_length" to textLength,
            AnalyticsParam.DURATION_MS to durationMs,
            AnalyticsParam.ACTION_RESULT to if (success) "success" else "failure"
        )
    )
}

/**
 * Log OCR scan completion.
 */
fun AnalyticsManager.logOcrScan(
    success: Boolean,
    detectedTextLength: Int,
    durationMs: Long
) {
    logEvent(
        AnalyticsEvent.OCR_SCAN_COMPLETED,
        mapOf(
            AnalyticsParam.ACTION_RESULT to if (success) "success" else "failure",
            "detected_text_length" to detectedTextLength,
            AnalyticsParam.DURATION_MS to durationMs
        )
    )
}

/**
 * Log pronunciation playback.
 */
fun AnalyticsManager.logPronunciationPlayed(
    wordId: String,
    language: String
) {
    logEvent(
        AnalyticsEvent.PRONUNCIATION_PLAYED,
        mapOf(
            AnalyticsParam.WORD_ID to wordId,
            "language" to language
        )
    )
}

/**
 * Log spelling practice completion.
 */
fun AnalyticsManager.logSpellingCompleted(
    wordId: String,
    isCorrect: Boolean,
    attempts: Int
) {
    logEvent(
        AnalyticsEvent.SPELLING_COMPLETED,
        mapOf(
            AnalyticsParam.WORD_ID to wordId,
            AnalyticsParam.ACTION_RESULT to if (isCorrect) "correct" else "incorrect",
            "attempts" to attempts
        )
    )
}

// ═══════════════════════════════════════════════════════════════════
// ONBOARDING EXTENSIONS
// ═══════════════════════════════════════════════════════════════════

/**
 * Log onboarding started.
 */
fun AnalyticsManager.logOnboardingStarted() {
    logEvent(AnalyticsEvent.ONBOARDING_STARTED)
}

/**
 * Log onboarding step completed.
 */
fun AnalyticsManager.logOnboardingStep(stepNumber: Int, stepName: String) {
    logEvent(
        AnalyticsEvent.ONBOARDING_STEP,
        mapOf(
            AnalyticsParam.STEP_NUMBER to stepNumber,
            AnalyticsParam.STEP_NAME to stepName
        )
    )
}

/**
 * Log onboarding completed.
 */
fun AnalyticsManager.logOnboardingCompleted(totalTimeMs: Long) {
    logEvent(
        AnalyticsEvent.ONBOARDING_COMPLETED,
        mapOf(AnalyticsParam.DURATION_MS to totalTimeMs)
    )
}

/**
 * Log onboarding skipped.
 */
fun AnalyticsManager.logOnboardingSkipped(atStep: Int) {
    logEvent(
        AnalyticsEvent.ONBOARDING_SKIPPED,
        mapOf(AnalyticsParam.STEP_NUMBER to atStep)
    )
}

// ═══════════════════════════════════════════════════════════════════
// CRASHLYTICS EXTENSIONS
// ═══════════════════════════════════════════════════════════════════

/**
 * Log a non-fatal API error to Crashlytics.
 */
fun CrashlyticsManager.logApiError(
    endpoint: String,
    errorCode: Int?,
    errorMessage: String
) {
    logBreadcrumb("API Error: $endpoint - $errorCode - $errorMessage")
    setCustomKey("last_api_error_endpoint", endpoint)
    errorCode?.let { setCustomKey("last_api_error_code", it) }
}

/**
 * Log user action as breadcrumb for crash context.
 */
fun CrashlyticsManager.logUserAction(
    action: String,
    details: String? = null
) {
    val message = if (details != null) {
        "User action: $action - $details"
    } else {
        "User action: $action"
    }
    logBreadcrumb(message)
}
