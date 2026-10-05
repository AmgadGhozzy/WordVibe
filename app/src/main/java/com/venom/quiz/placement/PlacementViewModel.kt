package com.venom.quiz.placement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venom.analytics.CrashlyticsManager
import com.venom.analytics.ext.logApiError
import com.venom.analytics.ext.logUserAction
import com.venom.data.local.dao.UserProfileDao
import com.venom.domain.repo.IUserIdentityRepository
import com.venom.domain.model.LevelScore
import com.venom.domain.model.PlacementLevels
import com.venom.domain.model.PlacementResult
import com.venom.domain.model.buildPlacementResult
import com.venom.domain.model.QuestionType
import com.venom.domain.model.QuizInput
import com.venom.domain.model.QuizMode
import com.venom.domain.model.QuizQuestionUi
import com.venom.domain.model.QuizSource
import com.venom.domain.model.QuizUiState
import com.venom.domain.model.WordLevels
import com.venom.domain.model.WordMaster
import com.venom.domain.repo.IWordMasterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class PlacementViewModel @Inject constructor(
    private val wordRepository: IWordMasterRepository,
    private val userProfileDao: UserProfileDao,
    private val identityRepository: IUserIdentityRepository,
    private val crashlyticsManager: CrashlyticsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        QuizUiState(mode = QuizMode.Placement, isLoading = true)
    )
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private val _result = MutableStateFlow<PlacementResult?>(null)
    val result: StateFlow<PlacementResult?> = _result.asStateFlow()

    private fun getQuestionsForLevel(level: String): Int {
        val units = when (level) {
            "beginner" -> 18
            "elementary" -> 19
            "intermediate" -> 20
            "upper_intermediate" -> 38
            "advanced" -> 37
            "mastery" -> 4
            else -> 10
        }
        return (units + 9) / 10
    }

    private val allLevels = PlacementLevels.levels()

    private companion object {
        /** One target plus three distinct distractors. */
        const val MIN_POOL = 4
    }

    /**
     * ADR-0010: the ladder starts at the lowest level that actually has words in
     * the seeded corpus (floor), so placement never degenerates into a
     * zero-question result on a level with no content.
     */
    private suspend fun buildQuestionSequence(): List<WordLevels> {
        val floorIndex = allLevels.indexOfFirst { level ->
            wordRepository.countWordsByCefr(level.cefrLevel) > 0
        }.coerceAtLeast(0)
        val levels = allLevels.drop(floorIndex)
        return levels.flatMap { level ->
            List(getQuestionsForLevel(level.id)) { level }
        }
    }
    private var questionSequence: List<WordLevels> = emptyList()

    private val levelResults = mutableMapOf<String, Boolean>()
    private val usedWordIds = mutableSetOf<Int>()
    private var currentQuestionIndex = 0
    private var isFinished = false
    private var isProcessing = false
    private val startTime = System.currentTimeMillis()

    fun start() {
        currentQuestionIndex = 0
        isFinished = false
        isProcessing = false
        levelResults.clear()
        usedWordIds.clear()
        _result.value = null
        viewModelScope.launch {
            questionSequence = buildQuestionSequence()
            if (questionSequence.isEmpty()) {
                finalize()
            } else {
                loadQuestionForLevel(questionSequence[currentQuestionIndex])
            }
        }
    }

    fun selectOption(option: String) {
        if (isFinished || isProcessing) return
        val question = _uiState.value.currentQuestion ?: return

        isProcessing = true
        val correct = option == question.correctAnswer
        val level = questionSequence[currentQuestionIndex]

        if (!correct) {
            levelResults[level.id] = false
        } else {
            if (levelResults[level.id] != false) {
                levelResults[level.id] = true
            }
        }

        _uiState.update {
            it.copy(
                selectedOption = option,
                isAnswered = true,
                correctCount = it.correctCount + if (correct) 1 else 0
            )
        }

        viewModelScope.launch {
            delay(1200)
            isProcessing = false
            if (isFinished) return@launch

            if (!correct) {
                // Adaptive: stop at first failed level
                finalize()
                return@launch
            }

            val nextIndex = currentQuestionIndex + 1
            if (nextIndex >= questionSequence.size) {
                finalize()
            } else {
                currentQuestionIndex = nextIndex
                loadQuestionForLevel(questionSequence[nextIndex])
            }
        }
    }

    fun skip() {
        if (isFinished || isProcessing) return
        isFinished = true
        // ADR-0010: abandoning the test is honest — no result, no persistence.
        // The dialog dismiss path is what marks placement as handled for the shell.
        _uiState.update { it.copy(isAbandoned = true, isLoading = false) }
    }

    /**
     * ADR-0010: candidates for this level. A pool too thin for one target plus three
     * distractors borrows from the nearest ladder levels (index distance). The target
     * comes from the level's own words when any exist; borrowed words fill distractors
     * and only back the target when the level has none — the test never ends on zero
     * questions while any ladder level can serve one.
     */
    private suspend fun placementPool(level: WordLevels): Pair<List<WordMaster>, List<WordMaster>> {
        fun usable(words: List<WordMaster>) =
            words.filter { it.definitionEn.isNotBlank() && it.arabicAr.isNotBlank() }

        val own = withContext(Dispatchers.IO) {
            wordRepository.getWordsByCefr(level.cefrLevel, 30)
        }
        if (usable(own).size >= MIN_POOL) return usable(own) to own

        val merged = own.toMutableList()
        for (neighbor in allLevels.filter { it.cefrLevel != level.cefrLevel }
            .sortedBy { kotlin.math.abs(PlacementLevels.indexOf(it) - PlacementLevels.indexOf(level)) }) {
            merged += wordRepository.getWordsByCefr(neighbor.cefrLevel, 30)
            if (usable(merged).size >= MIN_POOL) break
        }
        val usableOwn = usable(own)
        val usableMerged = usable(merged)
        return usableOwn.ifEmpty { usableMerged } to merged
    }

    private suspend fun loadQuestionForLevel(level: WordLevels) {
        _uiState.update { it.copy(isLoading = true) }

        val (targetPool, distractorPool) = placementPool(level)

        val target = targetPool
            .filter { it.id !in usedWordIds }
            .randomOrNull() ?: targetPool.randomOrNull()

        if (target == null) {
            levelResults[level.id] = false
            finalize()
            return
        }

        usedWordIds.add(target.id)

        val type = pickQuestionType(PlacementLevels.indexOf(level))
        val distractors = buildDistractors(target, type, distractorPool)

        if (distractors.isEmpty()) {
            levelResults[level.id] = false
            finalize()
            return
        }

        val (prompt, correct) = when (type) {
            QuestionType.MEANING -> target.wordEn to target.arabicAr
            QuestionType.DEFINITION -> target.wordEn to target.definitionEn
            QuestionType.REVERSE -> target.definitionEn to target.wordEn
        }

        val options = (distractors + correct).shuffled()

        val input = QuizInput(
            wordId = target.id,
            questionType = type,
            prompt = prompt,
            correctAnswer = correct,
            options = options,
            source = QuizSource.PLACEMENT,
            cefrLevel = target.cefrLevel.displayName,
            difficulty = target.difficultyScore
        )
        
        val paddedQuestions = MutableList(currentQuestionIndex + 1) { 
            QuizQuestionUi.FlashcardQuestion(input) 
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                isAnswered = false,
                selectedOption = null,
                questions = paddedQuestions,
                currentIndex = currentQuestionIndex,
                totalQuestions = questionSequence.size,
                placementBand = PlacementLevels.indexOf(level) + 1,
                placementBandLabel = "Level ${PlacementLevels.indexOf(level) + 1} — ${PlacementLevels.cefrFor(level)}"
            )
        }
    }

    private fun pickQuestionType(levelIndex: Int): QuestionType {
        val r = Random.nextDouble()
        return when {
            levelIndex <= 1 -> if (r < 0.80) QuestionType.MEANING else QuestionType.DEFINITION
            levelIndex <= 3 -> when {
                r < 0.50 -> QuestionType.MEANING
                r < 0.80 -> QuestionType.DEFINITION
                else -> QuestionType.REVERSE
            }
            else -> when {
                r < 0.35 -> QuestionType.MEANING
                r < 0.65 -> QuestionType.DEFINITION
                else -> QuestionType.REVERSE
            }
        }
    }

    private fun buildDistractors(
        target: WordMaster,
        type: QuestionType,
        pool: List<WordMaster>
    ): List<String> {
        val mapper: (WordMaster) -> String = when (type) {
            QuestionType.MEANING -> { w -> w.arabicAr }
            QuestionType.DEFINITION -> { w -> w.definitionEn }
            QuestionType.REVERSE -> { w -> w.wordEn }
        }
        val correct = mapper(target)

        return pool
            .filter { it.id != target.id }
            .filter { mapper(it) != correct }
            .filter { mapper(it).isNotBlank() }
            .distinctBy { mapper(it) }
            .shuffled()
            .take(3)
            .map(mapper)
    }

    private fun finalize() {
        if (isFinished) return
        isFinished = true

        // Score counts answered questions only: the current (0-based) index + 1 answered,
        // correctCount questions right. Both sides count questions, never levels.
        val placementResult = buildPlacementResult(
            allLevels = allLevels,
            levelResults = levelResults.toMap(),
            answeredQuestions = currentQuestionIndex + 1,
            totalCorrect = _uiState.value.correctCount,
            durationMs = System.currentTimeMillis() - startTime
        )
        val (assignedLevel, cefr) = placementResult.assignedLevel to placementResult.estimatedCefr
        val assignedIndex = PlacementLevels.indexOf(assignedLevel)

        // levelIndex: 1-based, represents how many levels to unlock
        val levelIndex = assignedIndex + 1

        _result.value = placementResult
        _uiState.update { it.copy(isLoading = false, showResults = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userId = identityRepository.getCurrentUserId()
                val now = System.currentTimeMillis()
                val assignedUserLevel = com.venom.domain.model.UserLevel.baseUserLevelForCefr(assignedLevel.cefrLevel)
                userProfileDao.updatePlacementAndXp(
                    userId = userId, 
                    unit = levelIndex, 
                    cefr = cefr, 
                    assignedXp = assignedUserLevel.minXp,
                    takenAt = now, 
                    now = now
                )
                crashlyticsManager.logUserAction("placement_saved", mapOf("unit" to levelIndex.toString(), "cefr" to cefr).toString())
            } catch (e: Exception) {
                crashlyticsManager.logNonFatalException(e, "Failed to persist placement result")
            }
        }
    }
}
