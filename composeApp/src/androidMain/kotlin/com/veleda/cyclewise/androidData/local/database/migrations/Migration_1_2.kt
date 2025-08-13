package com.veleda.cyclewise.androidData.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_1_2 : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `daily_entries` (
                `id` TEXT NOT NULL, 
                `cycle_id` TEXT NOT NULL, 
                `entry_date` TEXT NOT NULL, 
                `day_in_cycle` INTEGER NOT NULL, 
                `flow_intensity` TEXT, 
                `mood_score` INTEGER, 
                `energy_level` INTEGER, 
                `libido_level` TEXT, 
                `spotting` INTEGER NOT NULL, 
                `custom_tags` TEXT NOT NULL, 
                `cycle_phase` TEXT, 
                `created_at` INTEGER NOT NULL, 
                `updated_at` INTEGER NOT NULL, 
                PRIMARY KEY(`id`), 
                FOREIGN KEY(`cycle_id`) REFERENCES `cycles`(`uuid`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_entries_cycle_id` ON `daily_entries` (`cycle_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_entries_entry_date` ON `daily_entries` (`entry_date`)")
    }
}
