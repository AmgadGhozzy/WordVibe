package com.venom.stackcard.ui

import com.venom.data.local.entity.UserWordProgressEntity
import com.venom.data.local.entity.WordMasterEntity
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
 * Step 3b characterization written on the view model BEFORE the planner /
 * lifecycle extraction. The identical file ran green on 56e9010 (the
 * pre-extraction commit), pinning these expectations as the original
 * behavior — the extraction must delegate, never rewrite.
 *
 * The untouched P-01 suite (WordMasterySessionContractTest) stays the
 * durable-event authority; this file covers what 3b moves:
 *  - deck planning: due words first, then new words;
 *  - forgot tap: needs-review bump, processed count unchanged, card re-queued;
 *  - exit (BackToWelcome) and re-entry: clean counters, keys restart at :1;
 *  - regenerate: rebuilds the deck from the repository;
 *  - deck cleared: session finalized exactly once.
 */
class WordMasterySessionLifecycleContractTest {

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

    /** Rank-distinct word entity so the new-words serving order is deterministic. */
    private fun wordEntity(id: Int, rank: Int): WordMasterEntity =
        WordMasterEntity(id = id, wordEn = "w$id", pos = "verb", cefrLevel = "B1", rank = rank)

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

    private suspend fun plantDueWord(id: Int) {
        harness.wordDao.insert(testWordEntity(id))
        harness.progressDao.upsert(
            UserWordProgressEntity(
                userId = harness.userId,
                wordId = id,
                nextReview = System.currentTimeMillis() - 1_000
            )
        )
    }

    @Test
    fun `session deck serves due words first then new words`() {
        kotlinx.coroutines.runBlocking {
            harness.wordDao.insert(wordEntity(101, rank = 300))
            harness.wordDao.insert(wordEntity(102, rank = 200))
            harness.wordDao.insert(wordEntity(103, rank = 100))
            plantDueWord(7)

            val vm = viewModel()

            assertEquals(
                listOf(7, 103, 102, 101),
                vm.uiState.value.visibleCards.map { it.id },
                "due word first, then new words in serving (rank) order"
            )
        }
    }

    @Test
    fun `forgot tap bumps needs review without advancing the processed count`() {
        kotlinx.coroutines.runBlocking {
            val vm = viewModel()
            val word = testWord(7)

            vm.onEvent(WordMasteryEvent.SwipeCard(word, isRemembered = false))
            harness.await(scheduler) {
                harness.quizDao.events.count { it.wordId == 7 && !it.isCorrect } == 1
            }

            val s = vm.uiState.value
            assertEquals(1, s.sessionStats.needsReviewCount)
            assertEquals(0, s.processedCardsCount, "forgot never advances the processed count")
            assertEquals(listOf(7), s.visibleCards.map { it.id }, "the forgot card re-queues")
            assertTrue(s.removedCards.isEmpty(), "a forgot card never joins removed")
        }
    }

    @Test
    fun `back to welcome resets the session and re-entry starts keys at ordinal 1`() {
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
            val firstKeyPrefix = harness.quizDao.events.first().idempotencyKey!!.split(":")[0]

            vm.onEvent(WordMasteryEvent.BackToWelcome)
            harness.await(scheduler) {
                val s = vm.uiState.value
                s.visibleCards.isEmpty() && s.removedCards.isEmpty() &&
                    s.processedCardsCount == 0 && s.currentWord == null
            }

            // Re-entry on the same VM: word 8 plants as due, so it leads the new deck.
            plantDueWord(8)
            vm.onEvent(WordMasteryEvent.Initialize(isGenerative = false, topic = null))
            harness.await(scheduler) { vm.uiState.value.currentWord?.id == 8 }

            vm.onEvent(WordMasteryEvent.SwipeCard(testWord(8), isRemembered = false))
            harness.await(scheduler) {
                harness.quizDao.events.any { it.wordId == 8 && !it.isCorrect }
            }

            val parts = harness.quizDao.events.last { it.wordId == 8 }
                .idempotencyKey!!.split(":")
            // SESSION_ID:WORD:OUTCOME:ORDINAL — a fresh session, counter restarted at 1.
            assertEquals("8", parts[1])
            assertEquals("AGAIN", parts[2])
            assertEquals("1", parts[3], "ordinal restarts at 1 after the exit")
            assertTrue(
                parts[0].toLong() >= firstKeyPrefix.toLong(),
                "the re-entry session id is a fresh timestamp, never the old keyspace"
            )
            assertEquals(
                1, vm.uiState.value.sessionStats.needsReviewCount,
                "session counters restart clean after BackToWelcome"
            )
        }
    }

    @Test
    fun `regenerate rebuilds the deck from the repository`() {
        kotlinx.coroutines.runBlocking {
            harness.wordDao.insert(wordEntity(8, rank = 1500))
            val vm = viewModel()
            val word = testWord(7)

            vm.onEvent(WordMasteryEvent.SwipeCard(word, isRemembered = true))
            harness.await(scheduler) {
                harness.quizDao.events.count { it.wordId == 7 && it.isCorrect } == 1
            }
            val s = vm.uiState.value
            if (s.visibleCards.map { it.id } != listOf(8)) {
                throw AssertionError(
                    "deck=${s.visibleCards.map { it.id }} initial=${s.initialCardCount}"
                        + " removed=${s.removedCards.map { it.id }} err=${s.error}"
                        + " events=${harness.quizDao.events.map { it.wordId to it.isCorrect }}"
                )
            }

            vm.onEvent(WordMasteryEvent.RegenerateWords)
            harness.await(scheduler) {
                vm.uiState.value.visibleCards.map { it.id }.sorted() == listOf(7, 8)
            }
        }
    }

    @Test
    fun `clearing the deck finalizes the session exactly once`() {
        kotlinx.coroutines.runBlocking {
            harness.wordDao.insert(wordEntity(8, rank = 1500))
            val vm = viewModel()

            vm.onEvent(WordMasteryEvent.SwipeCard(testWord(7), isRemembered = true))
            harness.await(scheduler) {
                harness.quizDao.events.count { it.wordId == 7 && it.isCorrect } == 1
            }
            Thread.sleep(10) // keep the stamped duration strictly positive
            vm.onEvent(WordMasteryEvent.SwipeCard(testWord(8), isRemembered = true))
            harness.await(scheduler) {
                harness.quizDao.events.count { it.wordId == 8 && it.isCorrect } == 1
            }
            harness.await(scheduler) { harness.activityDao.getTotalSessionCount("u1") == 1 }
            scheduler.advanceUntilIdle()
            Thread.sleep(200)
            scheduler.advanceUntilIdle()

            assertEquals(
                1, harness.activityDao.getTotalSessionCount("u1"),
                "the cleared deck finalizes the session once — nothing more"
            )
            assertEquals(2, harness.quizDao.events.size, "two taps, two events")
            assertTrue(vm.uiState.value.sessionStats.sessionDurationMs > 0)
        }
    }
}
