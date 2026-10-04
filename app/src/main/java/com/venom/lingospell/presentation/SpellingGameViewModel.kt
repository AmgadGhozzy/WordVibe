package com.venom.lingospell.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venom.data.repo.UserWordProgressRepository
import com.venom.domain.model.WordMaster
import com.venom.domain.provider.AppConfigProvider
import com.venom.domain.repo.IUserIdentityRepository
import com.venom.domain.spelling.ProductionGrade
import com.venom.domain.spelling.ProductionGrader
import com.venom.lingospell.domain.FeedbackState
import com.venom.lingospell.domain.HintLevel
import com.venom.lingospell.domain.Letter
import com.venom.lingospell.domain.LetterStatus
import com.venom.lingospell.domain.SlotStatus
import com.venom.lingospell.domain.SpellingUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltViewModel
class SpellingGameViewModel @Inject constructor(
    private val progressRepository: UserWordProgressRepository,
    private val identityRepository: IUserIdentityRepository,
    val appConfigProvider: AppConfigProvider
) : ViewModel() {

    private companion object {
        /** Full errors tolerated before the attempt persists as a production failure. */
        const val MAX_MISTAKES_BEFORE_FAIL = 3
        const val SUCCESS_HOLD_MS = 1200L
    }

    private val sessionId: String = UUID.randomUUID().toString()
    private val tapCounters = ConcurrentHashMap<Int, AtomicInteger>()
    private val failedWords: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    /** ADR-0008: at most one counted production success per word per session. */
    private val creditedWords: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    private val _state = MutableStateFlow(SpellingGameState())
    val state: StateFlow<SpellingGameState> = _state.asStateFlow()

    private val _events = Channel<SpellingGameEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        // Typing is the default and the only counted path (ADR-0008).
        _state.update { it.copy(mode = SpellingGameMode.TYPING) }
    }

    fun setCustomWord(wordData: WordMaster) {
        _state.update { state ->
            state.copy(
                overrideWord = wordData,
                streak = 0,
                mistakes = 0,
                hintLevel = HintLevel.NONE,
                feedback = FeedbackState.IDLE,
                typedInput = "",
                mode = SpellingGameMode.TYPING,
                isLoading = true,
                showMasteryDialog = false
            )
        }
        initializeWord(resetStreak = true, wordData = wordData)
    }

    fun clearCustomWord() {
        _state.update { state ->
            state.copy(
                overrideWord = null,
                streak = 0,
                mistakes = 0,
                hintLevel = HintLevel.NONE,
                feedback = FeedbackState.IDLE,
                typedInput = "",
                isLoading = true,
                showMasteryDialog = false
            )
        }
    }

    private fun initializeWord(resetStreak: Boolean, wordData: WordMaster? = null) {
        val currentState = _state.value
        val currentWord = wordData ?: currentState.overrideWord ?: return

        val target = currentWord.wordEn.uppercase()
        val newSlots = SpellingUtils.createSlots(target)
        val newBank = SpellingUtils.generateBank(target, appConfigProvider.spellingDistractors)

        _state.update { state ->
            state.copy(
                overrideWord = currentWord,
                slots = newSlots,
                bank = newBank,
                hintLevel = HintLevel.NONE,
                mistakes = 0,
                typedInput = "",
                feedback = FeedbackState.IDLE,
                streak = if (resetStreak) 0 else state.streak,
                isLoading = false
            )
        }
        // ADR-0008: no recordView/XP here — viewing a prompt is not an attempt.
    }

    /** Dedupe key for exactly-once production commits (Constitution Article II). */
    private fun tapKey(wordId: Int, outcome: String): String =
        "$sessionId:$wordId:$outcome:" +
            tapCounters.getOrPut(wordId) { AtomicInteger(0) }.incrementAndGet()

    /** One production failure per word per spelling visit (P-02). */
    private fun recordProductionFailOnce(wordId: Int) {
        if (wordId <= 0 || !failedWords.add(wordId)) return
        viewModelScope.launch {
            progressRepository.recordProductionFail(
                identityRepository.getCurrentUserId(),
                wordId,
                tapKey(wordId, "PROD_FAIL"),
                sessionId
            )
        }
    }

    // ── Typing mode (the counted path) ──────────────────────────────────────

    fun onTypedInputChange(text: String) {
        if (_state.value.isInputBlocked) return
        _state.update { it.copy(typedInput = text) }
    }

    fun onSubmitTyped() {
        val currentState = _state.value
        if (currentState.isInputBlocked) return
        if (currentState.feedback != FeedbackState.IDLE &&
            currentState.feedback != FeedbackState.NEAR_MISS
        ) return
        val word = currentState.currentWord ?: return

        val grade = ProductionGrader.grade(
            input = currentState.typedInput,
            expected = word.productionExpected ?: word.wordEn,
            variants = word.productionVariants
        )
        when (grade) {
            ProductionGrade.CORRECT -> handleTypingSuccess(word)
            ProductionGrade.NEAR_MISS -> handleNearMiss()
            ProductionGrade.INCORRECT -> handleTypedError(word.id)
        }
    }

    private fun handleTypingSuccess(word: com.venom.domain.model.WordMaster) {
        val firstCredit = creditedWords.add(word.id)
        if (firstCredit) {
            viewModelScope.launch {
                progressRepository.recordProductionSuccess(
                    identityRepository.getCurrentUserId(),
                    word.id,
                    tapKey(word.id, "PROD_SUCCESS"),
                    sessionId
                )
            }
        }
        succeed(newStreak = _state.value.streak + 1)
    }

    private fun handleNearMiss() {
        _state.update { it.copy(feedback = FeedbackState.NEAR_MISS) }
        viewModelScope.launch {
            delay(appConfigProvider.spellingAutoAdvanceMs)
            _state.update { if (it.feedback == FeedbackState.NEAR_MISS) it.copy(feedback = FeedbackState.IDLE) else it }
        }
    }

    private fun handleTypedError(wordId: Int) {
        val newMistakes = _state.value.mistakes + 1
        _state.update { it.copy(mistakes = newMistakes, feedback = FeedbackState.ERROR) }
        if (newMistakes >= MAX_MISTAKES_BEFORE_FAIL) {
            recordProductionFailOnce(wordId)
        }
        viewModelScope.launch {
            delay(appConfigProvider.spellingAutoAdvanceMs)
            _state.update {
                if (it.feedback == FeedbackState.ERROR) it.copy(feedback = FeedbackState.IDLE, typedInput = "")
                else it
            }
        }
    }

    // ── Training mode (letter bank; records nothing) ────────────────────────

    /**
     * Enters training from typing: converting the word to training mode is itself
     * the first hint (ADR-0008 — help of any kind makes the attempt uncounted).
     */
    fun onEnterTraining() {
        val state = _state.value
        if (state.isInputBlocked || state.mode == SpellingGameMode.TRAINING) return
        _state.update { it.copy(mode = SpellingGameMode.TRAINING, typedInput = "") }
        onHintRequest()
    }

    fun onLetterClick(letter: Letter) {
        val currentState = _state.value
        if (currentState.isInputBlocked) return
        if (currentState.mode != SpellingGameMode.TRAINING) return
        if (letter.status != LetterStatus.AVAILABLE) return

        val firstEmptyIndex = currentState.firstEmptySlotIndex
        if (firstEmptyIndex == -1) return

        _state.update { state ->
            val newSlots = state.slots.toMutableList().apply {
                this[firstEmptyIndex] = this[firstEmptyIndex].copy(
                    letter = letter,
                    status = SlotStatus.FILLED
                )
            }
            val newBank = state.bank.map { l ->
                if (l.id == letter.id) l.copy(status = LetterStatus.USED) else l
            }
            state.copy(slots = newSlots, bank = newBank)
        }

        checkValidation()
    }

    fun onSlotClick(index: Int) {
        val currentState = _state.value
        if (currentState.isInputBlocked) return

        val slot = currentState.slots.getOrNull(index) ?: return
        if (slot.letter == null || slot.isLocked) return

        _state.update { state ->
            val newSlots = state.slots.toMutableList().apply {
                this[index] = this[index].copy(letter = null, status = SlotStatus.EMPTY)
            }
            val newBank = state.bank.map { l ->
                if (l.id == slot.letter.id) l.copy(status = LetterStatus.AVAILABLE) else l
            }
            state.copy(slots = newSlots, bank = newBank)
        }
    }

    fun onClearAll() {
        val currentState = _state.value
        if (currentState.isInputBlocked) return

        val lettersToReturnIds = currentState.slots
            .filter { it.letter != null && !it.isLocked }
            .mapNotNull { it.letter?.id }

        _state.update { state ->
            val newSlots = state.slots.map { slot ->
                if (slot.isLocked) slot
                else slot.copy(letter = null, status = SlotStatus.EMPTY)
            }
            val newBank = state.bank.map { l ->
                if (lettersToReturnIds.contains(l.id)) l.copy(status = LetterStatus.AVAILABLE) else l
            }
            state.copy(slots = newSlots, bank = newBank)
        }
    }

    fun onHintRequest() {
        val currentState = _state.value
        if (currentState.mode != SpellingGameMode.TRAINING) return
        if (!currentState.hintLevel.canUseHint) return
        if (currentState.feedback != FeedbackState.IDLE) return

        val word = currentState.currentWord ?: return
        val newLevel = currentState.hintLevel.next()
        val target = word.wordEn.uppercase()

        when (newLevel) {
            HintLevel.LEVEL_1 -> applyHintLevel1(target)
            HintLevel.LEVEL_2 -> applyHintLevel2(target)
            HintLevel.LEVEL_3 -> applyHintLevel3(target)
            HintLevel.NONE -> {}
        }

        _state.update { it.copy(hintLevel = newLevel) }

        if (newLevel == HintLevel.LEVEL_1 || newLevel == HintLevel.LEVEL_2) {
            checkValidation()
        }
    }

    private fun applyHintLevel1(target: String) {
        val slots = _state.value.slots
        val bank = _state.value.bank.toMutableList()

        var targetIndex = -1
        for (i in slots.indices) {
            if (slots[i].letter?.char != target[i]) {
                targetIndex = i
                break
            }
        }

        if (targetIndex == -1) return

        val targetChar = target[targetIndex]
        val letter = findAvailableLetter(targetChar, bank) ?: return

        _state.update { state ->
            val newSlots = state.slots.toMutableList()
            val prevLetter = newSlots[targetIndex].letter

            if (prevLetter != null) {
                val bankIdx = bank.indexOfFirst { it.id == prevLetter.id }
                if (bankIdx != -1) bank[bankIdx] = bank[bankIdx].copy(status = LetterStatus.AVAILABLE)
            }

            val bankIdx = bank.indexOfFirst { it.id == letter.id }
            if (bankIdx != -1) bank[bankIdx] = bank[bankIdx].copy(status = LetterStatus.USED)

            newSlots[targetIndex] = newSlots[targetIndex].copy(
                letter = letter,
                isLocked = true,
                status = SlotStatus.CORRECT
            )

            state.copy(slots = newSlots, bank = bank)
        }
    }

    private fun applyHintLevel2(target: String) {
        _state.update { state ->
            val newSlots = state.slots.toMutableList()
            val newBank = state.bank.toMutableList()

            for (i in newSlots.indices step 2) {
                if (newSlots[i].isLocked && newSlots[i].letter?.char == target[i]) continue

                val targetChar = target[i]
                val letter = findAvailableLetterInBank(targetChar, newBank)

                if (letter != null) {
                    val prevLetter = newSlots[i].letter
                    if (prevLetter != null) {
                        val bankIdx = newBank.indexOfFirst { it.id == prevLetter.id }
                        if (bankIdx != -1) newBank[bankIdx] =
                            newBank[bankIdx].copy(status = LetterStatus.AVAILABLE)
                    }

                    val bankIdx = newBank.indexOfFirst { it.id == letter.id }
                    if (bankIdx != -1) newBank[bankIdx] =
                        newBank[bankIdx].copy(status = LetterStatus.USED)

                    newSlots[i] = newSlots[i].copy(
                        letter = letter,
                        isLocked = true,
                        status = SlotStatus.CORRECT
                    )
                }
            }
            state.copy(slots = newSlots, bank = newBank)
        }
    }

    private fun applyHintLevel3(target: String) {
        _state.update { state ->
            val newBank = state.bank.map { letter ->
                if (letter.status == LetterStatus.AVAILABLE && !target.contains(letter.char)) {
                    letter.copy(status = LetterStatus.HIDDEN)
                } else {
                    letter
                }
            }
            state.copy(bank = newBank)
        }
    }

    private fun findAvailableLetterInBank(char: Char, bank: List<Letter>): Letter? =
        bank.find { it.char == char && it.status == LetterStatus.AVAILABLE }

    private fun findAvailableLetter(
        char: Char,
        bank: List<Letter>
    ): Letter? {
        var letter = bank.find { it.char == char && it.status == LetterStatus.AVAILABLE }

        if (letter == null) {
            letter = bank.find { it.char == char && it.status == LetterStatus.USED }
            if (letter != null) {
                _state.update { state ->
                    val newSlots = state.slots.map { slot ->
                        if (slot.letter?.id == letter.id) {
                            slot.copy(letter = null, status = SlotStatus.EMPTY)
                        } else slot
                    }
                    state.copy(slots = newSlots)
                }
            }
        }

        return letter
    }

    /** Letter-bank validation: training only — records no production events (ADR-0008). */
    private fun checkValidation() {
        val currentState = _state.value
        if (!currentState.allSlotsFilled) return
        if (currentState.feedback != FeedbackState.IDLE) return

        val currentSpelling = currentState.slots.mapNotNull { it.letter?.char }.joinToString("")
        val target = currentState.currentWord?.wordEn?.uppercase() ?: return

        if (currentSpelling == target) {
            succeed(newStreak = _state.value.streak + 1)
        } else {
            handleError(target)
        }
    }

    /** Success feedback shared by both modes; never repeats the same word. */
    private fun succeed(newStreak: Int) {
        _state.update { it.copy(streak = newStreak, feedback = FeedbackState.SUCCESS) }

        viewModelScope.launch {
            val wordEn = _state.value.currentWord?.wordEn ?: ""
            _events.send(SpellingGameEvent.PlayTts(wordEn, "en-US"))
        }

        // ADR-0008: the same word is never re-served in this session — the queue
        // advances on [NavigateBack].
        viewModelScope.launch {
            delay(SUCCESS_HOLD_MS)
            _events.send(SpellingGameEvent.NavigateBack)
        }
    }

    private fun handleError(target: String) {
        val newMistakes = _state.value.mistakes + 1

        _state.update { state ->
            val newSlots = state.slots.mapIndexed { index, slot ->
                if (slot.isLocked) slot
                else slot.copy(
                    status = if (slot.letter?.char == target[index]) SlotStatus.FILLED else SlotStatus.ERROR
                )
            }
            state.copy(
                slots = newSlots,
                mistakes = newMistakes,
                feedback = FeedbackState.ERROR,
                shakeTrigger = state.shakeTrigger + 1
            )
        }

        // Bank errors are training mistakes: they never persist production failures.
        viewModelScope.launch {
            delay(appConfigProvider.spellingAutoAdvanceMs)
            _state.update { state ->
                val returningIds = mutableListOf<String>()

                val newSlots = state.slots.mapIndexed { index, slot ->
                    if (slot.isLocked) return@mapIndexed slot

                    val isCorrectChar = slot.letter?.char == target[index]
                    if (!isCorrectChar) {
                        slot.letter?.id?.let { returningIds.add(it) }
                        slot.copy(letter = null, status = SlotStatus.EMPTY)
                    } else {
                        slot.copy(status = SlotStatus.FILLED)
                    }
                }

                val newBank = state.bank.map { l ->
                    if (returningIds.contains(l.id)) l.copy(status = LetterStatus.AVAILABLE) else l
                }

                state.copy(
                    slots = newSlots,
                    bank = newBank,
                    feedback = FeedbackState.IDLE
                )
            }
        }
    }

    /**
     * "Reveal answer": one recorded production failure in both modes (P-02);
     * the solved slots lock so the reveal cannot be replayed for credit.
     */
    fun onRevealAnswer() {
        val state = _state.value
        if (state.isInputBlocked) return
        val word = state.currentWord ?: return

        recordProductionFailOnce(word.id)

        val target = word.wordEn.uppercase()
        val revealedSlots = state.slots.mapIndexed { index, slot ->
            slot.copy(
                letter = Letter(id = "reveal-$index", char = target[index]),
                isLocked = true,
                status = SlotStatus.CORRECT
            )
        }
        _state.update { it.copy(slots = revealedSlots, feedback = FeedbackState.SUCCESS) }
    }

    /**
     * Back/close. Exiting after an unrecorded attempt persists one production
     * failure — an abandoned retrieval never vanishes (durable at tap time).
     */
    fun onCloseClick() {
        val state = _state.value
        val word = state.currentWord
        if (word != null && state.mistakes > 0 &&
            state.feedback != FeedbackState.SUCCESS && state.feedback != FeedbackState.MASTERED
        ) {
            recordProductionFailOnce(word.id)
        }
        viewModelScope.launch {
            _events.send(SpellingGameEvent.NavigateBack)
        }
    }
}
