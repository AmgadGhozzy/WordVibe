package com.venom.analytics

/**
 * Centralized event constants for the application.
 * 
 * WHY: Using constants instead of string literals prevents typos,
 * enables IDE autocomplete, and makes event auditing straightforward.
 * 
 * NAMING CONVENTION:
 * - Events: snake_case (Firebase best practice)
 * - Parameters: snake_case
 * - Prefix with feature name for feature-specific events
 * 
 * EVENT LIMITS (Firebase):
 * - Max 500 unique events
 * - Event name: max 40 characters
 * - Parameter name: max 40 characters
 * - Parameter value: max 100 characters
 */
object AnalyticsEvent {

    // ═══════════════════════════════════════════════════════════════════
    // SCREEN VIEWS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Automatically logged screen view event.
     * Firebase uses "screen_view" internally.
     */
    const val SCREEN_VIEW = "screen_view"
    
    // ═══════════════════════════════════════════════════════════════════
    // APP LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════
    
    /** User opened the app */
    const val APP_OPEN = "app_open"
    
    /** User returned to the app from background */
    const val APP_RESUME = "app_resume"
    
    /** User sent app to background */
    const val APP_BACKGROUND = "app_background"
    
    /** Session started (configurable threshold) */
    const val SESSION_START = "session_start"
    
    // ═══════════════════════════════════════════════════════════════════
    // FEATURE USAGE
    // ═══════════════════════════════════════════════════════════════════
    
    /** Generic feature opened event */
    const val FEATURE_OPENED = "feature_opened"
    
    /** Feature closed or exited */
    const val FEATURE_CLOSED = "feature_closed"
    
    /** User started using a feature */
    const val FEATURE_STARTED = "feature_started"
    
    /** User completed an action within a feature */
    const val FEATURE_COMPLETED = "feature_completed"
    
    // ═══════════════════════════════════════════════════════════════════
    // USER INTERACTIONS
    // ═══════════════════════════════════════════════════════════════════
    
    /** Button click event */
    const val BUTTON_CLICK = "button_click"
    
    /** Menu item selected */
    const val MENU_SELECTED = "menu_selected"
    
    /** Navigation action */
    const val NAVIGATION_ACTION = "navigation_action"
    
    /** Settings changed */
    const val SETTINGS_CHANGED = "settings_changed"
    
    /** Search performed */
    const val SEARCH_PERFORMED = "search_performed"
    
    /** Filter applied */
    const val FILTER_APPLIED = "filter_applied"
    
    /** Share action */
    const val SHARE_ACTION = "share_action"
    
    // ═══════════════════════════════════════════════════════════════════
    // CONTENT ENGAGEMENT
    // ═══════════════════════════════════════════════════════════════════
    
    /** Content viewed */
    const val CONTENT_VIEWED = "content_viewed"
    
    /** Content liked/favorited */
    const val CONTENT_FAVORITED = "content_favorited"
    
    /** Content saved */
    const val CONTENT_SAVED = "content_saved"
    
    // ═══════════════════════════════════════════════════════════════════
    // LEARNING-SPECIFIC
    // ═══════════════════════════════════════════════════════════════════
    
    /** Word learned/mastered */
    const val WORD_LEARNED = "word_learned"
    
    /** Flashcard swiped */
    const val FLASHCARD_SWIPED = "flashcard_swiped"
    
    /** Quiz started */
    const val QUIZ_STARTED = "quiz_started"
    
    /** Quiz completed */
    const val QUIZ_COMPLETED = "quiz_completed"
    
    /** Translation performed */
    const val TRANSLATION_PERFORMED = "translation_performed"
    
    /** OCR scan completed */
    const val OCR_SCAN_COMPLETED = "ocr_scan_completed"
    
    /** Pronunciation played */
    const val PRONUNCIATION_PLAYED = "pronunciation_played"
    
    /** Spelling practice completed */
    const val SPELLING_COMPLETED = "spelling_completed"
    
    // ═══════════════════════════════════════════════════════════════════
    // PERFORMANCE MARKERS
    // ═══════════════════════════════════════════════════════════════════
    
    /** Measures app startup time */
    const val PERF_APP_STARTUP = "perf_app_startup"
    
    /** Measures screen load time */
    const val PERF_SCREEN_LOAD = "perf_screen_load"
    
    /** Measures API response time */
    const val PERF_API_RESPONSE = "perf_api_response"
    
    // ═══════════════════════════════════════════════════════════════════
    // ERROR TRACKING (Non-fatal)
    // ═══════════════════════════════════════════════════════════════════
    
    /** Handled error event */
    const val ERROR_HANDLED = "error_handled"
    
    /** API error occurred */
    const val API_ERROR = "api_error"
    
    /** Validation error */
    const val VALIDATION_ERROR = "validation_error"
    
    // ═══════════════════════════════════════════════════════════════════
    // ONBOARDING
    // ═══════════════════════════════════════════════════════════════════
    
    /** Onboarding started */
    const val ONBOARDING_STARTED = "onboarding_started"
    
    /** Onboarding step completed */
    const val ONBOARDING_STEP = "onboarding_step"
    
    /** Onboarding completed */
    const val ONBOARDING_COMPLETED = "onboarding_completed"
    
    /** Onboarding skipped */
    const val ONBOARDING_SKIPPED = "onboarding_skipped"
}

/**
 * Parameter keys for analytics events.
 * 
 * WHY: Centralizing parameter names ensures consistency
 * across all events and prevents key mismatches in analytics dashboards.
 */
object AnalyticsParam {
    
    // Screen tracking
    const val SCREEN_NAME = "screen_name"
    const val SCREEN_CLASS = "screen_class"
    const val PREVIOUS_SCREEN = "previous_screen"
    
    // Feature tracking
    const val FEATURE_NAME = "feature_name"
    const val FEATURE_ID = "feature_id"
    
    // Action tracking
    const val BUTTON_NAME = "button_name"
    const val ACTION_TYPE = "action_type"
    const val ACTION_RESULT = "action_result"
    
    // Content tracking
    const val CONTENT_TYPE = "content_type"
    const val CONTENT_ID = "content_id"
    const val CONTENT_NAME = "content_name"
    
    // Performance tracking
    const val DURATION_MS = "duration_ms"
    const val LOAD_TIME_MS = "load_time_ms"
    const val RESPONSE_TIME_MS = "response_time_ms"
    
    // Error tracking
    const val ERROR_TYPE = "error_type"
    const val ERROR_MESSAGE = "error_message"
    const val ERROR_CODE = "error_code"
    
    // Learning-specific
    const val WORD_ID = "word_id"
    const val WORD_TEXT = "word_text"
    const val MASTERY_LEVEL = "mastery_level"
    const val QUIZ_SCORE = "quiz_score"
    const val SWIPE_DIRECTION = "swipe_direction"
    
    // Session tracking
    const val SESSION_ID = "session_id"
    const val SESSION_DURATION = "session_duration"
    
    // Metadata (anonymized, non-PII)
    const val APP_VERSION = "app_version"
    const val BUILD_TYPE = "build_type"
    const val TIMESTAMP = "timestamp"
    
    // Onboarding
    const val STEP_NUMBER = "step_number"
    const val STEP_NAME = "step_name"
}
