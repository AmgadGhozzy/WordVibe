package com.venom.stackcard.ui

import com.venom.harness.VmSessionHarness
import com.venom.harness.testWord
import com.venom.harness.testWordEntity
import com.venom.stackcard.ui.viewmodel.WordMasteryEvent
import com.venom.stackcard.ui.viewmodel.WordMasteryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Phase 0 / P-01 session-verdict contracts (RED before fix):
 *
 * A forgotten-then-remembered card must record exactly ONE recall failure (at the
 * forget tap) and exactly ONE recall success (at the remember tap). The sticky
 * session verdict must never re-record the failure on the exit swipe, and
 * finalization must record zero additional learning events.
 */
class WordMasterySessionContractTest {

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

    private suspend fun viewModel(): WordMasteryViewModel {
        val vm = WordMasteryViewModel(
            progressRepository = harness.progressRepository,
            identityRepository = harness.identity,
            activityRepository = harness.activityRepository,
            wordMasterRepository = com.venom.harness.FakeWordMasterRepository(harness.wordDao),
            analyticsManager = com.venom.analytics.StubAnalyticsManager(),
            crashlyticsManager = com.venom.analytics.StubCrashlyticsManager(),
            dataStore = harness.dataStore
        )
        vm.onEvent(WordMasteryEvent.Initialize(isGenerative = false, topic = null))
        harness.await(scheduler) { vm.uiState.value.visibleCards.isNotEmpty() }
        return vm
    }

    @Test
    fun `forgotten then remembered records fail once and success once`() {
        kotlinx.coroutines.runBlocking {
            val vm = viewModel()
            val word = testWord(7)

            vm.onEvent(WordMasteryEvent.SwipeCard(word, isRemembered = false))
            harness.await(scheduler) {
                harness.quizDao.events.count { it.wordId == 7 && !it.isCorrect } == 1
            }

            vm.onEvent(WordMasteryEvent.SwipeCard(word, isRemembered = true))
            harness.await(scheduler) {
                harness.quizDao.events.count { it.wordId == 7 && it.isCorrect } == 1
            }
            harness.await(scheduler) {
                harness.quizDao.events.count { it.wordId == 7 } == 2
            }

            // Finalize (last card left the deck) must record zero additional events.
            harness.await(scheduler) { harness.activityDao.getTotalSessionCount("u1") == 1 }
            scheduler.advanceUntilIdle()
            Thread.sleep(200)
            scheduler.advanceUntilIdle()

            val events = harness.quizDao.events.filter { it.wordId == 7 }
            assertEquals(2, events.size, "two taps, two events — nothing more at finalize")
            assertEquals(1, events.count { !it.isCorrect }, "the forget tap is the only failure")
            assertEquals(1, events.count { it.isCorrect }, "the remember tap is the only success")
            assertTrue(
                events.all { it.questionType == "RECALL" },
                "swipe outcomes are recall events"
            )
        }
    }

    @Test
    fun `repeated forget swipes record one failure per attempt`() {
        kotlinx.coroutines.runBlocking {
            val vm = viewModel()
            val word = testWord(7)

            vm.onEvent(WordMasteryEvent.SwipeCard(word, isRemembered = false))
            harness.await(scheduler) {
                harness.quizDao.events.count { it.wordId == 7 && !it.isCorrect } == 1
            }
            vm.onEvent(WordMasteryEvent.SwipeCard(word, isRemembered = false))
            harness.await(scheduler) {
                harness.quizDao.events.count { it.wordId == 7 && !it.isCorrect } == 2
            }

            val events = harness.quizDao.events.filter { it.wordId == 7 }
            assertEquals(2, events.size, "each forget tap is one genuine failed attempt")
            assertTrue(
                events.none { it.isCorrect },
                "no success may be fabricated by repeated forget taps"
            )
            assertTrue(
                events.map { it.idempotencyKey }.distinct().size == 2,
                "each attempt carries its own idempotency key"
            )
        }
    }

    @Test
    fun `fresh remember swipe records exactly one success`() {
        kotlinx.coroutines.runBlocking {
            val vm = viewModel()
            val word = testWord(7)

            vm.onEvent(WordMasteryEvent.SwipeCard(word, isRemembered = true))
            harness.await(scheduler) {
                harness.quizDao.events.count { it.wordId == 7 && it.isCorrect } == 1
            }
            harness.await(scheduler) { harness.activityDao.getTotalSessionCount("u1") == 1 }
            scheduler.advanceUntilIdle()
            Thread.sleep(200)
            scheduler.advanceUntilIdle()

            val events = harness.quizDao.events.filter { it.wordId == 7 }
            assertEquals(1, events.size, "one remember tap, exactly one success event")
        }
    }

    @Test
    fun `new word count matches the frozen no-progress-row definition`() {
        kotlinx.coroutines.runBlocking {
            // Two seed words; one gains a progress row, one stays untouched.
            harness.wordDao.insert(com.venom.harness.testWordEntity(8))

            val vm = viewModel()
            val word = testWord(7)
            vm.onEvent(WordMasteryEvent.SwipeCard(word, isRemembered = true))
            // Wait for the projection (progress row), not just the event insert.
            harness.await(scheduler) { harness.progressDao.rows["u1" to 7] != null }

            // Frozen definition: a word row with NO progress row for this user.
            val newList = harness.progressRepository.getNewWordsForUser("u1", 10).map { it.id }
            val newCount = harness.progressRepository.countNewWordsForUser("u1")

            assertEquals(listOf(8), newList, "only the untouched word is new")
            assertEquals(newList.size, newCount, "count equals the list the same definition yields")
        }
    }
}
