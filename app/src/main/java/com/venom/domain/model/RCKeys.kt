package com.venom.domain.model

object RCKeys {
    object Quiz {
        const val QUESTION_COUNT = "quiz_question_count"
        const val WORD_TIMER_SEC = "quiz_word_timer_sec"
        const val TEST_TIMER_SEC = "quiz_test_timer_sec"
        const val INITIAL_HEARTS = "quiz_initial_hearts"
        const val MAX_HEARTS = "quiz_max_hearts"
        const val HEART_RECOVERY_STREAK = "quiz_heart_recovery_streak"
        const val LEVEL_UNLOCK_RATIO = "quiz_level_unlock_ratio"
        const val QUICK_BONUS_THRESHOLD = "quiz_quick_bonus_threshold"
        const val BASE_POINTS = "quiz_base_points"
        const val STREAK_BONUS = "quiz_streak_bonus"
        const val MAX_STREAK_BONUS = "quiz_max_streak_bonus"
        const val QUICK_BONUS_AMOUNT = "quiz_quick_bonus_amount"
    }

    object Spelling {
        const val AUTO_ADVANCE_MS = "spelling_auto_advance_ms"
        const val DISTRACTORS = "spelling_distractors"
        const val MAX_STREAK = "spelling_max_streak"
        const val TTS_INITIAL_DELAY = "spelling_tts_initial_delay"
        const val SHAKE_DURATION = "spelling_shake_duration"
    }

    object XP {
        const val QUIZ_BASE = "xp_quiz_base"
        const val WORD_VIEW = "xp_word_view"
        const val SWIPE_REMEMBER = "xp_swipe_remember"
        const val SWIPE_FORGOT = "xp_swipe_forgot"
        const val RECALL_SUCCESS = "xp_recall_success"
        const val RECALL_FAIL = "xp_recall_fail"
        const val PRACTICE_SUCCESS = "xp_practice_success"
        const val WORD_MASTERED = "xp_word_mastered"
        const val DAILY_GOAL_BONUS = "xp_daily_goal_bonus"
        const val FIRST_SESSION_BONUS = "xp_first_session_bonus"
        const val STREAK_MULTIPLIER_STEP = "xp_streak_multiplier_step"
        const val STREAK_MULTIPLIER_MAX = "xp_streak_multiplier_max"
    }

    object Feature {
        const val AI_TRANSLATION_ENABLED = "feature_ai_translation_enabled"
        const val LEADERBOARD_ENABLED = "feature_leaderboard_enabled"
        const val SENTENCE_ENABLED = "feature_sentence_enabled"
        const val ONBOARDING_ENABLED = "feature_onboarding_enabled"
    }

    object Provider {
        const val DEEPSEEK_ENABLED = "provider_deepseek_enabled"
        const val GROQ_ENABLED = "provider_groq_enabled"
        const val GPT_MODEL = "provider_gpt_model"
        const val GEMINI_MODEL = "provider_gemini_model"
        const val GROQ_MODEL = "provider_groq_model"
        const val DEEPSEEK_MODEL = "provider_deepseek_model"
        const val MAX_RETRIES = "provider_max_retries"
        const val RETRY_DELAY_MS = "provider_retry_delay_ms"
    }

    object OCR {
        const val ENABLED = "feature_ocr_enabled"
        const val COMPRESSION_QUALITY = "ocr_compression_quality"
    }

    object Phrase {
        const val ENABLED = "feature_phrase_enabled"
    }

    object Sentence {
        const val ENABLED = "feature_sentence_enabled"
        const val MAX_LIMIT = "sentence_max_limit"
    }

    object Quote {
        const val ENABLED = "feature_quote_enabled"
        const val SEARCH_DEBOUNCE = "quote_search_debounce_ms"
    }

    object Translation {
        const val ENABLED = "feature_translation_enabled"
        const val DEBOUNCE = "translation_debounce_ms"
    }

    object STT {
        const val ENABLED = "feature_stt_enabled"
    }

    object TTS {
        const val ENABLED = "feature_tts_enabled"
        const val DEFAULT_VOICE_EN = "tts_default_voice_en"
        const val DEFAULT_VOICE_AR = "tts_default_voice_ar"
    }

    object Sync {
        const val PERIODIC_INTERVAL_MS = "sync_periodic_interval_ms"
    }

    object Ads {
        const val ENABLED = "feature_ads_enabled"
    }

    object Analytics {
        const val ENABLED = "feature_analytics_enabled"
        const val CRASHLYTICS_ENABLED = "feature_crashlytics_enabled"
    }

    object AppInfo {
        const val EMAIL = "app_email"
        const val GITHUB = "app_github"
        const val LINKEDIN = "app_linkedin"
        const val LICENSE = "app_license"
        const val PRIVACY = "app_privacy"
        const val TERMS = "app_terms"
        const val PLAY_STORE = "app_play_store"
        const val PLAY_STORE_SEARCH = "app_play_store_search"
    }
}
