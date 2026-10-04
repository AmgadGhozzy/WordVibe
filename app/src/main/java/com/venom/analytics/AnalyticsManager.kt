package com.venom.analytics

import kotlinx.coroutines.flow.StateFlow

/**
 * Analytics abstraction interface.
 * 
 * WHY: Abstracting Firebase Analytics behind an interface enables:
 * - Easy testing with mock implementations
 * - Swapping analytics providers without code changes
 * - Type-safe event logging
 * - Centralized analytics policy enforcement
 * 
 * DESIGN PRINCIPLES:
 * - All methods are side-effect free for thread safety
 * - No blocking operations on main thread
 * - Parameters are sanitized automatically
 * - GDPR-compliant by design
 */
interface AnalyticsManager {

    /**
     * Current analytics collection enabled state.
     * Respects user consent preferences.
     */
    val isEnabled: StateFlow<Boolean>

    /**
     * Initialize analytics.
     * Should be called once during app startup.
     * 
     * @param enableCollection Whether to enable analytics collection.
     *                         Set to false for GDPR consent handling.
     */
    fun initialize(enableCollection: Boolean = true)

    /**
     * Log a screen view event.
     * Call this when a new screen becomes visible to the user.
     * 
     * @param screenName Human-readable screen name (e.g., "Home", "Settings")
     * @param screenClass Optional screen class name for debugging
     */
    fun logScreenView(screenName: String, screenClass: String? = null)

    /**
     * Log a custom event with optional parameters.
     * 
     * @param eventName The event name (use constants from [AnalyticsEvent])
     * @param params Optional key-value parameters (use keys from [AnalyticsParam])
     * 
     * LIMITS:
     * - Event name: max 40 chars
     * - Parameter key: max 40 chars
     * - Parameter value: max 100 chars
     */
    fun logEvent(eventName: String, params: Map<String, Any>? = null)

    /**
     * Log a button click event.
     * Convenience method for standardized button tracking.
     * 
     * @param buttonName The button identifier
     * @param screenName The screen where the button was clicked
     */
    fun logButtonClick(buttonName: String, screenName: String)

    /**
     * Log a feature opened event.
     * Call when user enters a distinct feature area.
     * 
     * @param featureName The feature identifier
     * @param additionalParams Optional additional parameters
     */
    fun logFeatureOpened(featureName: String, additionalParams: Map<String, Any>? = null)

    /**
     * Log a feature completed event.
     * Call when user successfully completes an action within a feature.
     * 
     * @param featureName The feature identifier
     * @param result Optional result description
     * @param durationMs Optional duration in milliseconds
     */
    fun logFeatureCompleted(
        featureName: String,
        result: String? = null,
        durationMs: Long? = null
    )

    /**
     * Log a handled error (non-fatal).
     * Use for gracefully handled errors that don't crash the app.
     * 
     * @param errorType Error category (e.g., "network", "validation")
     * @param errorMessage Human-readable error description
     * @param screenName Current screen where error occurred
     */
    fun logError(
        errorType: String,
        errorMessage: String,
        screenName: String? = null
    )

    /**
     * Log a performance marker.
     * Use for measuring app performance metrics.
     * 
     * @param markerName The performance metric name
     * @param durationMs Duration in milliseconds
     * @param additionalParams Optional additional context
     */
    fun logPerformanceMarker(
        markerName: String,
        durationMs: Long,
        additionalParams: Map<String, Any>? = null
    )

    /**
     * Set a user property.
     * User properties are persistent attributes attached to all events.
     * 
     * @param name Property name (max 24 chars)
     * @param value Property value (max 36 chars), null to clear
     * 
     * IMPORTANT: Never set PII as user properties!
     */
    fun setUserProperty(name: String, value: String?)

    /**
     * Enable or disable analytics collection.
     * Use for GDPR consent management.
     * 
     * @param enabled True to enable, false to disable collection
     */
    fun setAnalyticsCollectionEnabled(enabled: Boolean)

    /**
     * Reset analytics data.
     * Clears the analytics instance ID and associated data.
     * Use when user requests data deletion (GDPR Right to Erasure).
     */
    suspend fun resetAnalyticsData()
}
