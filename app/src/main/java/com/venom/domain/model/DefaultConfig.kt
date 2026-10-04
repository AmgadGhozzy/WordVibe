package com.venom.domain.model

object DefaultConfig {
    object Quiz {
        const val QUESTION_COUNT = 10
        const val WORD_TIMER_SEC = 15
        const val TEST_TIMER_SEC = 60
        const val INITIAL_HEARTS = 3
        const val MAX_HEARTS = 5
        const val HEART_RECOVERY_STREAK = 5
        const val LEVEL_UNLOCK_RATIO = 0.7
        const val QUICK_BONUS_THRESHOLD = 2
        const val BASE_POINTS = 10
        const val STREAK_BONUS = 2
        const val MAX_STREAK_BONUS = 5
        const val QUICK_BONUS_AMOUNT = 5
    }

    object Spelling {
        const val AUTO_ADVANCE_MS = 1500L
        const val DISTRACTORS = 3
        const val MAX_STREAK = 3
        const val TTS_INITIAL_DELAY = 500L
        const val SHAKE_DURATION = 500L
    }

    object XP {
        const val QUIZ_BASE = 10
        const val WORD_VIEW = 1
        const val SWIPE_REMEMBER = 3
        const val SWIPE_FORGOT = 1
        const val RECALL_SUCCESS = 5
        const val RECALL_FAIL = 0
        const val PRACTICE_SUCCESS = 10
        const val WORD_MASTERED = 25
        const val DAILY_GOAL_BONUS = 20
        const val FIRST_SESSION_BONUS = 10
        const val STREAK_MULTIPLIER_STEP = 0.05
        const val STREAK_MULTIPLIER_MAX = 2.0
    }

    object Feature {
        const val AI_TRANSLATION_ENABLED = true
        const val LEADERBOARD_ENABLED = true
        const val SENTENCE_ENABLED = true
        const val ONBOARDING_ENABLED = true
    }

    object Provider {
        const val DEEPSEEK_ENABLED = false
        const val GROQ_ENABLED = true
        const val GPT_MODEL = "gpt-3.5-turbo"
        const val GEMINI_MODEL = "gemini-1.5-flash-latest"
        const val GROQ_MODEL = "mixtral-8x7b-32768"
        const val DEEPSEEK_MODEL = "deepseek-chat"
        const val MAX_RETRIES = 3
        const val RETRY_DELAY_MS = 1000L
    }

    object OCR {
        const val ENABLED = true
        const val COMPRESSION_QUALITY = 80L
    }

    object Phrase {
        const val ENABLED = true
    }

    object Sentence {
        const val ENABLED = true
        const val MAX_LIMIT = 20L
    }

    object Quote {
        const val ENABLED = true
        const val SEARCH_DEBOUNCE = 300L
    }

    object Translation {
        const val ENABLED = true
        const val DEBOUNCE = 500L
    }

    object STT {
        const val ENABLED = true
    }

    object TTS {
        const val ENABLED = true
        const val DEFAULT_VOICE_EN = "troy"
        const val DEFAULT_VOICE_AR = "noura"
    }

    object Sync {
        const val PERIODIC_INTERVAL_MS = 60000L
    }

    object Ads {
        const val ENABLED = true
    }

    object Analytics {
        const val ENABLED = true
        const val CRASHLYTICS_ENABLED = true
    }

    object AppInfo {
        const val EMAIL = "AmgadGhozzy@gmail.com"
        const val GITHUB = "https://github.com/AmgadGhozzy"
        const val LINKEDIN = "https://linkedin.com/"
        const val LICENSE = "https://github.com/AmgadGhozzy/LingoLens/blob/master/LICENSE"
        const val PRIVACY = "https://github.com/AmgadGhozzy/LingoLens/blob/master/Privacy.md"
        const val TERMS = "https://github.com/AmgadGhozzy/LingoLens/blob/master/Terms.md"
        const val PLAY_STORE = "https://play.google.com/store/apps/details?id=com.venom.lingolens"
        const val PLAY_STORE_SEARCH = "https://play.google.com/store/search?q=venom&c=apps"
    }
}
