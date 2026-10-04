package com.venom.domain.provider

import com.venom.domain.model.DefaultConfig
import com.venom.domain.model.RCKeys
import com.venom.domain.repo.IRemoteConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Type-safe provider for Remote Config values.
 * Acts as a bridge between the raw config and the domain/UI layers.
 */
@Singleton
class AppConfigProvider @Inject constructor(
    private val remoteConfig: IRemoteConfig
) {

    // Quiz Configs
    val quizQuestionCount: Int
        get() = remoteConfig.getLong(RCKeys.Quiz.QUESTION_COUNT, DefaultConfig.Quiz.QUESTION_COUNT.toLong()).toInt()

    val quizWordTimerSec: Int
        get() = remoteConfig.getLong(RCKeys.Quiz.WORD_TIMER_SEC, DefaultConfig.Quiz.WORD_TIMER_SEC.toLong()).toInt()

    val quizTestTimerSec: Int
        get() = remoteConfig.getLong(RCKeys.Quiz.TEST_TIMER_SEC, DefaultConfig.Quiz.TEST_TIMER_SEC.toLong()).toInt()

    val quizInitialHearts: Int
        get() = remoteConfig.getLong(RCKeys.Quiz.INITIAL_HEARTS, DefaultConfig.Quiz.INITIAL_HEARTS.toLong()).toInt()

    val quizMaxHearts: Int
        get() = remoteConfig.getLong(RCKeys.Quiz.MAX_HEARTS, DefaultConfig.Quiz.MAX_HEARTS.toLong()).toInt()

    val quizHeartRecoveryStreak: Int
        get() = remoteConfig.getLong(RCKeys.Quiz.HEART_RECOVERY_STREAK, DefaultConfig.Quiz.HEART_RECOVERY_STREAK.toLong()).toInt()

    val quizLevelUnlockRatio: Float
        get() = remoteConfig.getDouble(RCKeys.Quiz.LEVEL_UNLOCK_RATIO, DefaultConfig.Quiz.LEVEL_UNLOCK_RATIO).toFloat()

    val quizQuickBonusThreshold: Int
        get() = remoteConfig.getLong(RCKeys.Quiz.QUICK_BONUS_THRESHOLD, DefaultConfig.Quiz.QUICK_BONUS_THRESHOLD.toLong()).toInt()

    val quizBasePoints: Int
        get() = remoteConfig.getLong(RCKeys.Quiz.BASE_POINTS, DefaultConfig.Quiz.BASE_POINTS.toLong()).toInt()

    val quizStreakBonus: Int
        get() = remoteConfig.getLong(RCKeys.Quiz.STREAK_BONUS, DefaultConfig.Quiz.STREAK_BONUS.toLong()).toInt()

    val quizMaxStreakBonus: Int
        get() = remoteConfig.getLong(RCKeys.Quiz.MAX_STREAK_BONUS, DefaultConfig.Quiz.MAX_STREAK_BONUS.toLong()).toInt()

    val quizQuickBonusAmount: Int
        get() = remoteConfig.getLong(RCKeys.Quiz.QUICK_BONUS_AMOUNT, DefaultConfig.Quiz.QUICK_BONUS_AMOUNT.toLong()).toInt()

    // Spelling Configs
    val spellingAutoAdvanceMs: Long
        get() = remoteConfig.getLong(RCKeys.Spelling.AUTO_ADVANCE_MS, DefaultConfig.Spelling.AUTO_ADVANCE_MS)

    val spellingDistractors: Int
        get() = remoteConfig.getLong(RCKeys.Spelling.DISTRACTORS, DefaultConfig.Spelling.DISTRACTORS.toLong()).toInt()
        
    val spellingMaxStreak: Int
        get() = remoteConfig.getLong(RCKeys.Spelling.MAX_STREAK, DefaultConfig.Spelling.MAX_STREAK.toLong()).toInt()

    val spellingTtsInitialDelay: Long
        get() = remoteConfig.getLong(RCKeys.Spelling.TTS_INITIAL_DELAY, DefaultConfig.Spelling.TTS_INITIAL_DELAY)

    val spellingShakeDuration: Long
        get() = remoteConfig.getLong(RCKeys.Spelling.SHAKE_DURATION, DefaultConfig.Spelling.SHAKE_DURATION)

    // XP Configs
    val xpQuizBase: Int
        get() = remoteConfig.getLong(RCKeys.XP.QUIZ_BASE, DefaultConfig.XP.QUIZ_BASE.toLong()).toInt()

    val xpWordView: Int
        get() = remoteConfig.getLong(RCKeys.XP.WORD_VIEW, DefaultConfig.XP.WORD_VIEW.toLong()).toInt()

    val xpSwipeRemember: Int
        get() = remoteConfig.getLong(RCKeys.XP.SWIPE_REMEMBER, DefaultConfig.XP.SWIPE_REMEMBER.toLong()).toInt()

    val xpSwipeForgot: Int
        get() = remoteConfig.getLong(RCKeys.XP.SWIPE_FORGOT, DefaultConfig.XP.SWIPE_FORGOT.toLong()).toInt()

    val xpRecallSuccess: Int
        get() = remoteConfig.getLong(RCKeys.XP.RECALL_SUCCESS, DefaultConfig.XP.RECALL_SUCCESS.toLong()).toInt()

    val xpRecallFail: Int
        get() = remoteConfig.getLong(RCKeys.XP.RECALL_FAIL, DefaultConfig.XP.RECALL_FAIL.toLong()).toInt()

    val xpPracticeSuccess: Int
        get() = remoteConfig.getLong(RCKeys.XP.PRACTICE_SUCCESS, DefaultConfig.XP.PRACTICE_SUCCESS.toLong()).toInt()

    val xpWordMastered: Int
        get() = remoteConfig.getLong(RCKeys.XP.WORD_MASTERED, DefaultConfig.XP.WORD_MASTERED.toLong()).toInt()

    val xpDailyGoalBonus: Int
        get() = remoteConfig.getLong(RCKeys.XP.DAILY_GOAL_BONUS, DefaultConfig.XP.DAILY_GOAL_BONUS.toLong()).toInt()

    val xpFirstSessionBonus: Int
        get() = remoteConfig.getLong(RCKeys.XP.FIRST_SESSION_BONUS, DefaultConfig.XP.FIRST_SESSION_BONUS.toLong()).toInt()

    val xpStreakMultiplierStep: Float
        get() = remoteConfig.getDouble(RCKeys.XP.STREAK_MULTIPLIER_STEP, DefaultConfig.XP.STREAK_MULTIPLIER_STEP).toFloat()

    val xpStreakMultiplierMax: Float
        get() = remoteConfig.getDouble(RCKeys.XP.STREAK_MULTIPLIER_MAX, DefaultConfig.XP.STREAK_MULTIPLIER_MAX).toFloat()

    // Feature Flags
    val isAiTranslationEnabled: Boolean
        get() = remoteConfig.getBoolean(RCKeys.Feature.AI_TRANSLATION_ENABLED, DefaultConfig.Feature.AI_TRANSLATION_ENABLED)

    val isLeaderboardEnabled: Boolean
        get() = remoteConfig.getBoolean(RCKeys.Feature.LEADERBOARD_ENABLED, DefaultConfig.Feature.LEADERBOARD_ENABLED)

    val isSentenceEnabled: Boolean
        get() = remoteConfig.getBoolean(RCKeys.Feature.SENTENCE_ENABLED, DefaultConfig.Feature.SENTENCE_ENABLED)

    val isOnboardingEnabled: Boolean
        get() = remoteConfig.getBoolean(RCKeys.Feature.ONBOARDING_ENABLED, DefaultConfig.Feature.ONBOARDING_ENABLED)

    // Sentence Configs
    val sentenceMaxLimit: Int
        get() = remoteConfig.getLong(RCKeys.Sentence.MAX_LIMIT, DefaultConfig.Sentence.MAX_LIMIT).toInt()

    // Providers
    val providerGptModel: String
        get() = remoteConfig.getString(RCKeys.Provider.GPT_MODEL, DefaultConfig.Provider.GPT_MODEL)

    val providerGeminiModel: String
        get() = remoteConfig.getString(RCKeys.Provider.GEMINI_MODEL, DefaultConfig.Provider.GEMINI_MODEL)

    val providerGroqModel: String
        get() = remoteConfig.getString(RCKeys.Provider.GROQ_MODEL, DefaultConfig.Provider.GROQ_MODEL)

    val providerDeepseekModel: String
        get() = remoteConfig.getString(RCKeys.Provider.DEEPSEEK_MODEL, DefaultConfig.Provider.DEEPSEEK_MODEL)

    val providerMaxRetries: Int
        get() = remoteConfig.getLong(RCKeys.Provider.MAX_RETRIES, DefaultConfig.Provider.MAX_RETRIES.toLong()).toInt()

    val providerRetryDelayMs: Long
        get() = remoteConfig.getLong(RCKeys.Provider.RETRY_DELAY_MS, DefaultConfig.Provider.RETRY_DELAY_MS)

    // Providers
    val isDeepSeekEnabled: Boolean
        get() = remoteConfig.getBoolean(RCKeys.Provider.DEEPSEEK_ENABLED, DefaultConfig.Provider.DEEPSEEK_ENABLED)

    val isGroqEnabled: Boolean
        get() = remoteConfig.getBoolean(RCKeys.Provider.GROQ_ENABLED, DefaultConfig.Provider.GROQ_ENABLED)

    // OCR
    val isOcrEnabled: Boolean
        get() = remoteConfig.getBoolean(RCKeys.OCR.ENABLED, DefaultConfig.OCR.ENABLED)

    val ocrCompressionQuality: Int
        get() = remoteConfig.getLong(RCKeys.OCR.COMPRESSION_QUALITY, DefaultConfig.OCR.COMPRESSION_QUALITY).toInt()

    // Phrase
    val isPhraseEnabled: Boolean
        get() = remoteConfig.getBoolean(RCKeys.Phrase.ENABLED, DefaultConfig.Phrase.ENABLED)

    // Quote
    val isQuoteEnabled: Boolean
        get() = remoteConfig.getBoolean(RCKeys.Quote.ENABLED, DefaultConfig.Quote.ENABLED)

    val quoteSearchDebounceMs: Long
        get() = remoteConfig.getLong(RCKeys.Quote.SEARCH_DEBOUNCE, DefaultConfig.Quote.SEARCH_DEBOUNCE)

    // Translation
    val isTranslationEnabled: Boolean
        get() = remoteConfig.getBoolean(RCKeys.Translation.ENABLED, DefaultConfig.Translation.ENABLED)

    val translationDebounceMs: Long
        get() = remoteConfig.getLong(RCKeys.Translation.DEBOUNCE, DefaultConfig.Translation.DEBOUNCE)

    // STT
    val isSttEnabled: Boolean
        get() = remoteConfig.getBoolean(RCKeys.STT.ENABLED, DefaultConfig.STT.ENABLED)

    // TTS
    val isTtsEnabled: Boolean
        get() = remoteConfig.getBoolean(RCKeys.TTS.ENABLED, DefaultConfig.TTS.ENABLED)

    val ttsDefaultVoiceEn: String
        get() = remoteConfig.getString(RCKeys.TTS.DEFAULT_VOICE_EN, DefaultConfig.TTS.DEFAULT_VOICE_EN)

    val ttsDefaultVoiceAr: String
        get() = remoteConfig.getString(RCKeys.TTS.DEFAULT_VOICE_AR, DefaultConfig.TTS.DEFAULT_VOICE_AR)

    // Sync
    val syncPeriodicIntervalMs: Long
        get() = remoteConfig.getLong(RCKeys.Sync.PERIODIC_INTERVAL_MS, DefaultConfig.Sync.PERIODIC_INTERVAL_MS)

    // Ads
    val isAdsEnabled: Boolean
        get() = remoteConfig.getBoolean(RCKeys.Ads.ENABLED, DefaultConfig.Ads.ENABLED)

    // Analytics
    val isAnalyticsEnabled: Boolean
        get() = remoteConfig.getBoolean(RCKeys.Analytics.ENABLED, DefaultConfig.Analytics.ENABLED)

    val isCrashlyticsEnabled: Boolean
        get() = remoteConfig.getBoolean(RCKeys.Analytics.CRASHLYTICS_ENABLED, DefaultConfig.Analytics.CRASHLYTICS_ENABLED)

    // App Info
    val appEmail: String
        get() = remoteConfig.getString(RCKeys.AppInfo.EMAIL, DefaultConfig.AppInfo.EMAIL)

    val appGithub: String
        get() = remoteConfig.getString(RCKeys.AppInfo.GITHUB, DefaultConfig.AppInfo.GITHUB)

    val appLinkedin: String
        get() = remoteConfig.getString(RCKeys.AppInfo.LINKEDIN, DefaultConfig.AppInfo.LINKEDIN)

    val appLicense: String
        get() = remoteConfig.getString(RCKeys.AppInfo.LICENSE, DefaultConfig.AppInfo.LICENSE)

    val appPrivacy: String
        get() = remoteConfig.getString(RCKeys.AppInfo.PRIVACY, DefaultConfig.AppInfo.PRIVACY)

    val appTerms: String
        get() = remoteConfig.getString(RCKeys.AppInfo.TERMS, DefaultConfig.AppInfo.TERMS)

    val appPlayStore: String
        get() = remoteConfig.getString(RCKeys.AppInfo.PLAY_STORE, DefaultConfig.AppInfo.PLAY_STORE)

    val appPlayStoreSearch: String
        get() = remoteConfig.getString(RCKeys.AppInfo.PLAY_STORE_SEARCH, DefaultConfig.AppInfo.PLAY_STORE_SEARCH)

    /**
     * Initial fetch to ensure values are updated at app startup.
     */
    suspend fun initialize() {
        remoteConfig.fetchAndActivate()
    }
}
