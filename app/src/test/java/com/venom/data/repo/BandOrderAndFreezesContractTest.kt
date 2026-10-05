package com.venom.data.repo

import androidx.datastore.preferences.core.edit
import com.venom.data.local.PreferencesKeys
import com.venom.data.local.entity.UserActivityEntity
import com.venom.data.local.entity.UserProfileEntity
import com.venom.data.local.entity.WordMasterEntity
import com.venom.harness.VmSessionHarness
import com.venom.harness.testWordEntity
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * ADR-0010 contracts:
 *
 * - New-word serving honours the placement band: band words first (rank-ordered),
 *   then the rest — the frozen no-progress-row definition is unchanged.
 * - A freeze is earned at every exact 7-day honest streak, capped at 2, and a
 *   bridged day neither earns nor fabricates.
 */
class BandOrderAndFreezesContractTest {

    private lateinit var harness: VmSessionHarness
    private lateinit var scheduler: TestCoroutineScheduler

    @BeforeEach
    fun setUp() {
        val dispatcher = UnconfinedTestDispatcher()
        scheduler = dispatcher.scheduler
        Dispatchers.setMain(dispatcher)
        harness = VmSessionHarness(words = emptyList())
    }

    @AfterEach
    fun tearDown() {
        harness.close()
        Dispatchers.resetMain()
    }

    private fun dayKey(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    @Test
    fun `new words are served band-first then by rank`() {
        runBlocking {
            harness.wordDao.insertAll(
                listOf(
                    WordMasterEntity(id = 1, wordEn = "zeta", cefrLevel = "B2", rank = 1, frequency = 3),
                    WordMasterEntity(id = 2, wordEn = "obtain", cefrLevel = "B1", rank = 2, frequency = 3),
                    WordMasterEntity(id = 3, wordEn = "alpha", cefrLevel = "B2", rank = 3, frequency = 3),
                    WordMasterEntity(id = 4, wordEn = "yield", cefrLevel = "B1", rank = 4, frequency = 3),
                )
            )

            val bandFirst = harness.progressRepository.getNewWordsForUser("u1", 4, preferredCefr = "B2")
            assertEquals(
                listOf(1, 3, 2, 4), bandFirst.map { it.id },
                "B2 band first (rank-ordered), then everything else (rank-ordered)"
            )

            val noBand = harness.progressRepository.getNewWordsForUser("u1", 4)
            assertEquals(
                listOf(1, 2, 3, 4), noBand.map { it.id },
                "without a band: pure rank order (frozen definition)"
            )
        }
    }

    @Test
    fun `freeze is earned at the 7-day mark`() {
        runBlocking {
            // Six honest days before today; today's first activity completes 7.
            for (d in 1..6) {
                harness.activityDao.upsert(UserActivityEntity(userId = "u1", dateKey = dayKey(d)))
            }
            harness.activityRepository.ensureTodayExists("u1")

            val profile = harness.profileDao.store["u1"]!!
            assertEquals(7, profile.currentStreak, "streak completes to 7")
            assertEquals(1, profile.streakFreezes, "one freeze earned at the 7-day mark")
        }
    }

    @Test
    fun `freeze balance never exceeds two`() {
        runBlocking {
            harness.profileDao.upsert(UserProfileEntity(userId = "u1", streakFreezes = 2))
            for (d in 1..6) {
                harness.activityDao.upsert(UserActivityEntity(userId = "u1", dateKey = dayKey(d)))
            }
            harness.activityRepository.ensureTodayExists("u1")

            val profile = harness.profileDao.store["u1"]!!
            assertEquals(7, profile.currentStreak)
            assertEquals(2, profile.streakFreezes, "cap: no third freeze")
        }
    }

    @Test
    fun `a bridged day earns nothing`() {
        runBlocking {
            harness.profileDao.upsert(UserProfileEntity(userId = "u1", streakFreezes = 1))
            // Last activity two days ago: today consumes the lone freeze (bridge).
            harness.activityDao.upsert(UserActivityEntity(userId = "u1", dateKey = dayKey(2)))
            harness.activityRepository.ensureTodayExists("u1")

            val profile = harness.profileDao.store["u1"]!!
            // Frozen StreakEngine semantics: the bridge keeps the CHAIN alive, so
            // the count is the chain length (day-2 + synth + today = 3).
            assertEquals(3, profile.currentStreak, "bridge preserves continuity only")
            assertEquals(0, profile.streakFreezes, "the freeze was consumed")
            // A 3-day bridged chain is not a 7-day mark: no grant, no fabrication.
            assertEquals(0, profile.streakFreezes)
        }
    }
}
