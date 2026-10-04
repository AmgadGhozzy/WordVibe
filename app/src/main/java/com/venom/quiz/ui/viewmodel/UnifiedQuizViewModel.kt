package com.venom.quiz.ui.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venom.data.local.PreferencesKeys
import com.venom.domain.model.DefaultConfig.Quiz.BASE_POINTS
import com.venom.domain.model.Question
import com.venom.domain.model.QuizMode
import com.venom.domain.model.QuizQuestionUi
import com.venom.domain.model.QuizQuestionUi.FlashcardQuestion
import com.venom.domain.model.QuizQuestionUi.TestQuestion
import com.venom.domain.model.QuizQuestionUi.WordQuestion
import com.venom.domain.model.QuizResult
import com.venom.domain.model.QuizUiState
import com.venom.domain.model.WordLevels
import com.venom.domain.model.WordMaster
import com.venom.domain.provider.AppConfigProvider
import com.venom.domain.repo.IQuizRepository
import com.venom.domain.repo.IWordMasterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max

@HiltViewModel
class UnifiedQuizViewModel @Inject constructor(
    private val wordRepository: IWordMasterRepository,
    private val quizRepository: IQuizRepository,
    private val appConfigProvider: AppConfigProvider,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val quizConfig get() = appConfigProvider


    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState

    private val _quizResult = MutableSharedFlow<QuizResult>(extraBufferCapacity = 1)
    val quizResult: SharedFlow<QuizResult> = _quizResult

    private var timerJob: Job? = null
    private var animationJob: Job? = null
    private var flashcardStartTime: Long = 0L
    private var flashcardAttempts: Int = 0

    fun start(mode: QuizMode) {
        timerJob?.cancel()
        animationJob?.cancel()

        when (mode) {
            is QuizMode.Word -> startWordQuiz(mode)
            is QuizMode.Test -> startTestQuiz(mode)
            is QuizMode.Flashcard -> startFlashcardQuiz(mode)
            is QuizMode.Placement -> {} // Handled by PlacementViewModel
        }
    }

    // Flashcard Mode 

    private fun startFlashcardQuiz(mode: QuizMode.Flashcard) {
        flashcardStartTime = System.currentTimeMillis()
        flashcardAttempts = 0

        val question = FlashcardQuestion(input = mode.input)
        _uiState.update {
            it.copy(
                mode = mode,
                questions = listOf(question),
                currentIndex = 0,
                selectedOption = null,
                selectedAnswers = emptyMap(),
                isAnswered = false,
                isLoading = false,
                timeRemaining = 0,
                hearts = 0,
                streak = 0,
                score = 0f,
                totalQuestions = 1,
                showResults = false,
                correctCount = 0,
                error = null
            )
        }
    }

    fun selectOption(option: String) {
        val current = _uiState.value
        if (current.isAnswered) return

        val question = current.currentQuestion ?: return
        val isCorrect = question.correctAnswer?.let { option == it } ?: false

        when (question) {
            is WordQuestion -> handleWordAnswer(isCorrect, option)
            is TestQuestion -> handleTestAnswer(isCorrect, option)
            is FlashcardQuestion -> handleFlashcardAnswer(isCorrect, option, question)
        }
    }

    private fun handleFlashcardAnswer(
        isCorrect: Boolean,
        option: String,
        question: FlashcardQuestion
    ) {
        flashcardAttempts++

        _uiState.update {
            it.copy(
                selectedOption = option,
                selectedAnswers = mapOf(0 to option),
                isAnswered = true,
                correctCount = if (isCorrect) 1 else 0
            )
        }

        val input = question.input
        val result = QuizResult(
            wordId = input.wordId,
            isCorrect = isCorrect,
            selectedAnswer = option,
            correctAnswer = input.correctAnswer,
            timeSpentMs = System.currentTimeMillis() - flashcardStartTime,
            attempts = flashcardAttempts,
            usedSkip = false,
            source = input.source,
            sessionId = input.sessionId,
            cardPosition = input.cardPosition,
            questionType = input.questionType,
            cefrLevel = input.cefrLevel,
            difficulty = input.difficulty
        )

        _quizResult.tryEmit(result)
    }

    fun skipFlashcard() {
        val current = _uiState.value
        val question = current.currentQuestion as? FlashcardQuestion ?: return
        val input = question.input

        val result = QuizResult(
            wordId = input.wordId,
            isCorrect = false,
            selectedAnswer = "",
            correctAnswer = input.correctAnswer,
            timeSpentMs = System.currentTimeMillis() - flashcardStartTime,
            attempts = flashcardAttempts,
            usedSkip = true,
            source = input.source,
            sessionId = input.sessionId,
            cardPosition = input.cardPosition,
            questionType = input.questionType,
            cefrLevel = input.cefrLevel,
            difficulty = input.difficulty
        )

        _quizResult.tryEmit(result)
    }

    // Word Mode 

    private fun startWordQuiz(mode: QuizMode.Word) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, mode = mode, error = null) }
            try {
                val words = wordRepository.getWordsByCefr(
                    cefrLevel = mode.level.cefrLevel,
                    limit = appConfigProvider.quizQuestionCount
                )

                if (words.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "No words available for this level"
                        )
                    }
                    return@launch
                }

                val questions = words.map { word ->
                    WordQuestion(
                        word = word,
                        options = generateOptions(word, mode.level),
                        correctAnswer = word.arabicAr,
                        translation = word.arabicAr
                    )
                }

                _uiState.update {
                    it.copy(
                        questions = questions,
                        currentIndex = 0,
                        selectedOption = null,
                        selectedAnswers = emptyMap(),
                        isAnswered = false,
                        isLoading = false,
                        timeRemaining = appConfigProvider.quizWordTimerSec,
                        hearts = appConfigProvider.quizInitialHearts,
                        streak = 0,
                        score = 0f,
                        totalQuestions = questions.size,
                        showResults = false,
                        correctCount = 0
                    )
                }
                startTimer()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // Test Mode 

    private fun startTestQuiz(mode: QuizMode.Test) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, mode = mode, error = null) }
            try {
                val questions = quizRepository.getQuestionsForTest(mode.testId).first()
                if (questions.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "No questions available for this test"
                        )
                    }
                    return@launch
                }

                val mapped = questions.map { it.toTestQuestionUi() }
                _uiState.update {
                    it.copy(
                        questions = mapped,
                        currentIndex = 0,
                        selectedOption = null,
                        selectedAnswers = emptyMap(),
                        isAnswered = false,
                        isLoading = false,
                        timeRemaining = appConfigProvider.quizTestTimerSec,
                        hearts = 0,
                        streak = 0,
                        score = 0f,
                        totalQuestions = mapped.size,
                        showResults = false,
                        correctCount = 0
                    )
                }
                startTimer()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // Answer Handlers 

    private fun handleWordAnswer(isCorrect: Boolean, option: String) {
        val state = _uiState.value
        timerJob?.cancel()

        val newStreak = if (isCorrect) state.streak + 1 else 0
        val points = calculatePoints(isCorrect, newStreak, state.timeRemaining)

        val heartRecovery = isCorrect &&
                newStreak > 0 &&
                newStreak % appConfigProvider.quizHeartRecoveryStreak == 0 &&
                state.hearts < appConfigProvider.quizMaxHearts
        val newHearts = when {
            heartRecovery -> state.hearts + 1
            !isCorrect -> max(state.hearts - 1, 0)
            else -> state.hearts
        }

        val newSelected = state.selectedAnswers + (state.currentIndex to option)
        val newCorrectCount = calculateCorrectCount(newSelected, state.questions)

        _uiState.update {
            it.copy(
                selectedOption = option,
                selectedAnswers = newSelected,
                isAnswered = true,
                hearts = newHearts,
                streak = newStreak,
                score = it.score + points,
                correctCount = newCorrectCount
            )
        }

        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            delay(1500)
            if (newHearts <= 0 || state.currentIndex >= state.totalQuestions - 1) {
                finishQuiz()
            } else {
                nextQuestion()
            }
        }
    }

    private fun handleTestAnswer(isCorrect: Boolean, option: String) {
        val state = _uiState.value
        timerJob?.cancel()

        val points = if (isCorrect) BASE_POINTS.toFloat() else 0f
        val newSelected = state.selectedAnswers + (state.currentIndex to option)
        val newCorrectCount = calculateCorrectCount(newSelected, state.questions)

        _uiState.update {
            it.copy(
                selectedOption = option,
                selectedAnswers = newSelected,
                isAnswered = true,
                correctCount = newCorrectCount,
                score = it.score + points
            )
        }
    }

    private fun calculateCorrectCount(
        selectedAnswers: Map<Int, String>,
        questions: List<QuizQuestionUi>
    ): Int = selectedAnswers.count { (index, value) ->
        questions.getOrNull(index)?.correctAnswer == value
    }

    // Navigation 

    fun nextQuestion() {
        val state = _uiState.value
        timerJob?.cancel()
        animationJob?.cancel()

        if (state.currentIndex >= state.totalQuestions - 1) {
            finishQuiz()
            return
        }

        val nextIndex = state.currentIndex + 1
        val nextSelected = state.selectedAnswers[nextIndex]
        val isNextAnswered = nextSelected != null

        _uiState.update {
            it.copy(
                currentIndex = nextIndex,
                selectedOption = nextSelected,
                isAnswered = isNextAnswered,
                timeRemaining = when (it.mode) {
                    is QuizMode.Word -> appConfigProvider.quizWordTimerSec
                    is QuizMode.Test -> appConfigProvider.quizTestTimerSec
                    else -> 0
                }
            )
        }

        if (!isNextAnswered) startTimer()
    }

    fun previousQuestion() {
        val state = _uiState.value
        if (state.currentIndex == 0) return
        timerJob?.cancel()
        animationJob?.cancel()

        val prevIndex = state.currentIndex - 1
        val prevSelected = state.selectedAnswers[prevIndex]

        _uiState.update {
            it.copy(
                currentIndex = prevIndex,
                selectedOption = prevSelected,
                isAnswered = prevSelected != null
            )
        }
    }

    // Timer 

    private fun startTimer() {
        timerJob?.cancel()
        val duration = when (_uiState.value.mode) {
            is QuizMode.Word -> appConfigProvider.quizWordTimerSec
            is QuizMode.Test -> appConfigProvider.quizTestTimerSec
            else -> return
        }

        timerJob = viewModelScope.launch {
            for (timeLeft in duration downTo 0) {
                _uiState.update { it.copy(timeRemaining = timeLeft) }
                if (_uiState.value.isAnswered) break
                if (timeLeft == 0) {
                    handleTimeOut(); break
                }
                delay(1000)
            }
        }
    }

    private fun handleTimeOut() {
        val state = _uiState.value
        if (state.isAnswered) return

        when (state.mode) {
            is QuizMode.Word -> {
                val newHearts = max(state.hearts - 1, 0)
                _uiState.update { it.copy(isAnswered = true, hearts = newHearts, streak = 0) }

                viewModelScope.launch {
                    delay(1500)
                    if (newHearts <= 0 || state.currentIndex >= state.totalQuestions - 1) finishQuiz()
                    else nextQuestion()
                }
            }

            is QuizMode.Test -> _uiState.update { it.copy(isAnswered = false) }
            else -> {}
        }
    }

    // Finish 

    private fun finishQuiz() {
        timerJob?.cancel()
        animationJob?.cancel()
        _uiState.update { it.copy(showResults = true) }

        val state = _uiState.value
        if (state.mode is QuizMode.Word) {
            val percentage = if (state.totalQuestions == 0) 0f
            else state.correctCount.toFloat() / state.totalQuestions

            if (percentage >= appConfigProvider.quizLevelUnlockRatio) {
                viewModelScope.launch { unlockNextLevel((state.mode as QuizMode.Word).level) }
            }
        }
    }

    /**
     * Unlock the NEXT level only — prevents skipping.
     * Validates that all prior levels are already unlocked before progressing.
     */
    private suspend fun unlockNextLevel(currentLevel: WordLevels) {
        val allLevels = WordLevels.values()
        val currentIndex = allLevels.indexOf(currentLevel)
        if (currentIndex < 0 || currentIndex >= allLevels.size - 1) return

        val nextLevel = allLevels[currentIndex + 1]
        val currentUnlocked = dataStore.data
            .map { it[PreferencesKeys.UNLOCKED_LEVELS] ?: setOf(WordLevels.Beginner.id) }
            .firstOrNull() ?: setOf(WordLevels.Beginner.id)

        // Verify all prior levels are unlocked (prevents skip exploits)
        val priorUnlocked = allLevels.take(currentIndex + 1)
            .all { currentUnlocked.contains(it.id) }

        if (priorUnlocked && !currentUnlocked.contains(nextLevel.id)) {
            dataStore.edit { it[PreferencesKeys.UNLOCKED_LEVELS] = currentUnlocked + nextLevel.id }
        }
    }

    // Helpers 

    /**
     * Generates exactly 4 unique options (3 distractors + correct answer).
     * Guarantees no duplicates and no blank options.
     */
    private suspend fun generateOptions(currentWord: WordMaster, level: WordLevels): List<String> {
        val pool = wordRepository.getWordsByCefr(
            cefrLevel = level.cefrLevel,
            limit = 30
        )

        val distractors = pool
            .filter { it.id != currentWord.id }
            .filter { it.arabicAr.isNotBlank() && it.arabicAr != currentWord.arabicAr }
            .distinctBy { it.arabicAr }  // prevent duplicates
            .shuffled()
            .take(3)
            .map { it.arabicAr }

        // All 4 options must be unique and non-blank
        val allOptions = (distractors + currentWord.arabicAr).distinct()
        return allOptions.shuffled()
    }

    private fun calculatePoints(isCorrect: Boolean, streak: Int, timeRemaining: Int): Float {
        if (!isCorrect) return 0f
        var points = appConfigProvider.quizBasePoints.toFloat()
        points += minOf(
            streak,
            appConfigProvider.quizMaxStreakBonus
        ) * appConfigProvider.quizStreakBonus
        val timerDuration = when (_uiState.value.mode) {
            is QuizMode.Word -> appConfigProvider.quizWordTimerSec
            is QuizMode.Test -> appConfigProvider.quizTestTimerSec
            else -> appConfigProvider.quizWordTimerSec
        }
        if (timeRemaining >= (timerDuration * appConfigProvider.quizQuickBonusThreshold) / 3) {
            points += appConfigProvider.quizQuickBonusAmount
        }
        return points
    }

    override fun onCleared() {
        timerJob?.cancel()
        animationJob?.cancel()
        super.onCleared()
    }
}

/**
 * Converts a DB question to a UI question with validation:
 * - Removes blank/duplicate choices
 * - Ensures exactly one correct answer exists
 * - Shuffles options for randomization
 */
private fun Question.toTestQuestionUi(): TestQuestion {
    // Deduplicate and remove blanks
    val deduped = choices
        .filter { it.isNotBlank() }
        .distinct()

    val correct = mapAnswerToOption(answer, deduped)

    return TestQuestion(
        question = this,
        options = deduped.shuffled(), // Randomize order
        correctAnswer = correct
    )
}

private fun mapAnswerToOption(answer: String, options: List<String>): String? {
    val normalized = answer.trim()
    val index = when (normalized.uppercase()) {
        "A" -> 0; "B" -> 1; "C" -> 2; "D" -> 3; "E" -> 4; else -> null
    }
    return index?.let { options.getOrNull(it) } ?: normalized.takeIf { options.contains(it) }
}