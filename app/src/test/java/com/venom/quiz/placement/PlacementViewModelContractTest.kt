package com.venom.quiz.placement

import com.venom.analytics.StubCrashlyticsManager
import com.venom.data.local.entity.WordMasterEntity
import com.venom.domain.model.QuizQuestionUi
import com.venom.domain.model.WordLevels
import com.venom.harness.FakeIdentityRepository
import com.venom.harness.FakeProfileDao
import com.venom.harness.FakeWordDao
import com.venom.harness.FakeWordMasterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * ADR-0010 placement-path contracts over the approved corpus-floor ladder:
 * a corpus without A1 serves questions from its floor, abandoning the test
 * persists nothing, completion stores the estimated level, and the cap at B2
 * holds across the shifted ladder.
 */
class PlacementViewModelContractTest {

    private val mainDispatcher = UnconfinedTestDispatcher()
    private val scheduler get() = mainDispatcher.scheduler

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun wordEntity(id: Int, cefr: String) = WordMasterEntity(
        id = id,
        wordEn = "word$id",
        cefrLevel = cefr,
        rank = id,
        definitionEn = "definition $id",
        arabicAr = "معنى $id"
    )

    private class Rig(words: List<WordMasterEntity>) {
        val profileDao = FakeProfileDao()
        val vm = PlacementViewModel(
            wordRepository = FakeWordMasterRepository(FakeWordDao(words)),
            userProfileDao = profileDao,
            identityRepository = FakeIdentityRepository("u1"),
            crashlyticsManager = StubCrashlyticsManager()
        )
    }

    /** Advances the VM's virtual time while yielding to its real Dispatchers.IO work. */
    private suspend fun awaitVm(
        timeoutMs: Long = 5_000,
        cond: () -> Boolean,
        message: String
    ) {
        val start = System.currentTimeMillis()
        while (!cond()) {
            if (System.currentTimeMillis() - start > timeoutMs) throw AssertionError(message)
            scheduler.advanceUntilIdle()
            delay(10)
        }
        scheduler.advanceUntilIdle()
    }

    private suspend fun Rig.awaitQuestion() =
        awaitVm(cond = { vm.uiState.value.currentQuestion != null }, message = "no question served")

    private suspend fun Rig.awaitResult() =
        awaitVm(cond = { vm.result.value != null }, message = "no placement result emitted")

    private val Rig.correctAnswer: String?
        get() = (vm.uiState.value.currentQuestion as? QuizQuestionUi.FlashcardQuestion)
            ?.input?.correctAnswer

    private fun Rig.wrongAnswer(): String {
        val question = vm.uiState.value.currentQuestion as QuizQuestionUi.FlashcardQuestion
        return question.input.options.first { it != question.input.correctAnswer }
    }

    /**
     * runBlocking, deliberately not runTest: other suites' harness DataStore writes
     * can surface as process-level uncaught exceptions, which runTest fails on at
     * enter; runBlocking is immune. Virtual time for the VM's Main dispatcher still
     * advances via its own scheduler below.
     */
    private fun vmTest(test: suspend () -> Unit) = runBlocking { test() }

    @Test
    fun `a corpus without A1 serves its first question from the floor level`() = vmTest {
        val rig = Rig((11..14).map { wordEntity(it, "B1") })
        rig.vm.start()
        rig.awaitQuestion()

        val cefr = (rig.vm.uiState.value.currentQuestion as QuizQuestionUi.FlashcardQuestion)
            .input.cefrLevel
        assertEquals("B1", cefr, "the ladder starts at the lowest level with words")
    }

    @Test
    fun `abandoning the test persists nothing`() = vmTest {
        val rig = Rig((11..14).map { wordEntity(it, "B1") })
        rig.vm.start()
        rig.awaitQuestion()

        rig.vm.skip()
        scheduler.advanceUntilIdle()

        assertNull(rig.vm.result.value, "abandoning never emits a result")
        assertTrue(rig.vm.uiState.value.isAbandoned, "the UI is told the test was abandoned")
        assertNull(
            rig.profileDao.store["u1"]?.placementCefr,
            "no placement result reaches the profile"
        )
    }

    @Test
    fun `wrong answer on the first question assigns the level below the floor`() = vmTest {
        val rig = Rig((11..14).map { wordEntity(it, "B1") })
        rig.vm.start()
        rig.awaitQuestion()

        rig.vm.selectOption(rig.wrongAnswer())
        rig.awaitResult()

        val result = rig.vm.result.value!!
        assertEquals(WordLevels.Beginner, result.assignedLevel, "the level below B1 is A1")
        assertEquals("A1", result.estimatedCefr)
    }

    @Test
    fun `completion persists the estimated level to the profile`() = vmTest {
        val rig = Rig((11..14).map { wordEntity(it, "B1") })
        rig.vm.start()
        rig.awaitQuestion()

        rig.vm.selectOption(rig.wrongAnswer())
        rig.awaitResult()
        awaitVm(
            cond = { rig.profileDao.store["u1"]?.placementCefr != null },
            message = "placement never persisted"
        )

        assertEquals("A1", rig.profileDao.store["u1"]?.placementCefr)
    }

    @Test
    fun `a thin level borrows from the nearest neighbor instead of ending the test`() = vmTest {
        // B1 is rich, B2 has a single word: the ladder's second question must still
        // be served for B2, with its distractors borrowed from the neighbor.
        val rig = Rig((11..14).map { wordEntity(it, "B1") } + listOf(wordEntity(21, "B2")))
        rig.vm.start()

        awaitVm(
            cond = {
                val q = rig.vm.uiState.value.currentQuestion
                    as? QuizQuestionUi.FlashcardQuestion
                q != null && !rig.vm.uiState.value.isAnswered && q.input.cefrLevel == "B1"
            },
            message = "the floor question was never served"
        )
        rig.vm.selectOption(rig.correctAnswer!!)

        awaitVm(
            cond = {
                val q = rig.vm.uiState.value.currentQuestion
                    as? QuizQuestionUi.FlashcardQuestion
                q != null && !rig.vm.uiState.value.isAnswered && q.input.cefrLevel == "B2"
            },
            message = "the B2 question was never served from a borrowed pool"
        )
    }

    @Test
    fun `passing every ladder question caps the result at B2 and persists it`() = vmTest {
        // Every floor-ladder level has usable words, so no borrowing is involved here.
        val rig = Rig(
            (11..14).map { wordEntity(it, "B1") } +
                (21..24).map { wordEntity(it, "B2") } +
                (31..34).map { wordEntity(it, "C1") } +
                (41..44).map { wordEntity(it, "C2") }
        )
        rig.vm.start()

        // Floor ladder B1..C2, one question each; answer correct all the way. Each
        // round waits for a NEW unanswered question, not just any current question.
        var lastCefr = ""
        repeat(4) {
            awaitVm(
                cond = {
                    val q = rig.vm.uiState.value.currentQuestion
                        as? QuizQuestionUi.FlashcardQuestion
                    q != null && !rig.vm.uiState.value.isAnswered && q.input.cefrLevel != lastCefr
                },
                message = "next ladder question never served"
            )
            val q = rig.vm.uiState.value.currentQuestion as QuizQuestionUi.FlashcardQuestion
            lastCefr = q.input.cefrLevel.orEmpty()
            rig.vm.selectOption(q.input.correctAnswer)
        }
        rig.awaitResult()

        val result = rig.vm.result.value!!
        assertEquals("B2", result.estimatedCefr, "C1/C2 stay progression-only")
        awaitVm(
            cond = { rig.profileDao.store["u1"]?.placementCefr != null },
            message = "placement never persisted"
        )
        assertEquals("B2", rig.profileDao.store["u1"]?.placementCefr)
    }
}
