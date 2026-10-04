package com.venom.lingospell

import com.venom.harness.VmSessionHarness
import com.venom.harness.testWord
import com.venom.harness.testWordEntity
import com.venom.lingospell.domain.FeedbackState
import com.venom.lingospell.domain.LetterStatus
import com.venom.lingospell.presentation.SpellingGameMode
import com.venom.lingospell.presentation.SpellingGameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Phase 2 / ADR-0008 production contracts (typed flow):
 *
 * 1. Typed success with no help  -> exactly one keyed production success.
 * 2. Success after any hint      -> zero production events (training mode).
 * 3. Accepted variant            -> correct, keyed success.
 * 4. Same word twice in session  -> credit exactly once.
 * 5. Reveal answer               -> one keyed failure; replay never doubles it.
 * 6. Hints alone can never reach MASTERED.
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
            )
        )
        vm.setCustomWord(testWord(7))
        scheduler.advanceUntilIdle()
        return vm
    }

    private fun typeAndSubmit(vm: SpellingGameViewModel, text: String) {
        vm.onTypedInputChange(text)
        vm.onSubmitTyped()
        scheduler.advanceUntilIdle()
    }

    private fun productionEvents() = harness.quizDao.events
        .filter { it.wordId == 7 && it.questionType == "PRODUCTION" }

    private fun completeByBankAfterHint(vm: SpellingGameViewModel) {
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

    @Test
    fun `typed success with no help records one keyed production success`() {
        runBlocking {
            val vm = viewModel()
            typeAndSubmit(vm, "Obtain")

            harness.await(scheduler) { productionEvents().count { it.isCorrect } == 1 }

            val success = productionEvents().first { it.isCorrect }
            assertTrue(
                !success.idempotencyKey.isNullOrBlank() && success.idempotencyKey.contains(":7:"),
                "success carries a session:word:outcome:ordinal key"
            )
            assertEquals(FeedbackState.SUCCESS, vm.state.value.feedback)

            harness.await(scheduler) {
                harness.progressDao.rows["u1" to 7]?.productionSuccess == 1
            }
        }
    }

    @Test
    fun `success after a hint records zero production events`() {
        runBlocking {
            val vm = viewModel()
            vm.onTypedInputChange("sub")
            vm.onEnterTraining() // converting to training is itself a hint (ADR-0008)
            scheduler.advanceUntilIdle()
            assertEquals(SpellingGameMode.TRAINING, vm.state.value.mode)

            completeByBankAfterHint(vm)
            scheduler.advanceTimeBy(1200L + 10)
            scheduler.advanceUntilIdle()

            assertTrue(
                productionEvents().isEmpty(),
                "hinted success is training: no production success, no production failure"
            )
        }
    }

    @Test
    fun `accepted variant grades as a keyed correct attempt`() {
        runBlocking {
            val vm = viewModel()
            typeAndSubmit(vm, "  aCquire ") // variant, case+whitespace normalized

            harness.await(scheduler) { productionEvents().count { it.isCorrect } == 1 }
            assertEquals(FeedbackState.SUCCESS, vm.state.value.feedback)
        }
    }

    @Test
    fun `near miss records nothing and invites a retry`() {
        runBlocking {
            val vm = viewModel()
            typeAndSubmit(vm, "obtan") // one edit away

            scheduler.advanceTimeBy(com.venom.domain.model.DefaultConfig.Spelling.AUTO_ADVANCE_MS + 10)
            scheduler.advanceUntilIdle()

            assertTrue(productionEvents().isEmpty(), "near-miss is unrecorded")
            assertEquals(0, vm.state.value.mistakes, "near-miss is not a full error")
            assertEquals(FeedbackState.IDLE, vm.state.value.feedback, "retry is offered")

            typeAndSubmit(vm, "obtain")
            harness.await(scheduler) { productionEvents().count { it.isCorrect } == 1 }
        }
    }

    @Test
    fun `same word twice in one session is credited exactly once`() {
        runBlocking {
            val vm = viewModel()
            typeAndSubmit(vm, "obtain")
            harness.await(scheduler) { productionEvents().count { it.isCorrect } == 1 }

            // The queue re-opens the same word inside the same editor session.
            vm.setCustomWord(testWord(7))
            scheduler.advanceUntilIdle()
            typeAndSubmit(vm, "obtain")
            scheduler.advanceTimeBy(1200L + 10)
            scheduler.advanceUntilIdle()

            assertEquals(
                1, productionEvents().count { it.isCorrect },
                "ADR-0008: one counted production success per word per session"
            )
        }
    }

    @Test
    fun `reveal answer records one keyed failure and replay never doubles it`() {
        runBlocking {
            val vm = viewModel()
            typeAndSubmit(vm, "xxxxxx") // full error, mistake 1
            scheduler.advanceTimeBy(com.venom.domain.model.DefaultConfig.Spelling.AUTO_ADVANCE_MS + 10)
            scheduler.advanceUntilIdle()

            vm.onRevealAnswer()
            harness.await(scheduler) {
                harness.progressDao.rows["u1" to 7] != null &&
                    harness.progressDao.rows["u1" to 7]!!.lapsesCount == 1
            }

            val fails = productionEvents().filter { !it.isCorrect }
            assertEquals(1, fails.size)
            assertTrue(!fails[0].idempotencyKey.isNullOrBlank())

            harness.progressRepository.replayUnappliedEvents("u1")
            scheduler.advanceUntilIdle()
            assertEquals(1, productionEvents().count { !it.isCorrect }, "replay adds nothing")
        }
    }

    @Test
    fun `three full errors record exactly one production failure`() {
        runBlocking {
            val vm = viewModel()
            repeat(3) {
                typeAndSubmit(vm, "xxxxxx")
                scheduler.advanceTimeBy(com.venom.domain.model.DefaultConfig.Spelling.AUTO_ADVANCE_MS + 10)
                scheduler.advanceUntilIdle()
                if (it < 2) assertEquals(FeedbackState.IDLE, vm.state.value.feedback)
            }

            harness.await(scheduler) {
                harness.progressDao.rows["u1" to 7]?.lapsesCount == 1
            }
            assertEquals(1, productionEvents().count { !it.isCorrect }, "one failure, not per-keystroke")
        }
    }

    @Test
    fun `hints alone can never reach mastery`() {
        runBlocking {
            val vm = viewModel()
            repeat(3) {
                vm.setCustomWord(testWord(7))
                scheduler.advanceUntilIdle()
                vm.onEnterTraining()
                scheduler.advanceUntilIdle()
                // Complete through the bank with hints as help.
                val target = vm.state.value.currentWord!!.wordEn.uppercase()
                var guard = 0
                while (!vm.state.value.allSlotsFilled && guard < target.length * 2) {
                    val idx = vm.state.value.slots.indexOfFirst { it.letter == null }
                    val needed = target[idx]
                    val letter = vm.state.value.bank.first {
                        it.status == LetterStatus.AVAILABLE && it.char == needed
                    }
                    vm.onLetterClick(letter)
                    scheduler.advanceUntilIdle()
                    guard++
                }
                scheduler.advanceTimeBy(1200L + 10)
                scheduler.advanceUntilIdle()
            }

            assertTrue(productionEvents().isEmpty(), "training path mints zero production events")
            val row = harness.progressDao.rows["u1" to 7]
            assertNotEquals("MASTERED", row?.knownState, "hints alone cannot master")
        }
    }
}
