package com.venom.data.repo

import com.venom.harness.VmSessionHarness
import com.venom.harness.testWordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Ticket 14 (production-analog audit): the day-start chain
 * (`ensureTodayExists` → row creation → `updateStreakOnNewDay`) previously
 * ran un-serialized — two concurrent first-write-of-day callers both passed
 * the null check and could double the milestone / first-session bonus XP.
 *
 * Contract under test (PM round-13 item 1): two concurrent callers on the
 * day's first write MUST NOT double any XP — exactly one day-start XP ledger
 * per day regardless of interleaving.
 *
 * RED status note (honest): on the pre-fix code the doubling is a genuine
 * data race — it reproduces intermittently, not deterministically. The fix
 * (INSERT OR IGNORE on the composite (userId, dateKey) key + day-start chain
 * gated on "this caller created the row") makes the doubling structurally
 * impossible.
 */
class EnsureTodayExistsIdempotencyTest {

    private lateinit var harness: VmSessionHarness

    @BeforeEach
    fun setUp() {
        harness = VmSessionHarness(
            words = listOf(
                testWordEntity(7),
                testWordEntity(8),
                testWordEntity(9)
            )
        )
    }

    @AfterEach
    fun tearDown() {
        harness.close()
    }

    @Test
    fun `concurrent first-write-of-day callers never double the day-start XP`() {
        runBlocking {
            val repo = harness.activityRepository
            // A first activity row for another day must not interfere — this
            // test only races the day-start of TODAY.
            val rounds = 25
            repeat(rounds) { round ->
                val userId = "u1"
                harness.activityDao.upsert(
                    com.venom.data.local.entity.UserActivityEntity(
                        userId = userId,
                        dateKey = "1980-01-0$((round % 9) + 1)" // unrelated past days
                    )
                )
                val bonusBefore = harness.xpDao.events
                    .count { it.source == "first_session_bonus" }
                val milestoneBefore = harness.xpDao.events
                    .count { it.source.startsWith("streak_milestone") }

                coroutineScope {
                    val a = async(Dispatchers.IO) { repo.ensureTodayExists(userId) }
                    val b = async(Dispatchers.IO) { repo.ensureTodayExists(userId) }
                    awaitAll(a, b)
                }

                val bonusAfter = harness.xpDao.events
                    .count { it.source == "first_session_bonus" }
                val milestoneAfter = harness.xpDao.events
                    .count { it.source.startsWith("streak_milestone") }

                // Day-start XP fires once per day, exactly once — race or not.
                assertTrue(
                    bonusAfter <= bonusBefore + 1,
                    "round $round: first-session bonus duplicated " +
                        "($bonusBefore → $bonusAfter)"
                )
                assertTrue(
                    milestoneAfter <= milestoneBefore + 1,
                    "round $round: milestone XP duplicated " +
                        "($milestoneBefore → $milestoneAfter)"
                )
                // The day row is created ONCE (round 0); later concurrent
                // callers must add zero additional day-start bonus.
                if (round == 0) {
                    assertEquals(bonusBefore + 1, bonusAfter, "round 0 creates the day bonus exactly once")
                } else {
                    assertEquals(bonusBefore, bonusAfter, "round $round: no additional day-start bonus")
                }
            }
            // Global invariant: the bonus is a once-per-user-day event.
            assertEquals(
                1, harness.xpDao.events.count { it.source == "first_session_bonus" },
                "one first-session bonus across the whole race session"
            )
        }
    }
}
