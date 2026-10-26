package com.venom.stackcard.ui

import androidx.datastore.preferences.core.edit
import com.venom.data.local.PreferencesKeys
import com.venom.data.local.entity.UserProfileEntity
import com.venom.data.local.entity.UserWordProgressEntity
import com.venom.data.local.entity.WordMasterEntity
import com.venom.harness.FakeWordMasterRepository
import com.venom.harness.VmSessionHarness
import com.venom.stackcard.session.UnitGate
import com.venom.stackcard.ui.viewmodel.WordMasteryEvent
import com.venom.stackcard.ui.viewmodel.WordMasteryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NewWordUnitGatingTest {

    private lateinit var dispatcher: kotlinx.coroutines.test.TestDispatcher
    private lateinit var scheduler: TestCoroutineScheduler

    @BeforeEach
    fun setUp() {
        dispatcher = UnconfinedTestDispatcher()
        scheduler = dispatcher.scheduler
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeWord(
        id: Int,
        topic: String,
        cefr: String = "B1",
        rank: Int = 1000
    ): WordMasterEntity = WordMasterEntity(
        id = id,
        wordEn = "word$id",
        pos = "noun",
        cefrLevel = cefr,
        rank = rank,
        frequency = 3,
        difficultyScore = 5,
        phoneticUs = "wɜːd",
        phoneticAr = "وورد",
        definitionEn = "Definition $id",
        arabicAr = "معنى $id",
        ieltsTopic = topic
    )

    private suspend fun buildViewModel(harness: VmSessionHarness): WordMasteryViewModel {
        val vm = WordMasteryViewModel(
            progressRepository = harness.progressRepository,
            identityRepository = harness.identity,
            activityRepository = harness.activityRepository,
            wordMasterRepository = FakeWordMasterRepository(harness.wordDao),
            analyticsManager = com.venom.analytics.StubAnalyticsManager(),
            crashlyticsManager = com.venom.analytics.StubCrashlyticsManager(),
            dataStore = harness.dataStore
        )
        vm.onEvent(WordMasteryEvent.Initialize(isGenerative = false, topic = null))
        harness.await(scheduler) { vm.uiState.value.visibleCards.isNotEmpty() }
        return vm
    }

    @Test
    fun `first unit Education is open by default for unplaced user and persisted in DataStore`() = runTest {
        val words = listOf(
            makeWord(1, "Education", "B1", 100),
            makeWord(2, "Education", "B1", 200),
            makeWord(3, "Work", "B1", 300)
        )
        val harness = VmSessionHarness(userId = "u1", words = words)
        try {
            val vm = buildViewModel(harness)
            val persisted = harness.dataStore.data.first()[PreferencesKeys.OPEN_UNIT]
            assertEquals("Education", persisted)
            val served = vm.uiState.value.visibleCards
            assertTrue(served.all { it.ieltsTopic.equals("Education", ignoreCase = true) })
            assertEquals(listOf(1, 2), served.map { it.id })
        } finally {
            harness.close()
        }
    }

    @Test
    fun `new words are drawn only from the open unit in placement band-first then rank order`() = runTest {
        val words = listOf(
            makeWord(1, "Education", "B2", 200),
            makeWord(2, "Education", "B1", 100),
            makeWord(3, "Education", "B1", 300),
            makeWord(4, "Work", "B1", 50)
        )
        val harness = VmSessionHarness(userId = "u1", words = words)
        try {
            harness.profileDao.upsert(UserProfileEntity(userId = "u1", placementCefr = "B1"))
            val vm = buildViewModel(harness)
            val served = vm.uiState.value.visibleCards
            assertEquals(listOf(2, 3, 1), served.map { it.id })
            assertTrue(served.none { it.id == 4 })
        } finally {
            harness.close()
        }
    }

    @Test
    fun `due review words from locked units are still served unconditionally ahead of new words`() = runTest {
        val words = listOf(
            makeWord(1, "Education", "B1", 100),
            makeWord(2, "Work", "B1", 200)
        )
        val harness = VmSessionHarness(userId = "u1", words = words)
        try {
            harness.progressDao.upsert(
                UserWordProgressEntity(
                    userId = "u1",
                    wordId = 2,
                    knownState = "LEARNING",
                    nextReview = 1000L
                )
            )
            val vm = buildViewModel(harness)
            val served = vm.uiState.value.visibleCards
            assertEquals(2, served.size)
            assertEquals(2, served[0].id)
            assertEquals(1, served[1].id)
        } finally {
            harness.close()
        }
    }

    @Test
    fun `one below ceil 60 percent or LEARNING state keeps the next unit locked`() = runTest {
        val words = (1..5).map { makeWord(it, "Education", "B1", it * 10) } +
            listOf(makeWord(6, "Work", "B1", 100))
        val harness = VmSessionHarness(userId = "u1", words = words)
        try {
            harness.progressDao.upsert(UserWordProgressEntity(userId = "u1", wordId = 1, knownState = "KNOWN"))
            harness.progressDao.upsert(UserWordProgressEntity(userId = "u1", wordId = 2, knownState = "KNOWN"))

            val (unitSub60, _) = harness.progressRepository.resolveOpenUnitForUser("u1", null)
            assertEquals("Education", unitSub60)
            val reasonSub60 = UnitGate.lockReasonFor(2, 5)
            assertNotNull(reasonSub60)
            assertEquals(3, reasonSub60?.requiredKnownCount)
            assertEquals(1, reasonSub60?.remaining)

            harness.progressDao.upsert(UserWordProgressEntity(userId = "u1", wordId = 3, knownState = "LEARNING"))
            val (unitLearning, _) = harness.progressRepository.resolveOpenUnitForUser("u1", null)
            assertEquals("Education", unitLearning)
        } finally {
            harness.close()
        }
    }

    @Test
    fun `reaching ceil 60 percent KNOWN or MASTERED advances open unit to Work and persists it`() = runTest {
        val words = (1..5).map { makeWord(it, "Education", "B1", it * 10) } +
            (6..10).map { makeWord(it, "Work", "B1", it * 10) }
        val harness = VmSessionHarness(userId = "u1", words = words)
        try {
            harness.progressDao.upsert(UserWordProgressEntity(userId = "u1", wordId = 1, knownState = "KNOWN"))
            harness.progressDao.upsert(UserWordProgressEntity(userId = "u1", wordId = 2, knownState = "MASTERED"))
            harness.progressDao.upsert(UserWordProgressEntity(userId = "u1", wordId = 3, knownState = "KNOWN"))

            val vm = buildViewModel(harness)
            val persisted = harness.dataStore.data.first()[PreferencesKeys.OPEN_UNIT]
            assertEquals("Work", persisted)
            val served = vm.uiState.value.visibleCards
            assertTrue(served.all { it.ieltsTopic.equals("Work", ignoreCase = true) })
        } finally {
            harness.close()
        }
    }

    @Test
    fun `persisted open unit does not regress if a prior unit word later lapses to LEARNING`() = runTest {
        val words = (1..5).map { makeWord(it, "Education", "B1", it * 10) } +
            (6..10).map { makeWord(it, "Work", "B1", it * 10) }
        val harness = VmSessionHarness(userId = "u1", words = words)
        try {
            harness.dataStore.edit { prefs -> prefs[PreferencesKeys.OPEN_UNIT] = "Work" }

            harness.progressDao.upsert(UserWordProgressEntity(userId = "u1", wordId = 1, knownState = "KNOWN"))
            harness.progressDao.upsert(UserWordProgressEntity(userId = "u1", wordId = 2, knownState = "LEARNING"))
            harness.progressDao.upsert(UserWordProgressEntity(userId = "u1", wordId = 3, knownState = "LEARNING"))

            val (openUnit, _) = harness.progressRepository.resolveOpenUnitForUser("u1", "Work")
            assertEquals("Work", openUnit)

            val vm = buildViewModel(harness)
            val persisted = harness.dataStore.data.first()[PreferencesKeys.OPEN_UNIT]
            assertEquals("Work", persisted)
            val served = vm.uiState.value.visibleCards
            assertTrue(served.all { it.ieltsTopic.equals("Work", ignoreCase = true) })
        } finally {
            harness.close()
        }
    }

    @Test
    fun `UnitGate EDITORIAL_CHAIN matches the ten canonical ADR-0011 topics in exact order`() {
        val expected = listOf(
            "Education",
            "Work",
            "Health",
            "Travel",
            "Technology",
            "Environment",
            "Culture",
            "Society",
            "Economy",
            "Science"
        )
        assertEquals(expected, UnitGate.EDITORIAL_CHAIN)
        assertEquals(10, UnitGate.EDITORIAL_CHAIN.size)
        assertEquals(60, UnitGate.OPEN_RATIO_PERCENT)
    }
}
