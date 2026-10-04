package com.venom.analytics

/**
 * Crashlytics abstraction interface.
 * 
 * WHY: Abstracting Firebase Crashlytics behind an interface enables:
 * - Easy testing with mock/no-op implementations
 * - Swapping crash reporting providers
 * - Centralized crash policy enforcement
 * - Type-safe breadcrumb logging
 * 
 * BEST PRACTICES:
 * - Always add contextual breadcrumbs before risky operations
 * - Log non-fatal errors for gracefully handled exceptions
 * - Set custom keys for debugging context
 * - Never log PII or sensitive data
 */
interface CrashlyticsManager {

    /**
     * Initialize Crashlytics.
     * Should be called once during app startup.
     * 
     * @param enableCollection Whether to enable crash collection.
     *                         Set to false for GDPR consent handling.
     */
    fun initialize(enableCollection: Boolean = true)

    /**
     * Log a non-fatal exception.
     * Use for exceptions that are caught and handled gracefully.
     * 
     * @param throwable The exception to log
     * @param message Optional contextual message
     */
    fun logNonFatalException(throwable: Throwable, message: String? = null)

    /**
     * Log a breadcrumb message.
     * Breadcrumbs appear in crash reports to help understand what happened.
     * 
     * @param message The breadcrumb message (max 64KB total for all breadcrumbs)
     * 
     * GUIDELINES:
     * - Keep messages concise
     * - Include relevant state information
     * - Log at key decision points
     */
    fun logBreadcrumb(message: String)

    /**
     * Set a custom key-value pair.
     * Custom keys appear in crash reports for debugging.
     * 
     * @param key Key name (max 64 chars)
     * @param value Value (max 1024 chars for strings)
     * 
     * IMPORTANT: Never set PII as custom keys!
     */
    fun setCustomKey(key: String, value: String)
    fun setCustomKey(key: String, value: Boolean)
    fun setCustomKey(key: String, value: Int)
    fun setCustomKey(key: String, value: Long)
    fun setCustomKey(key: String, value: Float)
    fun setCustomKey(key: String, value: Double)

    /**
     * Set the current screen name.
     * Appears in crash reports to indicate where the crash occurred.
     * 
     * @param screenName The current screen name
     */
    fun setCurrentScreen(screenName: String)

    /**
     * Set the current feature name.
     * Useful for knowing which feature the user was using during a crash.
     * 
     * @param featureName The current feature name
     */
    fun setCurrentFeature(featureName: String)

    /**
     * Set a custom user identifier.
     * Use only non-PII identifiers (anonymous user ID, not email/phone).
     * 
     * @param userId Anonymous user identifier
     */
    fun setUserId(userId: String?)

    /**
     * Enable or disable crash collection.
     * Use for GDPR consent management.
     * 
     * @param enabled True to enable, false to disable collection
     */
    fun setCrashlyticsCollectionEnabled(enabled: Boolean)

    /**
     * Delete unsent crash reports.
     * Use when user denies consent and requests data deletion.
     */
    suspend fun deleteUnsentReports()

    /**
     * Check if there are unsent crash reports from a previous session.
     * 
     * @return True if there are unsent reports
     */
    suspend fun hasUnsentReports(): Boolean

    /**
     * Send any unsent crash reports.
     * Call this after user grants consent.
     */
    suspend fun sendUnsentReports()
}
