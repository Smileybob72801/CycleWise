package com.veleda.cyclewise.androidData.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_6_7 : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // SQLite doesn't directly support dropping columns or foreign keys easily.
        // The standard, safe procedure is to create a new table, copy the data, drop the old table, and rename.

        // 1. Create the new table without the foreign key and 'cycle_id' column.
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `daily_entries_new` (
                `id` TEXT NOT NULL, 
                `entry_date` TEXT NOT NULL, 
                `day_in_cycle` INTEGER NOT NULL, 
                `flow_intensity` TEXT, 
                `mood_score` INTEGER, 
                `energy_level` INTEGER, 
                `libido_level` TEXT, 
                `spotting` INTEGER NOT NULL, 
                `custom_tags` TEXT NOT NULL, 
                `note` TEXT, 
                `cycle_phase` TEXT, 
                `created_at` INTEGER NOT NULL, 
                `updated_at` INTEGER NOT NULL, 
                PRIMARY KEY(`id`)
            )
        """)

        // 2. Copy the data from the old table to the new one, omitting the 'cycle_id' column.
        db.execSQL("""
            INSERT INTO `daily_entries_new` (id, entry_date, day_in_cycle, flow_intensity, mood_score, energy_level, libido_level, spotting, custom_tags, note, cycle_phase, created_at, updated_at)
            SELECT id, entry_date, day_in_cycle, flow_intensity, mood_score, energy_level, libido_level, spotting, custom_tags, note, cycle_phase, created_at, updated_at
            FROM `daily_entries`
        """)

        // 3. Drop the old table.
        db.execSQL("DROP TABLE `daily_entries`")

        // 4. Rename the new table to the original name.
        db.execSQL("ALTER TABLE `daily_entries_new` RENAME TO `daily_entries`")

        // 5. Recreate the index on entry_date.
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_entries_entry_date` ON `daily_entries` (`entry_date`)")
    }
}