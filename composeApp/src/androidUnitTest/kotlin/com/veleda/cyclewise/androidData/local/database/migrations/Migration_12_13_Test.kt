package com.veleda.cyclewise.androidData.local.database.migrations

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [Migration_12_13].
 *
 * Verifies that the migration executes the correct SQL statements to create
 * `custom_tag_library` and `custom_tag_logs` tables, and tests the
 * [Migration_12_13.parseJsonStringArray] helper used during data migration.
 */
class Migration_12_13_Test {

    // ── Table creation ────────────────────────────────────────────────

    @Test
    fun migrate_WHEN_executed_THEN_createsCustomTagLibraryTable() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_12_13.migrate(db)

        // ASSERT
        verify {
            db.execSQL(match {
                it.contains("CREATE TABLE IF NOT EXISTS `custom_tag_library`") &&
                it.contains("`id` TEXT NOT NULL") &&
                it.contains("`name` TEXT NOT NULL") &&
                it.contains("`created_at` INTEGER NOT NULL") &&
                it.contains("PRIMARY KEY(`id`)")
            })
        }
    }

    @Test
    fun migrate_WHEN_executed_THEN_createsCustomTagLogsTable() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_12_13.migrate(db)

        // ASSERT
        verify {
            db.execSQL(match {
                it.contains("CREATE TABLE IF NOT EXISTS `custom_tag_logs`") &&
                it.contains("`entry_id` TEXT NOT NULL") &&
                it.contains("`tag_id` TEXT NOT NULL") &&
                it.contains("FOREIGN KEY(`entry_id`) REFERENCES `daily_entries`(`id`)") &&
                it.contains("ON DELETE CASCADE") &&
                it.contains("FOREIGN KEY(`tag_id`) REFERENCES `custom_tag_library`(`id`)")
            })
        }
    }

    @Test
    fun migrate_WHEN_executed_THEN_createsIndices() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_12_13.migrate(db)

        // ASSERT — unique index on library name
        verify {
            db.execSQL(match {
                it.contains("CREATE UNIQUE INDEX") &&
                it.contains("index_custom_tag_library_name") &&
                it.contains("`custom_tag_library`(`name`)")
            })
        }
        // ASSERT — index on logs entry_id
        verify {
            db.execSQL(match {
                it.contains("CREATE INDEX") &&
                it.contains("index_custom_tag_logs_entry_id") &&
                it.contains("`custom_tag_logs`(`entry_id`)")
            })
        }
        // ASSERT — index on logs tag_id
        verify {
            db.execSQL(match {
                it.contains("CREATE INDEX") &&
                it.contains("index_custom_tag_logs_tag_id") &&
                it.contains("`custom_tag_logs`(`tag_id`)")
            })
        }
    }

    // ── parseJsonStringArray ──────────────────────────────────────────

    @Test
    fun parseJsonStringArray_WHEN_emptyArray_THEN_returnsEmptyList() {
        // WHEN
        val result = Migration_12_13.parseJsonStringArray("[]")

        // THEN
        assertEquals(emptyList(), result)
    }

    @Test
    fun parseJsonStringArray_WHEN_blankString_THEN_returnsEmptyList() {
        // WHEN
        val result = Migration_12_13.parseJsonStringArray("")

        // THEN
        assertEquals(emptyList(), result)
    }

    @Test
    fun parseJsonStringArray_WHEN_validArray_THEN_returnsParsedList() {
        // WHEN
        val result = Migration_12_13.parseJsonStringArray("""["tag1","tag2","tag3"]""")

        // THEN
        assertEquals(listOf("tag1", "tag2", "tag3"), result)
    }

    @Test
    fun parseJsonStringArray_WHEN_singleElement_THEN_returnsSingleItemList() {
        // WHEN
        val result = Migration_12_13.parseJsonStringArray("""["tag1"]""")

        // THEN
        assertEquals(listOf("tag1"), result)
    }
}
