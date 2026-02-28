package com.veleda.cyclewise.androidData.local.database.migrations

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test

/**
 * Unit tests for [Migration_6_7].
 *
 * Verifies that the migration removes the `cycle_id` FK column from `daily_entries`
 * using the SQLite table-rebuild pattern (create new → copy → drop old → rename).
 */
class Migration_6_7_Test {

    @Test
    fun migrate_WHEN_executed_THEN_createsNewTableWithoutCycleId() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_6_7.migrate(db)

        // ASSERT
        verify {
            db.execSQL(match {
                it.contains("CREATE TABLE IF NOT EXISTS `daily_entries_new`") &&
                !it.contains("`cycle_id`") &&
                it.contains("`id` TEXT NOT NULL") &&
                it.contains("`entry_date` TEXT NOT NULL") &&
                it.contains("`flow_intensity` TEXT") &&
                it.contains("`spotting` INTEGER NOT NULL")
            })
        }
    }

    @Test
    fun migrate_WHEN_executed_THEN_followsTableRebuildPattern() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_6_7.migrate(db)

        // ASSERT — verify the 5-step pattern in order
        verifyOrder {
            db.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS `daily_entries_new`") })
            db.execSQL(match { it.contains("INSERT INTO `daily_entries_new`") && it.contains("SELECT") })
            db.execSQL(match { it.contains("DROP TABLE `daily_entries`") })
            db.execSQL(match { it.contains("ALTER TABLE `daily_entries_new` RENAME TO `daily_entries`") })
            db.execSQL(match { it.contains("CREATE INDEX") && it.contains("index_daily_entries_entry_date") })
        }
    }

    @Test
    fun migrate_WHEN_executed_THEN_copiesDataWithoutCycleIdColumn() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_6_7.migrate(db)

        // ASSERT — INSERT SELECT should not reference cycle_id
        verify {
            db.execSQL(match {
                it.contains("INSERT INTO `daily_entries_new`") &&
                !it.contains("cycle_id")
            })
        }
    }
}
