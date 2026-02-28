package com.veleda.cyclewise.androidData.local.database.migrations

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

/**
 * Unit tests for [Migration_8_9].
 *
 * Verifies that the migration creates the `water_intake` table with `date` as TEXT PK
 * and `cups`, `created_at`, `updated_at` columns.
 */
class Migration_8_9_Test {

    @Test
    fun migrate_WHEN_executed_THEN_createsWaterIntakeTable() {
        // ARRANGE
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        // ACT
        Migration_8_9.migrate(db)

        // ASSERT
        verify(exactly = 1) {
            db.execSQL(match {
                it.contains("CREATE TABLE IF NOT EXISTS `water_intake`") &&
                it.contains("`date` TEXT NOT NULL") &&
                it.contains("`cups` INTEGER NOT NULL") &&
                it.contains("`created_at` INTEGER NOT NULL") &&
                it.contains("`updated_at` INTEGER NOT NULL") &&
                it.contains("PRIMARY KEY(`date`)")
            })
        }
    }
}
