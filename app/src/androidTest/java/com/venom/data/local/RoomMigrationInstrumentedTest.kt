package com.venom.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.venom.wordvibe.di.AppModule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class RoomMigrationInstrumentedTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WordVibeDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate1To2() {
        // Create DB in version 1
        var db = helper.createDatabase(TEST_DB, 1)

        // Insert some data using SQL to verify it survives.
        // userWordProgress in schema v1 requires NOT NULL on all progress counters.
        db.execSQL(
            "INSERT INTO wordsMaster (id, wordEn, cefrLevel, rank) VALUES (1, 'apple', 'A1', 10)"
        )
        db.execSQL(
            """
            INSERT INTO userWordProgress (
                userId, wordId, firstSeen, bookmarked,
                productionSuccess, recallSuccess, totalAttempts,
                knownState, difficulty, stability, lapsesCount
            ) VALUES (
                'user1', 1, 1000, 0,
                0, 0, 0,
                'NEW', 0, 0.0, 0
            )
            """.trimIndent()
        )

        // Close to allow migration
        db.close()

        // Re-open DB at version 2 using the production migration
        db = helper.runMigrationsAndValidate(
            TEST_DB,
            2,
            true,
            AppModule.MIGRATION_1_2
        )

        // Verify wordsMaster data survived
        db.query("SELECT id, wordEn, cefrLevel, rank FROM wordsMaster WHERE id = 1").use { cursor ->
            assertTrue("Word row should survive migration", cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
            assertEquals("apple", cursor.getString(1))
            assertEquals("A1", cursor.getString(2))
            assertEquals(10, cursor.getInt(3))
        }

        // Verify userWordProgress data survived
        db.query(
            "SELECT knownState, totalAttempts, firstSeen " +
                "FROM userWordProgress WHERE userId = 'user1' AND wordId = 1"
        ).use { progressCursor ->
            assertTrue("Progress row should survive migration", progressCursor.moveToFirst())
            assertEquals("NEW", progressCursor.getString(0))
            assertEquals(0, progressCursor.getInt(1))
            assertEquals(1000L, progressCursor.getLong(2))
        }

        // Verify indexes exist in sqlite_master
        db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND (name='index_wordsMaster_cefrLevel_rank' OR name='index_wordsMaster_rank')"
        ).use { indexCursor ->
            var count = 0
            while (indexCursor.moveToNext()) {
                count++
            }
            assertEquals("Both indexes should exist", 2, count)
        }
    }
}
