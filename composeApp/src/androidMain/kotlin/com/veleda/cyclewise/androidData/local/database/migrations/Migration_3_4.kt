package com.veleda.cyclewise.androidData.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_3_4 : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add the new 'note' column to the daily_entries table.
        db.execSQL("ALTER TABLE `daily_entries` ADD COLUMN `note` TEXT")
    }
}