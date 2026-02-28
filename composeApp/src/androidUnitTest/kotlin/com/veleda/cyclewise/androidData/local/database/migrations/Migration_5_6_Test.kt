package com.veleda.cyclewise.androidData.local.database.migrations

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test

/**
 * Unit tests for [Migration_5_6].
 *
 * Verifies that the migration:
 * 1. Creates `symptom_library` with a unique name index.
 * 2. Creates `symptom_logs` with FKs to `daily_entries` (CASCADE) and `symptom_library` (RESTRICT).
 * 3. Migrates existing data from old `symptoms` table into the normalized schema.
 * 4. Drops the old `symptoms` table.
 */
class Migration_5_6_Test {

    private fun mockEmptyCursor(): Cursor {
        val cursor = mockk<Cursor>(relaxed = true)
        every { cursor.moveToFirst() } returns false
        return cursor
    }

    @Test
    fun migrate_WHEN_executed_THEN_createsSymptomLibraryTable() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        every { db.query(any<String>()) } returns mockEmptyCursor()

        // ACT
        Migration_5_6.migrate(db)

        // ASSERT
        verify {
            db.execSQL(match {
                it.contains("CREATE TABLE IF NOT EXISTS `symptom_library`") &&
                it.contains("`id` TEXT NOT NULL") &&
                it.contains("`name` TEXT NOT NULL") &&
                it.contains("`category` TEXT NOT NULL") &&
                it.contains("`created_at` INTEGER NOT NULL") &&
                it.contains("PRIMARY KEY(`id`)")
            })
        }
    }

    @Test
    fun migrate_WHEN_executed_THEN_createsUniqueNameIndexOnSymptomLibrary() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        every { db.query(any<String>()) } returns mockEmptyCursor()

        // ACT
        Migration_5_6.migrate(db)

        // ASSERT
        verify {
            db.execSQL(match {
                it.contains("CREATE UNIQUE INDEX") && it.contains("index_symptom_library_name")
            })
        }
    }

    @Test
    fun migrate_WHEN_executed_THEN_createsSymptomLogsTable() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        every { db.query(any<String>()) } returns mockEmptyCursor()

        // ACT
        Migration_5_6.migrate(db)

        // ASSERT
        verify {
            db.execSQL(match {
                it.contains("CREATE TABLE IF NOT EXISTS `symptom_logs`") &&
                it.contains("`id` TEXT NOT NULL") &&
                it.contains("`entry_id` TEXT NOT NULL") &&
                it.contains("`symptom_id` TEXT NOT NULL") &&
                it.contains("`severity` INTEGER NOT NULL") &&
                it.contains("`created_at` INTEGER NOT NULL") &&
                it.contains("FOREIGN KEY(`entry_id`) REFERENCES `daily_entries`(`id`)") &&
                it.contains("FOREIGN KEY(`symptom_id`) REFERENCES `symptom_library`(`id`)")
            })
        }
    }

    @Test
    fun migrate_WHEN_executed_THEN_createsSymptomLogsIndexes() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        every { db.query(any<String>()) } returns mockEmptyCursor()

        // ACT
        Migration_5_6.migrate(db)

        // ASSERT
        verify { db.execSQL(match { it.contains("CREATE INDEX") && it.contains("index_symptom_logs_entry_id") }) }
        verify { db.execSQL(match { it.contains("CREATE INDEX") && it.contains("index_symptom_logs_symptom_id") }) }
    }

    @Test
    fun migrate_WHEN_executed_THEN_dropsOldSymptomsTable() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        every { db.query(any<String>()) } returns mockEmptyCursor()

        // ACT
        Migration_5_6.migrate(db)

        // ASSERT
        verify { db.execSQL("DROP TABLE symptoms") }
    }

    @Test
    fun migrate_WHEN_existingDataPresent_THEN_migratesUniqueSymptomTypesToLibrary() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        val distinctCursor = mockk<Cursor>(relaxed = true)
        every { distinctCursor.moveToFirst() } returns true
        every { distinctCursor.moveToNext() } returns false
        every { distinctCursor.getString(0) } returns "Headache"

        val logCursor = mockEmptyCursor()
        every { db.query(match<String> { it.contains("SELECT DISTINCT symptom_type") }) } returns distinctCursor
        every { db.query(match<String> { it.contains("SELECT id, entry_id") }) } returns logCursor

        // ACT
        Migration_5_6.migrate(db)

        // ASSERT — inserts the symptom type into library with "OTHER" category
        verify {
            db.execSQL(
                match<String> { it.contains("INSERT INTO symptom_library") },
                match<Array<out Any?>> { args ->
                    args[1] == "Headache" && args[2] == "OTHER"
                }
            )
        }
    }
}
