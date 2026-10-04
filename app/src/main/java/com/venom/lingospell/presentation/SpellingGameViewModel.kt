package com.venom.lingospell.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venom.data.repo.UserWordProgressRepository
import com.venom.domain.model.WordMaster
import com.venom.domain.provider.AppConfigProvider
import com.venom.domain.repo.IUserIdentityRepository
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
        /** Mistakes tolerated before the attempt persists as a production failure. */
        const val MAX_MISTAKES_BEFORE_FAIL = 3
    }

    private val sessionId: String = UUID.randomUUID().toString()
    private val tapCounters = ConcurrentHashMap<Int, AtomicInteger>()
    private val failedWords: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    private val _state = MutableStateFlow(SpellingGameState())
    val state: StateFlow<SpellingGameState> = _state.asStateFlow()

    private val _events = Channel<SpellingGameEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        initializeWord(resetStreak = false)
    }

    fun setCustomWord(wordData: WordMaster) {
        _state.update { state ->
            state.copy(
                overrideWord = wordData,
                streak = 0,
                mistakes = 0,
                hintLevel = HintLevel.NONE,
                feedback = FeedbackState.IDLE,
                isLoading = true,
                showMasteryDialog = false
            )
        }
        // Pass wordData directly to avoid reading stale _state.value.overrideWord
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
                isLoading = true,
                showMasteryDialog = false
            )
        }
    }

    /**
     * Initializes or resets the current word.
     * If wordData is provided, it will be used directly instead of reading overrideWord from state
     * (avoids races with asynchronous state updates).
     */
    private fun initializeWord(resetStreak: Boolean, wordData: WordMaster? = null) {
        val currentState = _state.value
        val currentWord = wordData ?: currentState.overrideWord ?: return

        val target = currentWord.wordEn.uppercase()
        val newSlots = SpellingUtils.createSlots(target)
        val newBank = SpellingUtils.generateBank(target, appConfigProvider.spellingDistractors)

        _state.update { state ->
            state.copy(
                // ensure overrideWord is set to the word we actually initialized with
                overrideWord = currentWord,
                slots = newSlots,
                bank = newBank,
                hintLevel = HintLevel.NONE,
                mistakes = 0,
                feedback = FeedbackState.IDLE,
                streak = if (resetStreak) 0 else state.streak,
                isLoading = false,
                showMasteryDialog = false
            )
        }

        // Record view
        viewModelScope.launch {
            progressRepository.recordView(identityRepository.getCurrentUserId(), currentWord.id)
        }


        // Play Arabic TTS after short delay
//        viewModelScope.launch {
//            delay(appConfigProvider.spellingTtsInitialDelay)
//            _events.send(SpellingGameEvent.PlayTts(currentWord.arabicAr, "ar-SA"))
//        }
    }

    /**
     * Dedupe key for exactly-once production commits: session + word + outcome + ordinal
     * (Constitution Article II). Each spelling attempt is a distinct tap event.
     */
    private fun tapKey(wordId: Int, outcome: String): String =
        "$sessionId:$wordId:$outcome:" +
            tapCounters.getOrPut(wordId) { AtomicInteger(0) }.incrementAndGet()

    /**
     * One production failure per word per spelling visit: the first qualifying exit
     * (third mistake, reveal answer, or exit-after-attempt) persists it; repeats are
     * bookkeeping no-ops.
     */
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

    /**
     * Handles letter tap from the bank.
     */
    fun onLetterClick(letter: Letter) {
        val currentState = _state.value
        if (currentState.isInputBlocked) return
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

        // Check validation after placing letter
        checkValidation()
    }

    /**
     * Handles slot tap to remove letter.
     */
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

    /**
     * Clears all non-locked letters from slots.
     */
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

    /**
     * Applies next hint level.
     * Only available in Training Mode.
     */
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

        // Auto-validate for Hint 1/2 if they fill slots
        if (newLevel == HintLevel.LEVEL_1 || newLevel == HintLevel.LEVEL_2) {
            checkValidation()
        }
    }

    /**
     * Hint 1: Reveal next needed letter.
     * Immediate validation handled by caller.
     */
    private fun applyHintLevel1(target: String) {
        val slots = _state.value.slots
        val bank = _state.value.bank.toMutableList()

        // Find first position that's wrong or empty
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

    /**
     * Hint 2: Alternating Pattern (S _ H _ O _ L).
     * Fills indices 0, 2, 4...
     */
    private fun applyHintLevel2(target: String) {
        _state.value

        _state.update { state ->
            val newSlots = state.slots.toMutableList()
            val newBank = state.bank.toMutableList()
            mutableListOf<String>() // Track newly used to update bank at once if optimizing

            // 1. Identify letters needed for alternating positions (0, 2, 4...)
            for (i in newSlots.indices step 2) {
                // Skip if already correct/locked
                if (newSlots[i].isLocked && newSlots[i].letter?.char == target[i]) continue

                val targetChar = target[i]

                // Find letter: prioritize bank, then steal from other slots
                // Note: We need a fresh lookup each time or careful management of tempBank
                // For simplicity, we restart search from current state for each slot,
                // but real-world robust impl needs to track claimed letters.

                val letter = findAvailableLetter(targetChar, newBank)

                if (letter != null) {
                    // Return existing letter if any
                    val prevLetter = newSlots[i].letter
                    if (prevLetter != null) {
                        val bankIdx = newBank.indexOfFirst { it.id == prevLetter.id }
                        if (bankIdx != -1) newBank[bankIdx] =
                            newBank[bankIdx].copy(status = LetterStatus.AVAILABLE)
                    }

                    // Mark new letter used
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

    /**
     * Hint 3: Remove Distractors.
     * Removes/Hides letters in the bank that are not part of the target word.
     */
    private fun applyHintLevel3(target: String) {
        target.groupingBy { it }.eachCount()

        _state.update { state ->
            val newBank = state.bank.map { letter ->
                // If letter is available and NOT needed (or excess count), hide it.
                // Simplified: If char not in word, hide.
                // Advanced: Count needed vs available.
                // Sticking to simple "Not in word" or "Excess" logic.
                // User request: "Remove uncorrect letter from letters bank"

                if (letter.status == LetterStatus.AVAILABLE && !target.contains(letter.char)) {
                    letter.copy(status = LetterStatus.HIDDEN)
                } else {
                    letter
                }
            }
            state.copy(bank = newBank)
        }
    }

    private fun findAvailableLetter(
        char: Char,
        bank: List<Letter>
    ): Letter? {
        // First try to find available in bank
        var letter = bank.find { it.char == char && it.status == LetterStatus.AVAILABLE }

        // If not found, try to reclaim from a non-locked slot
        if (letter == null) {
            letter = bank.find { it.char == char && it.status == LetterStatus.USED }
            if (letter != null) {
                // Clear that slot
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

    /**
     * Validates the current spelling when all slots are filled.
     */
    private fun checkValidation() {
        val currentState = _state.value
        if (!currentState.allSlotsFilled) return
        if (currentState.feedback != FeedbackState.IDLE) return

        val currentSpelling = currentState.slots.mapNotNull { it.letter?.char }.joinToString("")
        val target = currentState.currentWord?.wordEn?.uppercase() ?: return

        if (currentSpelling == target) {
            handleSuccess()
        } else {
            handleError(target)
        }
    }

    private fun handleSuccess() {
        val newStreak = _state.value.streak + 1
        val isMastered = newStreak >= appConfigProvider.spellingMaxStreak

        // Mark all slots as correct
        _state.update { state ->
            val newSlots = state.slots.map { it.copy(status = SlotStatus.CORRECT, isLocked = true) }
            state.copy(
                slots = newSlots,
                streak = newStreak,
                feedback = if (isMastered) FeedbackState.MASTERED else FeedbackState.SUCCESS,
                showMasteryDialog = isMastered
            )
        }

        // Record progress (keyed production success, durable at tap time)
        viewModelScope.launch {
            val wordId = _state.value.overrideWord?.id ?: return@launch
            progressRepository.recordProductionSuccess(
                identityRepository.getCurrentUserId(),
                wordId,
                tapKey(wordId, "PROD_SUCCESS"),
                sessionId
            )
        }


        // Play TTS
        viewModelScope.launch {
            val wordEn = _state.value.currentWord?.wordEn ?: ""
            val text = if (isMastered) {
                "Perfect! $wordEn"
            } else {
                wordEn
            }
            _events.send(SpellingGameEvent.PlayTts(text, "en-US"))
        }

        // Auto-advance only if NOT mastered
        if (!isMastered) {
            viewModelScope.launch {
                delay(appConfigProvider.spellingAutoAdvanceMs)
                initializeWord(resetStreak = false)
            }
        }
        // If mastered, wait for user to click Continue button
    }

    /**
     * Called when user clicks Continue button on mastery dialog
     */
    fun onContinueAfterMastery() {
        _state.update { it.copy(showMasteryDialog = false) }
        // Navigate back to flashcard after mastering the word
        viewModelScope.launch {
            _events.send(SpellingGameEvent.NavigateBack)
        }
    }

    private fun handleError(target: String) {
        val newMistakes = _state.value.mistakes + 1

        // Trigger shake animation
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
                shakeTrigger = state.shakeTrigger + 1,
                // Using a potential animation duration tracker if needed, otherwise just updating state.
                // We'll keep the logic simple as originally requested.
            )
        }

        // The third mistake means retrieval failed: persist one production failure.
        if (newMistakes >= MAX_MISTAKES_BEFORE_FAIL) {
            _state.value.currentWord?.id?.let { recordProductionFailOnce(it) }
        }

        // Reset incorrect letters after delay
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
     * Handles back/close button. Exiting after at least one attempt without a recorded
     * outcome persists the attempt as a production failure — an abandoned retrieval
     * never vanishes (durable at tap time).
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

    /**
     * Shows the correct spelling and records the attempt as one production failure:
     * seeing the answer after failing to produce it is a failed production event, and
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
}