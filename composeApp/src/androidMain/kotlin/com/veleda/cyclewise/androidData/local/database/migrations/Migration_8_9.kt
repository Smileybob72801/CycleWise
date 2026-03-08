package com.veleda.cyclewise.androidData.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v8 -> v9: Creates the `water_intake` table with date as TEXT PK and cups/timestamp columns. */
object Migration_8_9 : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `water_intake` (
                `date` TEXT NOT NULL,
                `cups` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`date`)
            )
        """)
    }
}
