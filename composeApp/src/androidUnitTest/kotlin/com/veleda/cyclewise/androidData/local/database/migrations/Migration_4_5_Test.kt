package com.veleda.cyclewise.androidData.local.database.migrations

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test

/**
 * Unit tests for [Migration_4_5].
 *
 * Verifies that the migration:
 * 1. Creates `medication_library` with a unique name index.
 * 2. Creates new `medication_logs` with FKs to `daily_entries` (CASCADE) and `medication_library` (RESTRICT).
 * 3. Drops the old `medications` table.
 */
class Migration_4_5_Test {

    @Test
    fun migrate_WHEN_executed_THEN_createsMedicationLibraryTable() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_4_5.migrate(db)

        // ASSERT
        verify {
            db.execSQL(match {
                it.contains("CREATE TABLE IF NOT EXISTS `medication_library`") &&
                it.contains("`id` TEXT NOT NULL") &&
                it.contains("`name` TEXT NOT NULL") &&
                it.contains("`created_at` INTEGER NOT NULL") &&
                it.contains("PRIMARY KEY(`id`)")
            })
        }
    }

    @Test
    fun migrate_WHEN_executed_THEN_createsUniqueNameIndexOnMedicationLibrary() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_4_5.migrate(db)

        // ASSERT
        verify {
            db.execSQL(match {
                it.contains("CREATE UNIQUE INDEX") && it.contains("index_medication_library_name")
            })
        }
    }

    @Test
    fun migrate_WHEN_executed_THEN_createsNewMedicationLogsTable() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_4_5.migrate(db)

        // ASSERT
        verify {
            db.execSQL(match {
                it.contains("CREATE TABLE IF NOT EXISTS `medication_logs`") &&
                it.contains("`id` TEXT NOT NULL") &&
                it.contains("`entry_id` TEXT NOT NULL") &&
                it.contains("`medication_id` TEXT NOT NULL") &&
                it.contains("`created_at` INTEGER NOT NULL") &&
                it.contains("FOREIGN KEY(`entry_id`) REFERENCES `daily_entries`(`id`)") &&
                it.contains("ON DELETE CASCADE") &&
                it.contains("FOREIGN KEY(`medication_id`) REFERENCES `medication_library`(`id`)") &&
                it.contains("ON DELETE RESTRICT")
            })
        }
    }

    @Test
    fun migrate_WHEN_executed_THEN_createsMedicationLogsIndexes() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_4_5.migrate(db)

        // ASSERT
        verify { db.execSQL(match { it.contains("CREATE INDEX") && it.contains("index_medication_logs_entry_id") }) }
        verify { db.execSQL(match { it.contains("CREATE INDEX") && it.contains("index_medication_logs_medication_id") }) }
    }

    @Test
    fun migrate_WHEN_executed_THEN_dropsOldMedicationsTable() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_4_5.migrate(db)

        // ASSERT
        verify { db.execSQL(match { it.contains("DROP TABLE IF EXISTS medications") }) }
    }
}
