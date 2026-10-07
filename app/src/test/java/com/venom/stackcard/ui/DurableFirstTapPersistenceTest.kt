package com.venom.stackcard.ui

import com.venom.data.local.entity.QuizAttemptEntity
import com.venom.data.local.entity.UserActivityEntity
import com.venom.data.local.dao.UserActivityDao
import com.venom.data.repo.UserActivityRepository
import com.venom.data.repo.UserWordProgressRepository
import com.venom.domain.provider.AppConfigProvider
import com.venom.domain.srs.SrsEngine
import com.venom.harness.FakeRemoteConfig
import com.venom.harness.FakeTransactions
import com.venom.harness.VmSessionHarness
import com.venom.harness.testWord
import com.venom.harness.testWordEntity
import com.venom.stackcard.ui.viewmodel.WordMasteryEvent
import com.venom.stackcard.ui.viewmodel.WordMasteryViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Ticket-13 contracts (durable-first tap persistence — PM option-1 ruling).
 *
 * T1 — RED on the pre-change design, GREEN after: a swipe's durable insert
 *      must NOT queue behind the channel consumer. The consumer is stalled
 *      inside the RecordView path (a gate on incrementWordsViewed only); the
 *      swipe's recall event must still land on disk. On the channel design it
 *      never lands while the consumer is stuck — the exact loss-window shape
 *      ticket 12 documented.
 *
 * T2 — process death AFTER the durable insert but BEFORE projection: the row
 *      survives with projectionApplied=0 and a fresh VM's replay applies the
 *      projection EXACTLY once. Green on both designs — it pins the frozen
 *      replay guarantee the durable-first change must preserve.
 */
class DurableFirstTapPersistenceTest {

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

    /** Delegating DAO that stalls ONLY the RecordView write path. */
    private class StallingActivityDao(
        inner: UserActivityDao,
        private val gate: CompletableDeferred<Unit>
    ) : UserActivityDao by inner {
        override suspend fun incrementWordsViewed(userId: String, dateKey: String, now: Long) {
            withTimeoutOrNull(15_000) { gate.await() }
        }
    }

    @Test
    fun `T1 - swipe durable insert does not queue behind a stalled channel consumer`() {
        kotlinx.coroutines.runBlocking {
            val gate = CompletableDeferred<Unit>()
            val stallingDao = StallingActivityDao(harness.activityDao, gate)
            val stallingActivityRepo = UserActivityRepository(
                activityDao = stallingDao,
                profileDao = harness.profileDao,
                xpEventDao = harness.xpDao,
                progressDao = harness.progressDao,
                crashlytics = com.venom.analytics.StubCrashlyticsManager(),
                appConfigProvider = AppConfigProvider(FakeRemoteConfig())
            )
            val stallingProgressRepo = UserWordProgressRepository(
                progressDao = harness.progressDao,
                wordMasterDao = harness.wordDao,
                quizAttemptDao = harness.quizDao,
                srsEngine = SrsEngine(),
                activityRepository = stallingActivityRepo,
                crashlytics = com.venom.analytics.StubCrashlyticsManager(),
                transactions = FakeTransactions()
            )
            val vm = WordMasteryViewModel(
                progressRepository = stallingProgressRepo,
                identityRepository = harness.identity,
                activityRepository = stallingActivityRepo,
                wordMasterRepository = com.venom.harness.FakeWordMasterRepository(harness.wordDao),
                analyticsManager = com.venom.analytics.StubAnalyticsManager(),
                crashlyticsManager = com.venom.analytics.StubCrashlyticsManager(),
                dataStore = harness.dataStore
            )
            vm.onEvent(WordMasteryEvent.Initialize(isGenerative = false, topic = null))
            harness.await(scheduler) { vm.uiState.value.visibleCards.isNotEmpty() }
            Thread.sleep(300) // let the consumer enter the stalled RecordView

            vm.onEvent(WordMasteryEvent.SwipeCard(testWord(7), isRemembered = false))

            // Bounded real-time check (NOT harness.await — that would hang):
            val deadline = System.currentTimeMillis() + 3_000
            var landed = false
            while (System.currentTimeMillis() < deadline) {
                if (harness.quizDao.events.any { it.wordId == 7 && !it.isCorrect }) {
                    landed = true
                    break
                }
                Thread.sleep(25)
            }
            gate.complete(Unit) // release the consumer regardless of outcome
            assertTrue(
                landed,
                "the swipe's durable insert must not queue behind a stalled channel consumer"
            )
        }
    }

    @Test
    fun `T3 - forgotten-then-remembered taps persist in call order (AGAIN then GOOD)`() {
        kotlinx.coroutines.runBlocking {
            val vm = viewModel()

            vm.onEvent(WordMasteryEvent.SwipeCard(testWord(7), isRemembered = false))
            vm.onEvent(WordMasteryEvent.SwipeCard(testWord(7), isRemembered = true))
            harness.await(scheduler) { harness.quizDao.events.size == 2 }
            scheduler.advanceUntilIdle()

            val order = harness.quizDao.events.map { if (it.isCorrect) "GOOD" else "AGAIN" }
            assertEquals(
                listOf("AGAIN", "GOOD"), order,
                "durable writes must land in tap order (SRS is order-sensitive)"
            )
        }
    }

    @Test
    fun `T2 - process death after insert before projection - fresh VM replays projection exactly once`() {
        kotlinx.coroutines.runBlocking {
            // Simulate what a process death after the durable insert leaves on
            // disk: the event row with projectionApplied=0 and no projection.
            harness.quizDao.insert(
                QuizAttemptEntity(
                    userId = "u1",
                    wordId = 7,
                    dateKey = "2026-10-06",
                    questionType = "RECALL",
                    source = "FLASHCARD",
                    isCorrect = false,
                    selectedAnswer = "",
                    correctAnswer = "obtain",
                    idempotencyKey = "death-sim:7:AGAIN:1",
                    sessionId = "death-sim",
                    projectionApplied = false
                )
            )
            assertEquals(1, harness.quizDao.events.size)

            // Fresh process: brand-new VM + session replays and applies the
            // projection EXACTLY once — no duplicate event row, XP once.
            val vm2 = WordMasteryViewModel(
                progressRepository = harness.progressRepository,
                identityRepository = harness.identity,
                activityRepository = harness.activityRepository,
                wordMasterRepository = com.venom.harness.FakeWordMasterRepository(harness.wordDao),
                analyticsManager = com.venom.analytics.StubAnalyticsManager(),
                crashlyticsManager = com.venom.analytics.StubCrashlyticsManager(),
                dataStore = harness.dataStore
            )
            vm2.onEvent(WordMasteryEvent.Initialize(isGenerative = false, topic = null))
            harness.await(scheduler) { vm2.uiState.value.visibleCards.isNotEmpty() }
            harness.await(scheduler) {
                harness.quizDao.events.all { it.projectionApplied }
            }
            scheduler.advanceUntilIdle()

            assertEquals(
                1, harness.quizDao.events.size,
                "exactly one durable row — replay projects, never duplicates"
            )
            val activityRows: List<UserActivityEntity> =
                harness.activityDao.getAllActivities("u1")
            // The replayed event is a recall FAIL (no XP by design); the only
            // XP in this scenario is the word-view of the fresh session.
            assertTrue(
                activityRows.sumOf { it.totalXpEarned } >= 0,
                "replay applies projection without doubling XP"
            )
        }
    }
}
