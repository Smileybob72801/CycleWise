package com.veleda.cyclewise.androidData.local.database.migrations

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test

/**
 * Unit tests for [Migration_11_12].
 *
 * Verifies that the migration executes the correct SQL statements to rebuild
 * `symptom_logs` and `medication_logs` tables with CASCADE on their library FK
 * (changed from RESTRICT).
 */
class Migration_11_12_Test {

    // ── symptom_logs ────────────────────────────────────────────────

    @Test
    fun migrate_WHEN_executed_THEN_rebuildsSymptomLogsWithCascade() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_11_12.migrate(db)

        // ASSERT — verify the table rebuild sequence for symptom_logs
        verifyOrder {
            db.execSQL(match {
                it.contains("CREATE TABLE IF NOT EXISTS `symptom_logs_new`") &&
                it.contains("FOREIGN KEY(`symptom_id`) REFERENCES `symptom_library`(`id`)") &&
                it.contains("ON DELETE CASCADE")
            })
            db.execSQL(match {
                it.contains("INSERT INTO `symptom_logs_new`") &&
                it.contains("FROM `symptom_logs`")
            })
            db.execSQL(match { it.contains("DROP TABLE `symptom_logs`") })
            db.execSQL(match { it.contains("ALTER TABLE `symptom_logs_new` RENAME TO `symptom_logs`") })
            db.execSQL(match { it.contains("CREATE INDEX") && it.contains("index_symptom_logs_entry_id") })
            db.execSQL(match { it.contains("CREATE INDEX") && it.contains("index_symptom_logs_symptom_id") })
        }
    }

    @Test
    fun migrate_WHEN_executed_THEN_symptomLogsEntryIdFkRemainsCascade() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_11_12.migrate(db)

        // ASSERT — entry_id FK must still be CASCADE
        verify {
            db.execSQL(match {
                it.contains("symptom_logs_new") &&
                it.contains("FOREIGN KEY(`entry_id`) REFERENCES `daily_entries`(`id`)") &&
                it.contains("ON DELETE CASCADE")
            })
        }
    }

    // ── medication_logs ─────────────────────────────────────────────

    @Test
    fun migrate_WHEN_executed_THEN_rebuildsMedicationLogsWithCascade() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_11_12.migrate(db)

        // ASSERT — verify the table rebuild sequence for medication_logs
        verifyOrder {
            db.execSQL(match {
                it.contains("CREATE TABLE IF NOT EXISTS `medication_logs_new`") &&
                it.contains("FOREIGN KEY(`medication_id`) REFERENCES `medication_library`(`id`)") &&
                it.contains("ON DELETE CASCADE")
            })
            db.execSQL(match {
                it.contains("INSERT INTO `medication_logs_new`") &&
                it.contains("FROM `medication_logs`")
            })
            db.execSQL(match { it.contains("DROP TABLE `medication_logs`") })
            db.execSQL(match { it.contains("ALTER TABLE `medication_logs_new` RENAME TO `medication_logs`") })
            db.execSQL(match { it.contains("CREATE INDEX") && it.contains("index_medication_logs_entry_id") })
            db.execSQL(match { it.contains("CREATE INDEX") && it.contains("index_medication_logs_medication_id") })
        }
    }

    @Test
    fun migrate_WHEN_executed_THEN_medicationLogsEntryIdFkRemainsCascade() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_11_12.migrate(db)

        // ASSERT — entry_id FK must still be CASCADE
        verify {
            db.execSQL(match {
                it.contains("medication_logs_new") &&
                it.contains("FOREIGN KEY(`entry_id`) REFERENCES `daily_entries`(`id`)") &&
                it.contains("ON DELETE CASCADE")
            })
        }
    }
}
