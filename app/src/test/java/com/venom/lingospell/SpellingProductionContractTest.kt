package com.venom.lingospell

import com.venom.harness.VmSessionHarness
import com.venom.harness.testWord
import com.venom.harness.testWordEntity
import com.venom.lingospell.domain.FeedbackState
import com.venom.lingospell.domain.LetterStatus
import com.venom.lingospell.presentation.SpellingGameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Phase 0 / P-02 production-signal contracts (RED before fix):
 *
 * A spelling failure is a real learning signal: the third mistake, "reveal answer",
 * and exit-after-attempt must each persist exactly ONE production-failure event
 * carrying an idempotency key (session:word:outcome:ordinal), and every spelling
 * success must be keyed too. Replay must never double-apply a projection.
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

    private fun makeMistake(vm: SpellingGameViewModel) {
        vm.onClearAll()
        val target = vm.state.value.currentWord!!.wordEn.uppercase()
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

    private fun spellCorrectly(vm: SpellingGameViewModel) {
        vm.onClearAll()
        val target = vm.state.value.currentWord!!.wordEn.uppercase()
        for (c in target) {
            val letter = vm.state.value.bank.first {
                it.status == LetterStatus.AVAILABLE && it.char == c
            }
            vm.onLetterClick(letter)
        }
    }

    private fun settleAfterError(vm: SpellingGameViewModel) {
        scheduler.advanceTimeBy(com.venom.domain.model.DefaultConfig.Spelling.AUTO_ADVANCE_MS + 10)
        scheduler.advanceUntilIdle()
        assertEquals(FeedbackState.IDLE, vm.state.value.feedback, "error state resets for retry")
    }

    @Test
    fun `third spelling mistake records exactly one production failure`() {
        runBlocking {
            val vm = viewModel()

            repeat(3) {
                makeMistake(vm)
                assertEquals(FeedbackState.ERROR, vm.state.value.feedback)
                settleAfterError(vm)
            }

            harness.await(scheduler) {
                val row = harness.progressDao.rows["u1" to 7]
                row != null && row.lapsesCount == 1 && row.totalAttempts == 1
            }

            val failEvents = harness.quizDao.events.filter {
                it.wordId == 7 && it.questionType == "PRODUCTION" && !it.isCorrect
            }
            assertEquals(1, failEvents.size, "one persisted production failure, not per-keystroke")

            val row = harness.progressDao.rows["u1" to 7]!!
            assertEquals(1, row.lapsesCount, "failure counts one lapse")
            assertEquals(1, row.totalAttempts, "failure counts one attempt")
            assertEquals(6, row.difficulty, "failure hardens difficulty by one")

            // The event is already applied; replay must add nothing.
            val before = harness.progressDao.rows.size
            harness.progressRepository.replayUnappliedEvents("u1")
            scheduler.advanceUntilIdle()
            Thread.sleep(150)
            scheduler.advanceUntilIdle()
            assertEquals(before, harness.progressDao.rows.size, "replay adds no writes")
            assertEquals(1, harness.quizDao.events.count { it.wordId == 7 })
        }
    }

    @Test
    fun `reveal answer records one failure and blocks further attempts`() {
        runBlocking {
            val vm = viewModel()
            makeMistake(vm)
            settleAfterError(vm)

            vm.onRevealAnswer()
            harness.await(scheduler) {
                harness.quizDao.events.count {
                    it.wordId == 7 && it.questionType == "PRODUCTION" && !it.isCorrect
                } == 1
            }

            val failEvents = harness.quizDao.events.filter {
                it.wordId == 7 && it.questionType == "PRODUCTION" && !it.isCorrect
            }
            assertEquals(1, failEvents.size)
            assertTrue(
                vm.state.value.slots.all { it.isLocked },
                "revealing the answer locks the solved slots"
            )

            // Reveal twice must not double-record.
            vm.onRevealAnswer()
            scheduler.advanceUntilIdle()
            Thread.sleep(150)
            scheduler.advanceUntilIdle()
            assertEquals(
                1,
                harness.quizDao.events.count {
                    it.wordId == 7 && it.questionType == "PRODUCTION" && !it.isCorrect
                },
                "a second reveal adds no further failure event"
            )
        }
    }

    @Test
    fun `exit after an attempt records the failure once`() {
        runBlocking {
            val vm = viewModel()
            makeMistake(vm)
            settleAfterError(vm)

            vm.onCloseClick()
            harness.await(scheduler) {
                harness.quizDao.events.count {
                    it.wordId == 7 && it.questionType == "PRODUCTION" && !it.isCorrect
                } == 1
            }
            assertEquals(
                1,
                harness.quizDao.events.count {
                    it.wordId == 7 && it.questionType == "PRODUCTION" && !it.isCorrect
                },
                "exiting after one mistake records exactly one failure"
            )
        }
    }

    @Test
    fun `successful spelling persists a keyed production success`() {
        runBlocking {
            val vm = viewModel()
            spellCorrectly(vm)

            harness.await(scheduler) {
                harness.quizDao.events.count {
                    it.wordId == 7 && it.questionType == "PRODUCTION" && it.isCorrect
                } == 1
            }

            val success = harness.quizDao.events.first {
                it.wordId == 7 && it.questionType == "PRODUCTION" && it.isCorrect
            }
            assertTrue(
                !success.idempotencyKey.isNullOrBlank() && success.idempotencyKey.contains(":7:"),
                "production success carries a session:word:outcome:ordinal key"
            )
            assertEquals("u1", success.userId)

            val row = harness.progressDao.rows["u1" to 7]!!
            assertEquals(1, row.productionSuccess, "success promotes the production counter")
            assertEquals(0, row.recallSuccess, "spelling never touches recall counters")
        }
    }
}
