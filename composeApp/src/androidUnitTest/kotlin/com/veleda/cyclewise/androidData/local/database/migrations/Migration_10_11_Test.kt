package com.veleda.cyclewise.androidData.local.database.migrations

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test

/**
 * Unit tests for [Migration_10_11].
 *
 * Verifies that the migration executes the correct SQL statements to rebuild
 * the `period_logs` table with a nullable `flow_intensity` column (no NOT NULL).
 */
class Migration_10_11_Test {

    @Test
    fun migrate_WHEN_executed_THEN_rebuildsPeriodLogsWithNullableFlowIntensity() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_10_11.migrate(db)

        // ASSERT — verify the table rebuild sequence
        verifyOrder {
            db.execSQL(match {
                it.contains("CREATE TABLE IF NOT EXISTS `period_logs_new`") &&
                it.contains("`flow_intensity` TEXT") &&
                !it.contains("`flow_intensity` TEXT NOT NULL")
            })
            db.execSQL(match {
                it.contains("INSERT INTO `period_logs_new`") &&
                it.contains("COALESCE(`created_at`, 0)") &&
                it.contains("COALESCE(`updated_at`, 0)")
            })
            db.execSQL(match { it.contains("DROP TABLE `period_logs`") })
            db.execSQL(match { it.contains("ALTER TABLE `period_logs_new` RENAME TO `period_logs`") })
            db.execSQL(match { it.contains("CREATE INDEX") && it.contains("index_period_logs_entry_id") })
        }
    }

    @Test
    fun migrate_WHEN_executed_THEN_newTableHasForeignKeyConstraint() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_10_11.migrate(db)

        // ASSERT — verify FK and CASCADE in the CREATE statement
        verify {
            db.execSQL(match {
                it.contains("FOREIGN KEY(`entry_id`) REFERENCES `daily_entries`(`id`)") &&
                it.contains("ON DELETE CASCADE")
            })
        }
    }

    @Test
    fun migrate_WHEN_executed_THEN_recreatesEntryIdIndex() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_10_11.migrate(db)

        // ASSERT
        verify {
            db.execSQL(match {
                it.contains("CREATE INDEX IF NOT EXISTS `index_period_logs_entry_id` ON `period_logs`(`entry_id`)")
            })
        }
    }
}
