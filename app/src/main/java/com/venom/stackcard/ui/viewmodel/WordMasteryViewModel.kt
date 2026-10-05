package com.venom.stackcard.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venom.analytics.AnalyticsEvent
import com.venom.analytics.AnalyticsManager
import com.venom.analytics.AnalyticsParam
import com.venom.analytics.CrashlyticsManager
import com.venom.analytics.ext.logFlashcardSwipe
import com.venom.data.local.PreferencesKeys
import com.venom.data.mock.MockWordData
import com.venom.data.repo.UserActivityRepository
import com.venom.data.repo.UserWordProgressRepository
import com.venom.domain.model.CefrLevel
import com.venom.domain.model.DashboardData
import com.venom.domain.model.KnownState
import com.venom.domain.model.LanguageOption
import com.venom.domain.model.QuizInput
import com.venom.domain.model.QuizResult
import com.venom.domain.model.UserLevel
import com.venom.domain.model.UserWordProgress
import com.venom.domain.model.WordMaster
import com.venom.domain.model.getLanguageOptions
import com.venom.domain.repo.IUserIdentityRepository
import com.venom.domain.repo.IWordMasterRepository
import com.venom.stackcard.session.SessionPlanner
import com.venom.stackcard.session.WordSessionEngine
import com.venom.stackcard.ui.components.insights.InsightsTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

@Immutable
data class CurrentWordProgress(
    val repetitions: Int = 0,
    val nextReviewBucket: NextReviewBucket = NextReviewBucket.NOW,
    val nextReviewCount: Int = 0,
    val masteryProgress: Float = 0f,
    val isBookmarked: Boolean = false,
    val knownState: KnownState = KnownState.SEEN
)

/**
 * UI buckets for the next-review display. The view model classifies; the UI localizes
 * from strings.xml — no English text crosses the state boundary.
 */
enum class NextReviewBucket { NOW, SOON, TODAY, TOMORROW, IN_DAYS, IN_WEEKS, IN_MONTHS }

@Immutable
data class SessionStats(
    val totalCards: Int = 0,
    val masteredCount: Int = 0,
    val learningCount: Int = 0,
    val needsReviewCount: Int = 0,
    val currentStreak: Int = 0,
    val todayWordsViewed: Int = 0,
    val todayXpEarned: Int = 0,
    val totalXp: Int = 0,
    val levelProgress: Float = 0f,
    val xpToNextLevel: Int = 0,
    val totalSessionCount: Int = 0,
    val totalTimeMs: Long = 0,
    val totalDaysActive: Int = 0,
    val bestStreak: Int = 0,
    val totalWordsLearned: Int = 0,
    val totalWordsMastered: Int = 0,
    val sessionDurationMs: Long = 0
)

@Immutable
data class WordMasteryUiState(
    val visibleCards: List<WordMaster> = emptyList(),
    val removedCards: List<WordMaster> = emptyList(),
    val processedCardsCount: Int = 0,
    val currentWord: WordMaster? = null,
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val isHintRevealed: Boolean = false,
    val isBookmarked: Boolean = false,
    val flipRotation: Float = 0f,
    val isPracticeMode: Boolean = false,
    val isSheetOpen: Boolean = false,
    val activeTab: InsightsTab = InsightsTab.OVERVIEW,
    val pinnedLanguage: LanguageOption? = null,
    val showPowerTip: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isGenerativeMode: Boolean = false,
    val isSignedIn: Boolean = true,
    val userName: String? = null,
    val userProgress: DashboardData? = null,
    val userCefrLevel: CefrLevel = CefrLevel.B1,
    val currentWordProgress: CurrentWordProgress = CurrentWordProgress(),
    val sessionStats: SessionStats = SessionStats(),
    val initialCardCount: Int = 0,
    val nextWordProgressCache: CurrentWordProgress? = null,
    val quizInput: QuizInput? = null,
    val cardChangeCounter: Int = 0,
    val showPlacementModal: Boolean = false,
    val graduatedCards: Int = 0
)

sealed class WordMasteryEvent {
    object DismissPlacementModal : WordMasteryEvent()
    object FlipCard : WordMasteryEvent()
    object OpenSheet : WordMasteryEvent()
    object CloseSheet : WordMasteryEvent()
    data class ChangeTab(val tab: InsightsTab) : WordMasteryEvent()
    object ToggleBookmark : WordMasteryEvent()
    data class PinLanguage(val language: LanguageOption) : WordMasteryEvent()
    object RevealHint : WordMasteryEvent()
    object TogglePowerTip : WordMasteryEvent()
    object StartPractice : WordMasteryEvent()
    object PracticeHandled : WordMasteryEvent()
    data class PracticeResult(val result: QuizResult) : WordMasteryEvent()
    data class PracticeSkipped(val wordId: Int) : WordMasteryEvent()
    data class SwipeCard(val word: WordMaster, val isRemembered: Boolean) : WordMasteryEvent()
    object RegenerateWords : WordMasteryEvent()
    object BackToWelcome : WordMasteryEvent()
    data class Initialize(val isGenerative: Boolean, val topic: String? = null) : WordMasteryEvent()
}

private sealed class DbOperation {
    data class RecordRecallFail(val userId: String, val wordId: Int, val idempotencyKey: String, val sessionId: String) : DbOperation()
    data class RecordRecallSuccess(val userId: String, val wordId: Int, val idempotencyKey: String, val sessionId: String) : DbOperation()
    data class RecordView(val userId: String, val wordId: Int) : DbOperation()
    data class ToggleBookmark(val userId: String, val wordId: Int, val bookmarked: Boolean) :
        DbOperation()
}

@HiltViewModel
class WordMasteryViewModel @Inject constructor(
    private val progressRepository: UserWordProgressRepository,
    private val identityRepository: IUserIdentityRepository,
    private val activityRepository: UserActivityRepository,
    private val wordMasterRepository: IWordMasterRepository,
    private val analyticsManager: AnalyticsManager,
    private val crashlyticsManager: CrashlyticsManager,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _uiState = MutableStateFlow(WordMasteryUiState())
    val uiState: StateFlow<WordMasteryUiState> = _uiState.asStateFlow()

    private val loadDataJob = AtomicReference<Job?>(null)
    private val postLoginSyncJob = AtomicReference<Job?>(null)

    private val sessionMasteredCount = AtomicInteger(0)
    private val sessionLearningCount = AtomicInteger(0)

    // Step 3: session decisions (tap keys, deck order, needs-review counter) live
    // in the pure engine; the view model only executes them. sessionResults was
    // write-only dead state and is intentionally not carried over.
    private var sessionEngine: WordSessionEngine? = null

    private val dbOperationChannel = Channel<DbOperation>(Channel.BUFFERED)
    private val wordProgressCache = ConcurrentHashMap<Int, CurrentWordProgress>()
    private val dbSupervisorJob = SupervisorJob()

    init {
        initializeViewModel()
    }

    private fun initializeViewModel() {
        loadUserProgress()

        viewModelScope.launch {
            var previouslySignedIn = identityRepository.isSignedIn()

            identityRepository.authState.collect { authState ->
                val wasSignedIn = previouslySignedIn
                previouslySignedIn = authState.isSignedIn

                _uiState.update {
                    it.copy(
                        isSignedIn = authState.isSignedIn,
                        userName = authState.userName?.takeIf { name -> name.isNotBlank() }
                    )
                }

                if (!wasSignedIn && authState.isSignedIn) {
                    onPostLoginSync()
                }
            }
        }

        startDbOperationProcessor()
    }

    private fun onPostLoginSync() {
        postLoginSyncJob.getAndSet(
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val userId = identityRepository.getCurrentUserId()
                    crashlyticsManager.logBreadcrumb("Post-login sync started for: $userId")

                    reloadUserProgress(userId)
                    wordProgressCache.clear()

                    _uiState.value.currentWord?.let { word ->
                        prefetchWordProgress(word.id)
                    }

                    crashlyticsManager.logBreadcrumb("Post-login sync completed")
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        crashlyticsManager.logNonFatalException(e, "Post-login sync failed")
                    }
                }
            }
        )?.cancel()
    }

    private suspend fun reloadUserProgress(userId: String) {
        try {
            val dashboard = activityRepository.getDashboardData(userId)

            _uiState.update {
                it.copy(
                    userProgress = dashboard,
                    userCefrLevel = dashboard.level.toCefrLevel()
                )
            }
        } catch (e: Exception) {
            if (e !is CancellationException) {
                crashlyticsManager.logNonFatalException(e, "Failed to reload user progress")
            }
        }
    }

    private fun startDbOperationProcessor() {
        viewModelScope.launch(dbSupervisorJob + Dispatchers.IO) {
            dbOperationChannel.receiveAsFlow().collect { operation ->
                try {
                    when (operation) {
                        is DbOperation.RecordRecallFail -> {
                            progressRepository.recordRecallFail(
                                operation.userId,
                                operation.wordId,
                                operation.idempotencyKey,
                                operation.sessionId
                            )
                        }

                        is DbOperation.RecordRecallSuccess -> {
                            progressRepository.recordRecallSuccess(
                                operation.userId,
                                operation.wordId,
                                operation.idempotencyKey,
                                operation.sessionId
                            )

                            val progress = progressRepository.getDisplayProgress(
                                operation.userId,
                                operation.wordId
                            )
                            when (progress.knownState) {
                                KnownState.MASTERED, KnownState.KNOWN -> {
                                    sessionMasteredCount.incrementAndGet()
                                }

                                KnownState.LEARNING -> {
                                    sessionLearningCount.incrementAndGet()
                                }

                                else -> {}
                            }
                            updateSessionStatsInUI()
                        }

                        is DbOperation.RecordView -> {
                            progressRepository.recordView(operation.userId, operation.wordId)
                        }

                        is DbOperation.ToggleBookmark -> {
                            progressRepository.toggleBookmark(
                                operation.userId,
                                operation.wordId,
                                operation.bookmarked
                            )
                        }
                    }
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        crashlyticsManager.logNonFatalException(
                            e,
                            "DB operation failed: $operation"
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        logSessionCompletion()

        loadDataJob.get()?.cancel()
        postLoginSyncJob.get()?.cancel()
        dbSupervisorJob.cancel()
        dbOperationChannel.close()
        wordProgressCache.clear()
    }

    fun onEvent(event: WordMasteryEvent) {
        when (event) {
            is WordMasteryEvent.DismissPlacementModal -> {
                viewModelScope.launch {
                    dataStore.edit { it[PreferencesKeys.PLACEMENT_COMPLETED] = true }
                    _uiState.update { it.copy(showPlacementModal = false) }
                }
            }
            is WordMasteryEvent.FlipCard -> {
                _uiState.update {
                    it.copy(
                        isFlipped = !it.isFlipped,
                        flipRotation = if (it.isFlipped) 0f else 180f
                    )
                }
            }
            is WordMasteryEvent.OpenSheet -> openSheet()
            is WordMasteryEvent.CloseSheet -> closeSheet()
            is WordMasteryEvent.ChangeTab -> changeTab(event.tab)
            is WordMasteryEvent.ToggleBookmark -> toggleBookmark()
            is WordMasteryEvent.PinLanguage -> pinLanguage(event.language)
            is WordMasteryEvent.RevealHint -> revealHint()
            is WordMasteryEvent.TogglePowerTip -> togglePowerTip()
            is WordMasteryEvent.StartPractice -> startSpellPractice()
            is WordMasteryEvent.PracticeHandled -> practiceHandled()
            is WordMasteryEvent.PracticeResult -> handlePracticeResult(event.result)
            is WordMasteryEvent.PracticeSkipped -> handlePracticeSkipped(event.wordId)
            is WordMasteryEvent.SwipeCard -> handleSwipeCard(event.word, event.isRemembered)
            is WordMasteryEvent.RegenerateWords -> loadData(isGenerative = true)
            is WordMasteryEvent.BackToWelcome -> resetToWelcome()
            is WordMasteryEvent.Initialize -> loadData(event.isGenerative, event.topic)
        }
    }

    private fun openSheet() {
        _uiState.update { it.copy(isSheetOpen = true, activeTab = InsightsTab.OVERVIEW) }
    }

    private fun closeSheet() {
        _uiState.update { it.copy(isSheetOpen = false) }
    }

    private fun changeTab(tab: InsightsTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    private fun revealHint() {
        _uiState.update { it.copy(isHintRevealed = true) }
    }

    private fun togglePowerTip() {
        _uiState.update { it.copy(showPowerTip = !it.showPowerTip) }
    }

    /*
    private fun startQuizPractice() {
        val state = _uiState.value
        val word = state.currentWord ?: return
        if (state.isPracticeMode && state.quizInput?.wordId == word.id) return
        if (state.processedCardsCount < 5) return

        // Generate distractors from session words (same CEFR, different word)
        val distractors = state.visibleCards
            .plus(state.removedCards)
            .filter { it.id != word.id && it.cefrLevel == word.cefrLevel }
            .map { it.arabicAr }
            .distinct()
            .filter { it != word.arabicAr }
            .shuffled()
            .take(3)

        val quizInput = word.toQuizInput(
            distractors = distractors,
            cardPosition = state.processedCardsCount
        )

        _uiState.update { it.copy(isPracticeMode = true, quizInput = quizInput) }
    }
     */

    private fun startSpellPractice() {
        _uiState.update { it.copy(isPracticeMode = true) }
    }
    private fun practiceHandled() {
        _uiState.update { it.copy(isPracticeMode = false, quizInput = null) }
    }

    private fun handlePracticeResult(result: QuizResult) {
        _uiState.update { it.copy(isPracticeMode = false, quizInput = null) }

        viewModelScope.launch(Dispatchers.IO) {
            val userId = identityRepository.getCurrentUserId()
            val engine = sessionEngine
            if (result.isCorrect) {
                progressRepository.recordProductionSuccess(
                    userId,
                    result.wordId,
                    engine?.tapProductionKey(result.wordId, true) ?: "orphan:${result.wordId}:GOOD",
                    engine?.sessionId ?: "orphan"
                )
            } else {
                progressRepository.recordProductionFail(
                    userId,
                    result.wordId,
                    engine?.tapProductionKey(result.wordId, false) ?: "orphan:${result.wordId}:AGAIN",
                    engine?.sessionId ?: "orphan"
                )
            }
            // Refresh word progress cache
            wordProgressCache.remove(result.wordId)
            prefetchWordProgress(result.wordId)
        }
    }

    private fun handlePracticeSkipped(wordId: Int) {
        _uiState.update { it.copy(isPracticeMode = false, quizInput = null) }
    }

    private fun pinLanguage(language: LanguageOption?) {
        _uiState.update { state ->
            val newPinned = if (state.pinnedLanguage?.langName == language?.langName) {
                null
            } else {
                language ?: state.pinnedLanguage
            }
            state.copy(pinnedLanguage = newPinned)
        }
    }

    private fun handleSwipeCard(word: WordMaster, isRemembered: Boolean) {
        if (isRemembered) {
            analyticsManager.logFlashcardSwipe(wordId = word.wordEn, direction = "right", screenName = "FlashcardScreen")
            crashlyticsManager.logBreadcrumb("Swiped remember: ${word.wordEn}")

            val decision = sessionEngine!!.swipeRemembered(word, nowMs = System.currentTimeMillis())

            viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
                val userId = identityRepository.getCurrentUserId()
                dbOperationChannel.send(
                    DbOperation.RecordRecallSuccess(
                        userId, decision.recall.wordId, decision.recall.idempotencyKey,
                        decision.recall.sessionId
                    )
                )
            }

            val isSessionFinished = decision.sessionFinished

            wordProgressCache.remove(word.id)

            _uiState.update { state ->
                state.copy(
                    visibleCards = decision.visibleCards,
                    removedCards = decision.removedCards,
                    currentWord = decision.currentWord,
                    processedCardsCount = decision.processedCardsCount,
                    isFlipped = false,
                    isHintRevealed = false,
                    showPowerTip = false,
                    isSheetOpen = false,
                    flipRotation = 0f,
                    sessionStats = state.sessionStats.copy(
                        sessionDurationMs = if (isSessionFinished) {
                            sessionEngine!!.durationMs(System.currentTimeMillis())
                        } else state.sessionStats.sessionDurationMs
                    ),
                    cardChangeCounter = state.cardChangeCounter + 1
                )
            }

            prepareNextWordProgress(decision.currentWord, decision.visibleCards)
        } else {
            analyticsManager.logFlashcardSwipe(wordId = word.wordEn, direction = "left", screenName = "FlashcardScreen")
            crashlyticsManager.logBreadcrumb("Swiped forgot: ${word.wordEn}")
            
            val decision = sessionEngine!!.swipeForgot(word, nowMs = System.currentTimeMillis())

            updateSessionStatsInUI()

            // Durable at tap time: a forgotten tap is a stored failure even if the
            // session is abandoned before finalization.
            viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
                val userId = identityRepository.getCurrentUserId()
                dbOperationChannel.send(
                    DbOperation.RecordRecallFail(
                        userId, decision.recall.wordId, decision.recall.idempotencyKey,
                        decision.recall.sessionId
                    )
                )
            }

            _uiState.update { state ->
                state.copy(
                    visibleCards = decision.visibleCards,
                    currentWord = decision.currentWord,
                    isFlipped = false,
                    isHintRevealed = false,
                    showPowerTip = false,
                    isSheetOpen = false,
                    flipRotation = 0f,
                    cardChangeCounter = state.cardChangeCounter + 1
                )
            }

            prepareNextWordProgress(decision.currentWord, decision.visibleCards)
        }
    }

    private fun prepareNextWordProgress(nextWord: WordMaster?, remainingCards: List<WordMaster>) {
        if (nextWord != null) {
            val cachedProgress = wordProgressCache[nextWord.id]
            if (cachedProgress == null) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val freshProgress = loadWordProgressDirect(nextWord.id)
                        wordProgressCache[nextWord.id] = freshProgress
                        if (_uiState.value.currentWord?.id == nextWord.id) {
                            _uiState.update { state ->
                                state.copy(
                                    isBookmarked = freshProgress.isBookmarked,
                                    currentWordProgress = freshProgress
                                )
                            }
                        }
                    } catch (e: Exception) {
                        if (e !is CancellationException) {
                            crashlyticsManager.logNonFatalException(e, "Failed to load word progress")
                        }
                    }
                }
            } else {
                 _uiState.update { state ->
                    state.copy(
                        isBookmarked = cachedProgress.isBookmarked,
                        currentWordProgress = cachedProgress
                    )
                 }
            }

            remainingCards.getOrNull(1)?.let { prefetchWordProgress(it.id) }
            
        } else {
            _uiState.update { state ->
                state.copy(
                    sessionStats = state.sessionStats.copy(
                        sessionDurationMs = sessionEngine?.durationMs(System.currentTimeMillis()) ?: 0L
                    )
                )
            }
            finalizeSession()
        }
    }

    private fun updateSessionStats(
        currentStats: SessionStats,
        isSessionFinished: Boolean
    ): SessionStats {
        return if (isSessionFinished) {
            currentStats.copy(
                masteredCount = sessionMasteredCount.get(),
                learningCount = sessionLearningCount.get(),
                needsReviewCount = sessionEngine?.needsReviewCount ?: 0,
                sessionDurationMs = sessionEngine?.durationMs(System.currentTimeMillis()) ?: 0L
            )
        } else {
            currentStats.copy(
                masteredCount = sessionMasteredCount.get(),
                learningCount = sessionLearningCount.get(),
                needsReviewCount = sessionEngine?.needsReviewCount ?: 0
            )
        }
    }

    private fun finalizeSession() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userId = identityRepository.getCurrentUserId()

                // Recall outcomes are already persisted exactly once at tap time
                // (durable channel sends with idempotency keys). Finalization only
                // reconciles session-level bookkeeping, never re-records words.

                val duration = sessionEngine?.durationMs(System.currentTimeMillis()) ?: 0L
                activityRepository.recordSessionComplete(userId, duration)

                reloadUserProgress(userId)

                val dashboard = activityRepository.getDashboardData(userId)
                _uiState.update { state ->
                    state.copy(
                        sessionStats = state.sessionStats.copy(
                            todayXpEarned = dashboard.todayXp,
                            totalXp = dashboard.totalXp,
                            levelProgress = dashboard.levelProgress,
                            xpToNextLevel = dashboard.xpToNextLevel,
                            totalSessionCount = dashboard.totalSessionCount,
                            totalTimeMs = dashboard.totalTimeMs,
                            totalDaysActive = dashboard.totalDaysActive,
                            bestStreak = dashboard.bestStreak,
                            currentStreak = dashboard.currentStreak,
                            totalWordsLearned = dashboard.totalWordsLearned,
                            totalWordsMastered = dashboard.totalWordsMastered
                        )
                    )
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    crashlyticsManager.logNonFatalException(e, "Failed to finalize session")
                }
            }
        }
    }

    fun prefetchWordProgress(wordId: Int) {
        if (wordProgressCache.containsKey(wordId)) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userId = identityRepository.getCurrentUserId()
                val progress = progressRepository.getDisplayProgress(userId, wordId)
                val wordProgress = progress.toCurrentWordProgress()
                wordProgressCache[wordId] = wordProgress

                if (_uiState.value.currentWord?.id == wordId) {
                    _uiState.update { state ->
                        state.copy(
                            isBookmarked = progress.bookmarked,
                            currentWordProgress = wordProgress
                        )
                    }
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    crashlyticsManager.logNonFatalException(e, "Failed to prefetch word progress")
                }
            }
        }
    }

    private fun updateSessionStatsInUI() {
        _uiState.update { state ->
            state.copy(
                sessionStats = state.sessionStats.copy(
                    masteredCount = sessionMasteredCount.get(),
                    learningCount = sessionLearningCount.get(),
                    needsReviewCount = sessionEngine?.needsReviewCount ?: 0
                )
            )
        }
    }

    private fun loadData(isGenerative: Boolean, topic: String? = null) {
        loadDataJob.getAndSet(
            viewModelScope.launch {
                loadDataInternal(isGenerative, topic)
            }
        )?.cancel()
    }

    private suspend fun loadDataInternal(isGenerative: Boolean, topic: String?) {
        sessionEngine = WordSessionEngine(sessionStartMs = System.currentTimeMillis())
        sessionMasteredCount.set(0)
        sessionLearningCount.set(0)
        wordProgressCache.clear()

        crashlyticsManager.setCurrentScreen("FlashcardScreen")
        crashlyticsManager.setCurrentFeature("WordMastery")

        _uiState.update {
            it.copy(
                isLoading = true,
                isGenerativeMode = isGenerative,
                error = null
            )
        }

        try {
            val userId = identityRepository.getCurrentUserId()
            // Reconcile crashed sessions first: project any durable event that never applied.
            progressRepository.replayUnappliedEvents(userId)
            val sessionStats = loadSessionStats(userId)

            val words = if (isGenerative) {
                loadGenerativeWords(topic)
            } else {
                loadSrsWords(userId)
            }

            if (words.isNotEmpty()) {
                initializeWithWords(words, sessionStats, isGenerative, topic)
            } else {
                loadFallbackData(
                    Exception("No words available"),
                    topic,
                    "empty_result",
                    sessionStats
                )
            }
        } catch (e: Exception) {
            if (e !is CancellationException) {
                loadFallbackData(e, topic, "exception_fallback", SessionStats())
            }
        }
    }

    private suspend fun loadGenerativeWords(topic: String?): List<WordMaster> {
        return withContext(Dispatchers.IO) {
            wordMasterRepository.getRandomWords(15)
        }
    }

    private suspend fun loadSrsWords(userId: String): List<WordMaster> {
        return withContext(Dispatchers.IO) {
            val limit = SessionPlanner.SESSION_LIMIT
            val due = progressRepository.getWordsDueForReview(userId, limit)
            val newNeeded = limit - due.size

            val newWords = if (newNeeded > 0) {
                progressRepository.getNewWordsForUser(
                    userId, newNeeded,
                    preferredCefr = activityRepository.getPlacementCefr(userId)
                )
            } else {
                emptyList()
            }

            SessionPlanner.plan(due, newWords, limit)
        }
    }

    private suspend fun initializeWithWords(
        words: List<WordMaster>,
        baseSessionStats: SessionStats,
        isGenerative: Boolean,
        topic: String?
    ) {
        val firstWord = words.firstOrNull()

        // Swipe decisions are read from the engine's deck — it must mirror the
        // visible list or a multi-card session loses its remaining cards.
        sessionEngine?.start(words)

        val wordProgress = firstWord?.let {
            loadWordProgressDirect(it.id)
        } ?: CurrentWordProgress()

        firstWord?.let { wordProgressCache[it.id] = wordProgress }

        val defaultPinnedLanguage = firstWord?.let {
            getLanguageOptions(it).find { lang -> lang.langName == "Spanish" }
        }

        _uiState.update {
            it.copy(
                visibleCards = words,
                currentWord = firstWord,
                pinnedLanguage = defaultPinnedLanguage,
                isLoading = false,
                flipRotation = 0f,
                initialCardCount = words.size,
                currentWordProgress = wordProgress,
                isBookmarked = wordProgress.isBookmarked,
                sessionStats = baseSessionStats.copy(totalCards = words.size)
            )
        }

        //startPractice()
        words.drop(1).take(2).forEach { word ->
            prefetchWordProgress(word.id)
        }

        firstWord?.let { word ->
            viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
                val userId = identityRepository.getCurrentUserId()
                dbOperationChannel.send(DbOperation.RecordView(userId, word.id))
            }
        }

        val mode = if (isGenerative) "generative" else "srs_mixed"
        logSessionStarted(mode, words.size, topic)
    }

    private suspend fun loadWordProgressDirect(wordId: Int): CurrentWordProgress {
        return withContext(Dispatchers.IO) {
            try {
                val userId = identityRepository.getCurrentUserId()
                val progress = progressRepository.getDisplayProgress(userId, wordId)
                progress.toCurrentWordProgress()
            } catch (_: Exception) {
                CurrentWordProgress()
            }
        }
    }

    private suspend fun loadSessionStats(userId: String): SessionStats {
        return withContext(Dispatchers.IO) {
            try {
                val dashboard = activityRepository.getDashboardData(userId)
                SessionStats(
                    totalCards = 0,
                    masteredCount = 0,
                    learningCount = 0,
                    needsReviewCount = 0,
                    currentStreak = dashboard.currentStreak,
                    todayWordsViewed = dashboard.dailyGoalProgress,
                    todayXpEarned = dashboard.todayXp,
                    totalXp = dashboard.totalXp,
                    levelProgress = dashboard.levelProgress,
                    xpToNextLevel = dashboard.xpToNextLevel,
                    totalSessionCount = dashboard.totalSessionCount,
                    totalTimeMs = dashboard.totalTimeMs,
                    totalDaysActive = dashboard.totalDaysActive,
                    bestStreak = dashboard.bestStreak,
                    totalWordsLearned = dashboard.totalWordsLearned,
                    totalWordsMastered = dashboard.totalWordsMastered,
                    sessionDurationMs = 0
                )
            } catch (e: Exception) {
                crashlyticsManager.logNonFatalException(e, "Failed to load session stats")
                SessionStats()
            }
        }
    }

    private fun loadUserProgress() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userId = identityRepository.getCurrentUserId()
                reloadUserProgress(userId)

                val placementCompleted = dataStore.data
                    .map { it[PreferencesKeys.PLACEMENT_COMPLETED] ?: false }
                    .firstOrNull() ?: false

                _uiState.update { it.copy(showPlacementModal = !placementCompleted) }
            } catch (_: Exception) {
            }
        }
    }

    private fun loadFallbackData(
        error: Throwable,
        topic: String?,
        mode: String,
        baseSessionStats: SessionStats
    ) {
        // Demo-gate: mock words ship in DEBUG only. Release builds with no seeded
        // content surface an error instead of teaching fake vocabulary.
        val fallbackWords = if (com.venom.wordvibe.BuildConfig.DEBUG) {
            MockWordData.mockWordList.shuffled().take(15)
        } else {
            emptyList()
        }
        val defaultPinnedLanguage = fallbackWords.firstOrNull()?.let {
            getLanguageOptions(it).find { lang -> lang.langName == "Spanish" }
        }

        // Same engine-mirrors-visible-list rule as the main load path.
        sessionEngine?.start(fallbackWords)

        _uiState.update {
            it.copy(
                visibleCards = fallbackWords,
                currentWord = fallbackWords.firstOrNull(),
                pinnedLanguage = defaultPinnedLanguage,
                isLoading = false,
                flipRotation = 0f,
                error = if (fallbackWords.isEmpty()) {
                    error.message ?: "No words available"
                } else {
                    null
                },
                initialCardCount = fallbackWords.size,
                sessionStats = baseSessionStats.copy(totalCards = fallbackWords.size)
            )
        }

        //startPractice()

        analyticsManager.logError(
            errorType = "word_load",
            errorMessage = error.message ?: "Unknown error",
            screenName = "FlashcardScreen"
        )
        crashlyticsManager.logNonFatalException(error, "Word load failed - using fallback data")
        logSessionStarted(mode, fallbackWords.size, topic, error.message)
    }

    private fun resetToWelcome() {
        loadDataJob.get()?.cancel()
        postLoginSyncJob.get()?.cancel()

        sessionMasteredCount.set(0)
        sessionLearningCount.set(0)
        wordProgressCache.clear()
        sessionEngine = WordSessionEngine(
            sessionStartMs = sessionEngine?.sessionId?.toLongOrNull() ?: System.currentTimeMillis()
        )

        _uiState.update {
            WordMasteryUiState(
                isSignedIn = it.isSignedIn,
                userName = it.userName,
                userProgress = it.userProgress,
                userCefrLevel = it.userCefrLevel,
                isGenerativeMode = it.isGenerativeMode
            )
        }
    }

    private fun toggleBookmark() {
        val currentWord = _uiState.value.currentWord ?: return
        val newStatus = !_uiState.value.isBookmarked

        _uiState.update {
            it.copy(
                isBookmarked = newStatus,
                currentWordProgress = it.currentWordProgress.copy(isBookmarked = newStatus)
            )
        }

        wordProgressCache[currentWord.id]?.let { cached ->
            wordProgressCache[currentWord.id] = cached.copy(isBookmarked = newStatus)
        }

        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val userId = identityRepository.getCurrentUserId()
            dbOperationChannel.send(DbOperation.ToggleBookmark(userId, currentWord.id, newStatus))
        }
    }

    private fun logSessionStarted(
        mode: String,
        cardCount: Int,
        topic: String?,
        fallbackReason: String? = null
    ) {
        val params = mutableMapOf<String, Any>(
            "mode" to mode,
            "card_count" to cardCount
        )
        topic?.let { params["topic"] = it }
        fallbackReason?.let { params["fallback_reason"] = it }

        analyticsManager.logFeatureOpened(
            featureName = "flashcard_session",
            additionalParams = params
        )
    }

    private fun logSessionCompletion() {
        val startTime = sessionEngine?.sessionId?.toLongOrNull() ?: 0L
        val cardsSwiped = sessionEngine?.cardsSwiped ?: 0

        if (startTime > 0 && cardsSwiped > 0) {
            val sessionDuration = System.currentTimeMillis() - startTime

            analyticsManager.logFeatureCompleted(
                featureName = "flashcard_session",
                result = "completed",
                durationMs = sessionDuration
            )
            analyticsManager.logEvent(
                eventName = AnalyticsEvent.FEATURE_COMPLETED,
                params = mapOf<String, Any>(
                    AnalyticsParam.FEATURE_NAME to "flashcard_session",
                    AnalyticsParam.DURATION_MS to sessionDuration,
                    "cards_swiped" to cardsSwiped,
                    "cards_remaining" to _uiState.value.visibleCards.size,
                    "mastered" to sessionMasteredCount.get(),
                    "learning" to sessionLearningCount.get(),
                    "needs_review" to (sessionEngine?.needsReviewCount ?: 0)
                )
            )
        }
    }

    fun refreshProgress() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userId = identityRepository.getCurrentUserId()
                reloadUserProgress(userId)
            } catch (_: Exception) {
            }
        }
    }

    private fun UserWordProgress.toCurrentWordProgress(): CurrentWordProgress {
        val successfulInteractions = recallSuccess + productionSuccess

        val masteryProgress = when {
            totalAttempts == 0 -> 0f
            knownState == KnownState.MASTERED -> 1f
            knownState == KnownState.KNOWN -> 0.75f
            knownState == KnownState.LEARNING -> 0.5f
            knownState == KnownState.SEEN -> 0.25f
            else -> (successfulInteractions.toFloat() / maxOf(totalAttempts, 1))
                .coerceIn(0f, 1f)
        }

        val nextReview = calculateNextReviewBucket(nextReview)

        return CurrentWordProgress(
            repetitions = totalAttempts,
            nextReviewBucket = nextReview.first,
            nextReviewCount = nextReview.second,
            masteryProgress = masteryProgress,
            isBookmarked = bookmarked,
            knownState = knownState
        )
    }

    private fun calculateNextReviewBucket(nextReview: Long?): Pair<NextReviewBucket, Int> {
        if (nextReview == null) return NextReviewBucket.NOW to 0

        val now = System.currentTimeMillis()
        val diff = nextReview - now

        return when {
            diff <= 0 -> NextReviewBucket.NOW to 0
            diff < TimeUnit.HOURS.toMillis(1) -> NextReviewBucket.SOON to 0
            diff < TimeUnit.DAYS.toMillis(1) -> NextReviewBucket.TODAY to 0
            diff < TimeUnit.DAYS.toMillis(2) -> NextReviewBucket.TOMORROW to 0
            diff < TimeUnit.DAYS.toMillis(7) ->
                NextReviewBucket.IN_DAYS to TimeUnit.MILLISECONDS.toDays(diff).toInt()
            diff < TimeUnit.DAYS.toMillis(30) ->
                NextReviewBucket.IN_WEEKS to (TimeUnit.MILLISECONDS.toDays(diff) / 7).toInt()
            else ->
                NextReviewBucket.IN_MONTHS to (TimeUnit.MILLISECONDS.toDays(diff) / 30).toInt()
        }
    }

    /**
     * Maps UserLevel to corresponding CEFR level for performance optimization
     */
    private fun UserLevel.toCefrLevel(): CefrLevel = when (this) {
        UserLevel.NEWCOMER, UserLevel.EXPLORER -> CefrLevel.A1
        UserLevel.LEARNER, UserLevel.APPRENTICE -> CefrLevel.A2
        UserLevel.SCHOLAR, UserLevel.ADEPT -> CefrLevel.B1
        UserLevel.EXPERT, UserLevel.MASTER -> CefrLevel.B2
        UserLevel.CHAMPION, UserLevel.LEGEND -> CefrLevel.C1
        UserLevel.POLYGLOT, UserLevel.SAGE -> CefrLevel.C2
    }
}
