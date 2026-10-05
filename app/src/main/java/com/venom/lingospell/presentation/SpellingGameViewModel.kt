package com.venom.lingospell.presentation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venom.data.local.PreferencesKeys
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
    val appConfigProvider: AppConfigProvider,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private companion object {
        /** Full errors tolerated before the attempt persists as a production failure. */
        const val MAX_MISTAKES_BEFORE_FAIL = 3
        const val SUCCESS_HOLD_MS = 1200L
        /** Consecutive successes that trigger the celebration dialog (not mastery). */
        const val CELEBRATION_STREAK = 3
    }

    private val sessionId: String = UUID.randomUUID().toString()
    private val tapCounters = ConcurrentHashMap<Int, AtomicInteger>()
    private val failedWords: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    /** ADR-0009: at most one counted production success per word per session. */
    private val creditedWords: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    private val _state = MutableStateFlow(SpellingGameState())
    val state: StateFlow<SpellingGameState> = _state.asStateFlow()

    private val _events = Channel<SpellingGameEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        collectHintSettings()
    }

    /** Live hint-budget settings from DataStore (ADR-0009). */
    private fun collectHintSettings() {
        viewModelScope.launch {
            dataStore.data.collect { prefs ->
                _state.update {
                    it.copy(
                        hintsEnabled = prefs[PreferencesKeys.HINTS_ENABLED] ?: true,
                        maxHintsPerWord = (prefs[PreferencesKeys.MAX_HINTS_PER_WORD] ?: 3)
                            .coerceIn(0, 3),
                        hintCreditPolicy = prefs[PreferencesKeys.HINT_CREDIT_POLICY]
                            ?: "CREDIT_UP_TO_N",
                        hintCreditLimit = (prefs[PreferencesKeys.HINT_CREDIT_LIMIT] ?: 1)
                            .coerceIn(0, 3)
                    )
                }
            }
        }
    }

    fun setHintsEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[PreferencesKeys.HINTS_ENABLED] = enabled } }
    }

    fun setMaxHintsPerWord(max: Int) {
        viewModelScope.launch {
            dataStore.edit { it[PreferencesKeys.MAX_HINTS_PER_WORD] = max.coerceIn(0, 3) }
        }
    }

    fun setHintCreditPolicy(policy: HintCreditPolicy) {
        viewModelScope.launch {
            dataStore.edit { it[PreferencesKeys.HINT_CREDIT_POLICY] = policy.name }
        }
    }

    fun setHintCreditLimit(limit: Int) {
        viewModelScope.launch {
            dataStore.edit { it[PreferencesKeys.HINT_CREDIT_LIMIT] = limit.coerceIn(0, 3) }
        }
    }

    fun setCustomWord(wordData: WordMaster) {
        _state.update { state ->
            state.copy(
                overrideWord = wordData,
                // Session-scoped streak: consecutive successes across words feed
                // the celebration; only initializeWord resets it per word start.
                mistakes = 0,
                hintLevel = HintLevel.NONE,
                feedback = FeedbackState.IDLE,
                isLoading = true,
                showCelebrationDialog = false
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
                isLoading = true,
                showCelebrationDialog = false
            )
        }
    }

    private fun initializeWord(resetStreak: Boolean, wordData: WordMaster? = null) {
        val currentState = _state.value
        val currentWord = wordData ?: currentState.overrideWord ?: return

        // The bank is built from the corpus contract's expected form (ADR-0005/0009).
        val target = (currentWord.productionExpected ?: currentWord.wordEn).uppercase()
        val newSlots = SpellingUtils.createSlots(target)
        val newBank = SpellingUtils.generateBank(target, appConfigProvider.spellingDistractors)

        _state.update { state ->
            state.copy(
                overrideWord = currentWord,
                slots = newSlots,
                bank = newBank,
                hintLevel = HintLevel.NONE,
                mistakes = 0,
                feedback = FeedbackState.IDLE,
                isLoading = false,
                showCelebrationDialog = false
            )
        }
        // ADR-0008/0009: no recordView/XP here — viewing a prompt is not an attempt.
    }

    /** Acceptance set: wordEn, productionExpected and every accepted variant. */
    private fun acceptedAnswers(word: WordMaster): List<String> = buildList {
        add(word.wordEn)
        word.productionExpected?.let { add(it) }
        addAll(word.productionVariants)
    }.map { it.trim().lowercase() }.filter { it.isNotBlank() }

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

    // ── Letter bank (the only input surface, ADR-0009) ──────────────────────

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
        if (!currentState.hintsEnabled) return
        if (currentState.hintLevel.value >= currentState.maxHintsPerWord) return
        if (!currentState.hintLevel.canUseHint) return
        if (currentState.feedback != FeedbackState.IDLE) return

        val word = currentState.currentWord ?: return
        val newLevel = currentState.hintLevel.next()
        val target = (word.productionExpected ?: word.wordEn).uppercase()

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
                val letter = newBank.find {
                    it.char == targetChar && it.status == LetterStatus.AVAILABLE
                }

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

    private fun checkValidation() {
        val currentState = _state.value
        if (!currentState.allSlotsFilled) return
        if (currentState.feedback != FeedbackState.IDLE) return

        val currentSpelling = currentState.slots.mapNotNull { it.letter?.char }.joinToString("")
        val word = currentState.currentWord ?: return

        if (currentSpelling.lowercase() in acceptedAnswers(word)) {
            handleSuccess()
        } else {
            handleError((word.productionExpected ?: word.wordEn).uppercase())
        }
    }

    /**
     * Success under the hint-credit policy (ADR-0009): within the budget → one
     * keyed production success (once per word per session); beyond it → UI only.
     * Hints never block failure recording.
     */
    private fun handleSuccess() {
        val state = _state.value
        val word = state.currentWord ?: return
        val hintsUsed = state.hintLevel.value

        val credited = when (state.hintCreditPolicy) {
            "NO_CREDIT_IF_ANY_HINT" -> hintsUsed == 0
            else -> hintsUsed <= state.hintCreditLimit
        } && creditedWords.add(word.id)

        if (credited) {
            viewModelScope.launch {
                progressRepository.recordProductionSuccess(
                    identityRepository.getCurrentUserId(),
                    word.id,
                    tapKey(word.id, "PROD_SUCCESS"),
                    sessionId
                )
            }
        }

        val newStreak = state.streak + 1
        val celebrate = newStreak % CELEBRATION_STREAK == 0
        _state.update {
            it.copy(
                streak = newStreak,
                feedback = FeedbackState.SUCCESS,
                showCelebrationDialog = celebrate
            )
        }

        viewModelScope.launch {
            val wordEn = _state.value.currentWord?.wordEn ?: ""
            _events.send(SpellingGameEvent.PlayTts(wordEn, "en-US"))
        }

        // The same word is never re-served; the queue advances. The celebration
        // holds navigation until Continue.
        if (!celebrate) {
            viewModelScope.launch {
                delay(SUCCESS_HOLD_MS)
                _events.send(SpellingGameEvent.NavigateBack)
            }
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

        if (newMistakes >= MAX_MISTAKES_BEFORE_FAIL) {
            _state.value.currentWord?.id?.let { recordProductionFailOnce(it) }
        }

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
     * "Reveal answer": one recorded production failure (P-02) — hints never block
     * failure recording; the solved slots lock so the reveal cannot replay.
     */
    fun onRevealAnswer() {
        val state = _state.value
        if (state.isInputBlocked) return
        val word = state.currentWord ?: return

        recordProductionFailOnce(word.id)

        val target = (word.productionExpected ?: word.wordEn).uppercase()
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

    /** Continue from the celebration dialog: back to the queue, next word. */
    fun onContinueAfterCelebration() {
        _state.update { it.copy(showCelebrationDialog = false) }
        viewModelScope.launch {
            _events.send(SpellingGameEvent.NavigateBack)
        }
    }
}
