package com.venom.lingospell

import androidx.datastore.preferences.core.edit
import com.venom.data.local.PreferencesKeys
import com.venom.harness.VmSessionHarness
import com.venom.harness.testWord
import com.venom.harness.testWordEntity
import com.venom.lingospell.domain.FeedbackState
import com.venom.lingospell.domain.LetterStatus
import com.venom.lingospell.presentation.HintCreditPolicy
import com.venom.lingospell.presentation.SpellingGameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * ADR-0009 contracts (letter bank restored, hint budget + credit policy):
 *
 * - Success with no hint (or within the policy limit) -> one keyed production success.
 * - Success with hints beyond the limit -> zero production events.
 * - Policy NO_CREDIT_IF_ANY_HINT: any hint -> zero production events.
 * - Failures (3rd error / reveal / exit-after-attempt) -> exactly one keyed fail,
 *   regardless of hints; replay never doubles.
 * - One credit per word per session; the word is never re-served after success.
 * - Celebration dialog at 3 successes is UI-only (never a mastery claim).
 */
class SpellingProductionContractTest {

    private lateinit var harness: VmSessionHarness
    private lateinit var scheduler: TestCoroutineScheduler

    @BeforeEach
    fun setUp() {
        val dispatcher = UnconfinedTestDispatcher()
        scheduler = dispatcher.scheduler
        Dispatchers.setMain(dispatcher)
        harness = VmSessionHarness(words = listOf(testWordEntity(7)))
    }

    @AfterEach
    fun tearDown() {
        harness.close()
        Dispatchers.resetMain()
    }

    private fun viewModel(): SpellingGameViewModel {
        val vm = SpellingGameViewModel(
            progressRepository = harness.progressRepository,
            identityRepository = harness.identity,
            appConfigProvider = com.venom.domain.provider.AppConfigProvider(
                com.venom.harness.FakeRemoteConfig()
            ),
            dataStore = harness.dataStore
        )
        vm.setCustomWord(testWord(7))
        scheduler.advanceUntilIdle()
        return vm
    }

    private fun productionEvents() = harness.quizDao.events
        .filter { it.wordId == 7 && it.questionType == "PRODUCTION" }

    /** Clicks bank letters to spell the expected form, optionally using hints first. */
    private fun completeByBank(vm: SpellingGameViewModel, useHintsFirst: Int = 0) {
        repeat(useHintsFirst) { vm.onHintRequest(); scheduler.advanceUntilIdle() }
        val target = vm.state.value.currentWord!!.wordEn.uppercase()
        var guard = 0
        while (!vm.state.value.allSlotsFilled && guard < 40) {
            val i = vm.state.value.slots.indices.firstOrNull { j ->
                val s = vm.state.value.slots[j]
                !s.isLocked && (s.letter == null || s.letter?.char != target[j])
            } ?: break
            if (vm.state.value.slots[i].letter != null) vm.onSlotClick(i)
            val letter = vm.state.value.bank.first {
                it.status == LetterStatus.AVAILABLE && it.char == target[i]
            }
            vm.onLetterClick(letter)
            scheduler.advanceUntilIdle()
            guard++
        }
        scheduler.advanceUntilIdle()
    }

    private fun settleSuccess() {
        scheduler.advanceTimeBy(1200L + 10)
        scheduler.advanceUntilIdle()
    }

    private fun fillWrong(vm: SpellingGameViewModel) {
        vm.onClearAll()
        val target = (vm.state.value.currentWord!!.productionExpected
            ?: vm.state.value.currentWord!!.wordEn).uppercase()
        val available = vm.state.value.bank
            .filter { it.status == LetterStatus.AVAILABLE }
            .map { it.char }
        val odd = available.firstOrNull { it !in target }
            ?: available.first { it != target[0] }
        val arrangement = listOf(odd) + available.filter { it != odd }.take(target.length - 1)
        for (c in arrangement) {
            val letter = vm.state.value.bank.first {
                it.status == LetterStatus.AVAILABLE && it.char == c
            }
            vm.onLetterClick(letter)
        }
    }

    private fun vmWithPolicy(vm: SpellingGameViewModel, policy: String) {
        vm.setHintCreditPolicy(HintCreditPolicy.valueOf(policy))
        scheduler.advanceUntilIdle()
    }

    @Test
    fun `success with no hint records one keyed production success`() {
        runBlocking {
            val vm = viewModel()
            completeByBank(vm)
            settleSuccess()

            harness.await(scheduler) { productionEvents().count { it.isCorrect } == 1 }
            val success = productionEvents().first { it.isCorrect }
            assertTrue(!success.idempotencyKey.isNullOrBlank() && success.idempotencyKey.contains(":7:"))
            assertEquals(FeedbackState.SUCCESS, vm.state.value.feedback)
        }
    }

    @Test
    fun `success with one hint counts under the default policy`() {
        runBlocking {
            val vm = viewModel() // default: CREDIT_UP_TO_N with N = 1
            completeByBank(vm, useHintsFirst = 1)
            settleSuccess()

            harness.await(scheduler) { productionEvents().count { it.isCorrect } == 1 }
        }
    }

    @Test
    fun `success with hints beyond the limit records no production event`() {
        runBlocking {
            val vm = viewModel() // limit = 1
            completeByBank(vm, useHintsFirst = 2)
            settleSuccess()

            scheduler.advanceTimeBy(1500)
            scheduler.advanceUntilIdle()
            assertTrue(productionEvents().isEmpty(), "beyond-budget success is training only")
            assertEquals(FeedbackState.SUCCESS, vm.state.value.feedback)
        }
    }

    @Test
    fun `no-credit policy voids any hinted success`() {
        runBlocking {
            val vm = viewModel()
            vmWithPolicy(vm, "NO_CREDIT_IF_ANY_HINT")
            completeByBank(vm, useHintsFirst = 1)
            settleSuccess()

            scheduler.advanceTimeBy(1500)
            scheduler.advanceUntilIdle()
            assertTrue(productionEvents().isEmpty())
        }
    }

    @Test
    fun `hints never block failure recording`() {
        runBlocking {
            val vm = viewModel()
            completeByBank(vm, useHintsFirst = 1) // hinted success (counted under default)
            settleSuccess()
            vm.setCustomWord(testWord(7))
            scheduler.advanceUntilIdle()
            repeat(3) {
                fillWrong(vm)
                scheduler.advanceTimeBy(com.venom.domain.model.DefaultConfig.Spelling.AUTO_ADVANCE_MS + 10)
                scheduler.advanceUntilIdle()
            }

            harness.await(scheduler) { productionEvents().count { !it.isCorrect } == 1 }
        }
    }

    @Test
    fun `reveal answer records one keyed failure and replay never doubles it`() {
        runBlocking {
            val vm = viewModel()
            fillWrong(vm)
            scheduler.advanceTimeBy(com.venom.domain.model.DefaultConfig.Spelling.AUTO_ADVANCE_MS + 10)
            scheduler.advanceUntilIdle()

            vm.onRevealAnswer()
            harness.await(scheduler) {
                harness.progressDao.rows["u1" to 7] != null &&
                    harness.progressDao.rows["u1" to 7]!!.lapsesCount == 1
            }
            assertEquals(1, productionEvents().count { !it.isCorrect })

            harness.progressRepository.replayUnappliedEvents("u1")
            scheduler.advanceUntilIdle()
            assertEquals(1, productionEvents().count { !it.isCorrect }, "replay adds nothing")
        }
    }

    @Test
    fun `exit after an attempt records the failure once`() {
        runBlocking {
            val vm = viewModel()
            fillWrong(vm)
            scheduler.advanceTimeBy(com.venom.domain.model.DefaultConfig.Spelling.AUTO_ADVANCE_MS + 10)
            scheduler.advanceUntilIdle()

            vm.onCloseClick()
            harness.await(scheduler) { productionEvents().count { !it.isCorrect } == 1 }
            assertEquals(1, productionEvents().count { !it.isCorrect })
        }
    }

    @Test
    fun `same word twice in one session is credited exactly once`() {
        runBlocking {
            val vm = viewModel()
            completeByBank(vm)
            settleSuccess()
            harness.await(scheduler) { productionEvents().count { it.isCorrect } == 1 }

            vm.setCustomWord(testWord(7))
            scheduler.advanceUntilIdle()
            completeByBank(vm)
            settleSuccess()
            scheduler.advanceUntilIdle()

            assertEquals(1, productionEvents().count { it.isCorrect }, "ADR-0009 guard")
        }
    }

    @Test
    fun `celebration at three successes never claims mastery`() {
        runBlocking {
            val vm = viewModel()
            var celebrated = false
            repeat(3) {
                vm.setCustomWord(testWord(7))
                scheduler.advanceUntilIdle()
                completeByBank(vm)
                settleSuccess()
                if (vm.state.value.showCelebrationDialog) {
                    celebrated = true
                    vm.onContinueAfterCelebration()
                    scheduler.advanceUntilIdle()
                }
            }

            assertTrue(celebrated, "the 3-success celebration shows")
            val row = harness.progressDao.rows["u1" to 7]
            assertFalse(row?.knownState == "MASTERED", "celebration is not mastery")
            assertTrue(productionEvents().count { it.isCorrect } >= 1)
        }
    }

    @Test
    fun `max hints zero disables hint requests`() {
        runBlocking {
            val vm = viewModel()
            vm.setMaxHintsPerWord(0)
            scheduler.advanceUntilIdle()

            val before = vm.state.value.hintLevel
            vm.onHintRequest()
            scheduler.advanceUntilIdle()

            assertEquals(before, vm.state.value.hintLevel, "no hint granted at zero budget")
            assertTrue(productionEvents().isEmpty())
        }
    }
}
