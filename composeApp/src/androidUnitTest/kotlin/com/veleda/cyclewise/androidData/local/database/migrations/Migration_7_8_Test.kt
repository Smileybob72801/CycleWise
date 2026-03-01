package com.veleda.cyclewise.androidData.local.database.migrations

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test

/**
 * Unit tests for [Migration_7_8].
 *
 * Verifies that the migration:
 * 1. Creates `period_logs` with `entry_id` FK (CASCADE) to `daily_entries`.
 * 2. Creates a new `daily_entries` table without `flow_intensity` and `spotting` columns.
 * 3. Migrates non-null `flow_intensity` rows into `period_logs`.
 * 4. Copies remaining columns to the new table, drops old, and renames.
 */
class Migration_7_8_Test {

    private fun mockEmptyCursor(): Cursor {
        val cursor = mockk<Cursor>(relaxed = true)
        every { cursor.moveToFirst() } returns false
        return cursor
    }

    @Test
    fun migrate_WHEN_executed_THEN_createsPeriodLogsTable() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        every { db.query(any<String>()) } returns mockEmptyCursor()

        // ACT
        Migration_7_8.migrate(db)

        // ASSERT
        verify {
            db.execSQL(match {
                it.contains("CREATE TABLE IF NOT EXISTS `period_logs`") &&
                it.contains("`id` TEXT NOT NULL") &&
                it.contains("`entry_id` TEXT NOT NULL") &&
                it.contains("`flow_intensity` TEXT NOT NULL") &&
                it.contains("`created_at` INTEGER NOT NULL") &&
                it.contains("`updated_at` INTEGER NOT NULL") &&
                it.contains("FOREIGN KEY(`entry_id`) REFERENCES `daily_entries`(`id`)") &&
                it.contains("ON DELETE CASCADE")
            })
        }
    }

    @Test
    fun migrate_WHEN_executed_THEN_createsPeriodLogsIndex() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        every { db.query(any<String>()) } returns mockEmptyCursor()

        // ACT
        Migration_7_8.migrate(db)

        // ASSERT
        verify {
            db.execSQL(match { it.contains("CREATE INDEX") && it.contains("index_period_logs_entry_id") })
        }
    }

    @Test
    fun migrate_WHEN_executed_THEN_createsNewDailyEntriesWithoutFlowAndSpotting() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        every { db.query(any<String>()) } returns mockEmptyCursor()

        // ACT
        Migration_7_8.migrate(db)

        // ASSERT
        verify {
            db.execSQL(match {
                it.contains("CREATE TABLE `daily_entries_new`") &&
                !it.contains("`flow_intensity`") &&
                !it.contains("`spotting`") &&
                it.contains("`id` TEXT NOT NULL") &&
                it.contains("`entry_date` TEXT NOT NULL")
            })
        }
    }

    @Test
    fun migrate_WHEN_executed_THEN_followsTableRebuildPattern() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        every { db.query(any<String>()) } returns mockEmptyCursor()

        // ACT
        Migration_7_8.migrate(db)

        // ASSERT
        verifyOrder {
            db.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS `period_logs`") })
            db.execSQL(match { it.contains("CREATE TABLE `daily_entries_new`") })
            db.execSQL(match { it.contains("INSERT INTO `daily_entries_new`") && it.contains("SELECT") })
            db.execSQL(match { it.contains("DROP TABLE `daily_entries`") })
            db.execSQL(match { it.contains("ALTER TABLE `daily_entries_new` RENAME TO `daily_entries`") })
        }
    }

    @Test
    fun migrate_WHEN_flowDataExists_THEN_migratesRowsToPeriodLogs() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val cursor = mockk<Cursor>(relaxed = true)
        every { cursor.moveToFirst() } returns true
        every { cursor.moveToNext() } returns false
        every { cursor.getString(0) } returns "entry-1"
        every { cursor.getString(1) } returns "MEDIUM"
        every { cursor.getLong(2) } returns 1000L
        every { cursor.getLong(3) } returns 2000L
        every { db.query(match<String> { it.contains("flow_intensity IS NOT NULL") }) } returns cursor

        // ACT
        Migration_7_8.migrate(db)

        // ASSERT — should insert into period_logs with the flow data
        verify {
            db.execSQL(
                match<String> { it.contains("INSERT INTO period_logs") },
                match<Array<out Any?>> { args ->
                    args[1] == "entry-1" && args[2] == "MEDIUM" &&
                    args[3] == 1000L && args[4] == 2000L
                }
            )
        }
    }
}
