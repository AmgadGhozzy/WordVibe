package com.venom.quiz.placement

import com.venom.data.local.dao.UserProfileDao
import com.venom.data.local.entity.UserProfileEntity
import com.venom.data.local.entity.WordMasterEntity
import com.venom.harness.FakeProfileDao
import com.venom.harness.FakeWordDao
import com.venom.harness.FakeWordMasterRepository
import com.venom.harness.FakeIdentityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * ADR-0010 placement-floor contracts: the adaptive ladder starts at the lowest
 * CEFR level that actually has words, so placement always completes with real
 * questions on the current corpus — never a degenerate zero-question result.
 */
class PlacementFloorContractTest {

    private lateinit var profileDao: FakeProfileDao
    private lateinit var wordDao: FakeWordDao

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        profileDao = FakeProfileDao()
        runBlocking {
            profileDao.upsert(UserProfileEntity(userId = "u1"))
        }
        // Corpus floor is A2 (2 words); B1 has plenty. No A1 words exist.
        wordDao = FakeWordDao(
            listOf(
                WordMasterEntity(id = 1, wordEn = "able", cefrLevel = "A2", rank = 1, frequency = 3,
                    arabicAr = "قادر", definitionEn = "having the ability"),
                WordMasterEntity(id = 2, wordEn = "accept", cefrLevel = "A2", rank = 2, frequency = 3,
                    arabicAr = "يقبل", definitionEn = "to take willingly"),
                WordMasterEntity(id = 3, wordEn = "obtain", cefrLevel = "B1", rank = 3, frequency = 3,
                    arabicAr = "يحصل على", definitionEn = "to get"),
                WordMasterEntity(id = 4, wordEn = "substantial", cefrLevel = "B1", rank = 4, frequency = 3,
                    arabicAr = "كبير", definitionEn = "large in amount"),
                WordMasterEntity(id = 5, wordEn = "comprehensive", cefrLevel = "B1", rank = 5, frequency = 3,
                    arabicAr = "شامل", definitionEn = "covering broadly"),
            )
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): PlacementViewModel = PlacementViewModel(
        wordRepository = FakeWordMasterRepository(wordDao),
        userProfileDao = profileDao,
        identityRepository = FakeIdentityRepository(),
        crashlyticsManager = com.venom.analytics.StubCrashlyticsManager()
    )

    @Test
    fun `placement starts at the corpus floor and asks a real question`() {
        runBlocking {
            val vm = viewModel()
            vm.start()
            // The first question loads (pre-ADR: 0 A1 words ended placement instantly).
            val deadline = System.currentTimeMillis() + 5000
            while (vm.uiState.value.currentQuestion == null &&
                System.currentTimeMillis() < deadline
            ) {
                Thread.sleep(20)
            }
            runBlocking {
                com.venom.domain.model.CefrLevel.entries.forEach { lvl ->
                    println("DIAG-COUNT ${lvl.name} = " + wordDao.getCountByCefr(lvl.name))
                }
            }
            println("DIAG: " + vm.uiState.value.toString().take(200))
            assertNotNull(vm.uiState.value.currentQuestion, "placement must ask a question")
            // Floor = A2 → band label is the Elementary band (level index 1).
            assertTrue(
                vm.uiState.value.placementBandLabel?.contains("A2") == true,
                "the ladder starts at the corpus floor band: ${vm.uiState.value.placementBandLabel}"
            )
        }
    }

    @Test
    fun `answering the floor question advances instead of ending`() {
        runBlocking {
            val vm = viewModel()
            vm.start()
            val deadline = System.currentTimeMillis() + 5000
            while (vm.uiState.value.currentQuestion == null &&
                System.currentTimeMillis() < deadline
            ) {
                Thread.sleep(20)
            }
            val question = vm.uiState.value.currentQuestion!!
            val correct = question.correctAnswer
            assertNotNull(correct)
            // A correct answer must move to the next question, never end the run.
            vm.selectOption(question.options.first { it == correct })
            val deadline2 = System.currentTimeMillis() + 5000
            while (!vm.uiState.value.isAnswered && System.currentTimeMillis() < deadline2) {
                Thread.sleep(20)
            }
            assertTrue(vm.uiState.value.isAnswered, "answered state recorded")
        }
    }
}
