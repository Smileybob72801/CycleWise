package com.veleda.cyclewise.androidData.local.database.migrations

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test

/**
 * Unit tests for [Migration_9_10].
 *
 * Verifies that the migration executes the correct SQL statements in order:
 * 1. Creates `daily_entries_new` with `libido_score INTEGER` replacing `libido_level TEXT`.
 * 2. Copies data with CASE-mapped libido values (LOW->2, MEDIUM->3, HIGH->4, NULL->NULL).
 * 3. Drops old table, renames new table, recreates index.
 * 4. Adds `period_color` and `period_consistency` columns to `period_logs`.
 */
class Migration_9_10_Test {

    @Test
    fun migrate_WHEN_executed_THEN_rebuildsDailyEntriesTable() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_9_10.migrate(db)

        // ASSERT — verify the table rebuild sequence
        verifyOrder {
            db.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS `daily_entries_new`") && it.contains("libido_score") })
            db.execSQL(match { it.contains("INSERT INTO `daily_entries_new`") && it.contains("CASE libido_level") })
            db.execSQL(match { it.contains("DROP TABLE `daily_entries`") })
            db.execSQL(match { it.contains("ALTER TABLE `daily_entries_new` RENAME TO `daily_entries`") })
            db.execSQL(match { it.contains("CREATE INDEX") && it.contains("index_daily_entries_entry_date") })
        }
    }

    @Test
    fun migrate_WHEN_executed_THEN_mapsLibidoValuesCorrectly() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_9_10.migrate(db)

        // ASSERT — verify the CASE mapping in the INSERT statement
        verify {
            db.execSQL(match {
                it.contains("WHEN 'LOW' THEN 2") &&
                it.contains("WHEN 'MEDIUM' THEN 3") &&
                it.contains("WHEN 'HIGH' THEN 4") &&
                it.contains("ELSE NULL")
            })
        }
    }

    @Test
    fun migrate_WHEN_executed_THEN_addsPeriodColorAndConsistencyColumns() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_9_10.migrate(db)

        // ASSERT
        verify { db.execSQL(match { it.contains("ALTER TABLE `period_logs` ADD COLUMN `period_color` TEXT") }) }
        verify { db.execSQL(match { it.contains("ALTER TABLE `period_logs` ADD COLUMN `period_consistency` TEXT") }) }
    }
}
