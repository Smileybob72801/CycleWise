package com.veleda.cyclewise.androidData.local.database.migrations

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

/**
 * Unit tests for [Migration_3_4].
 *
 * Verifies that the migration adds the `note` TEXT column to `daily_entries`.
 */
class Migration_3_4_Test {

    @Test
    fun migrate_WHEN_executed_THEN_addsNoteColumnToDailyEntries() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_3_4.migrate(db)

        // ASSERT
        verify(exactly = 1) {
            db.execSQL(match {
                it.contains("ALTER TABLE `daily_entries` ADD COLUMN `note` TEXT")
            })
        }
    }
}
