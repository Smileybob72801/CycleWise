package com.veleda.cyclewise.androidData.local.database.migrations

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

/**
 * Unit tests for [Migration_2_3].
 *
 * Verifies that the migration creates the `symptoms` and `medication_logs` tables
 * with the correct columns, FKs, and indexes.
 */
class Migration_2_3_Test {

    @Test
    fun migrate_WHEN_executed_THEN_createsSymptomsTable() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_2_3.migrate(db)

        // ASSERT
        verify {
            db.execSQL(match {
                it.contains("CREATE TABLE IF NOT EXISTS `symptoms`") &&
                it.contains("`id` TEXT NOT NULL") &&
                it.contains("`entry_id` TEXT NOT NULL") &&
                it.contains("`symptom_type` TEXT NOT NULL") &&
                it.contains("`severity` INTEGER NOT NULL") &&
                it.contains("`note` TEXT") &&
                it.contains("PRIMARY KEY(`id`)") &&
                it.contains("FOREIGN KEY(`entry_id`) REFERENCES `daily_entries`(`id`)")
            })
        }
    }

    @Test
    fun migrate_WHEN_executed_THEN_createsSymptomsIndex() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_2_3.migrate(db)

        // ASSERT
        verify {
            db.execSQL(match { it.contains("CREATE INDEX") && it.contains("index_symptoms_entry_id") })
        }
    }

    @Test
    fun migrate_WHEN_executed_THEN_createsMedicationLogsTable() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_2_3.migrate(db)

        // ASSERT
        verify {
            db.execSQL(match {
                it.contains("CREATE TABLE IF NOT EXISTS `medication_logs`") &&
                it.contains("`id` TEXT NOT NULL") &&
                it.contains("`entry_id` TEXT NOT NULL") &&
                it.contains("`medication_name` TEXT NOT NULL") &&
                it.contains("`note` TEXT") &&
                it.contains("PRIMARY KEY(`id`)") &&
                it.contains("FOREIGN KEY(`entry_id`) REFERENCES `daily_entries`(`id`)")
            })
        }
    }

    @Test
    fun migrate_WHEN_executed_THEN_createsMedicationLogsIndex() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_2_3.migrate(db)

        // ASSERT
        verify {
            db.execSQL(match { it.contains("CREATE INDEX") && it.contains("index_medications_entry_id") })
        }
    }
}
