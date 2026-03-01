package com.veleda.cyclewise.androidData.local.database.migrations

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verifyOrder
import org.junit.Test

/**
 * Unit tests for [Migration_1_2].
 *
 * Verifies that the migration creates the `daily_entries` table with all required columns,
 * the `cycle_id` FK to `periods`, and indexes on `cycle_id` and `entry_date`.
 */
class Migration_1_2_Test {

    @Test
    fun migrate_WHEN_executed_THEN_createsDailyEntriesTableWithCorrectSchema() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_1_2.migrate(db)

        // ASSERT
        verifyOrder {
            db.execSQL(match {
                it.contains("CREATE TABLE IF NOT EXISTS `daily_entries`") &&
                it.contains("`id` TEXT NOT NULL") &&
                it.contains("`cycle_id` TEXT NOT NULL") &&
                it.contains("`entry_date` TEXT NOT NULL") &&
                it.contains("`day_in_cycle` INTEGER NOT NULL") &&
                it.contains("`flow_intensity` TEXT") &&
                it.contains("`mood_score` INTEGER") &&
                it.contains("`energy_level` INTEGER") &&
                it.contains("`libido_level` TEXT") &&
                it.contains("`spotting` INTEGER NOT NULL") &&
                it.contains("`custom_tags` TEXT NOT NULL") &&
                it.contains("`cycle_phase` TEXT") &&
                it.contains("`created_at` INTEGER NOT NULL") &&
                it.contains("`updated_at` INTEGER NOT NULL") &&
                it.contains("PRIMARY KEY(`id`)") &&
                it.contains("FOREIGN KEY(`cycle_id`) REFERENCES `periods`(`uuid`)")
            })
            db.execSQL(match { it.contains("CREATE INDEX") && it.contains("index_daily_entries_cycle_id") })
            db.execSQL(match { it.contains("CREATE INDEX") && it.contains("index_daily_entries_entry_date") })
        }
    }

    @Test
    fun migrate_WHEN_executed_THEN_createsCycleIdForeignKey() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_1_2.migrate(db)

        // ASSERT
        io.mockk.verify {
            db.execSQL(match {
                it.contains("FOREIGN KEY(`cycle_id`) REFERENCES `periods`(`uuid`)") &&
                it.contains("ON DELETE CASCADE")
            })
        }
    }
}
