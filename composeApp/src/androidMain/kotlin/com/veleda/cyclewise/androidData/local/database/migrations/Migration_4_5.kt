package com.veleda.cyclewise.androidData.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.benasher44.uuid.uuid4

object Migration_4_5 : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Step 1: Create the new medication_library table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `medication_library` (
                `id` TEXT NOT NULL, 
                `name` TEXT NOT NULL, 
                `created_at` INTEGER NOT NULL, 
                PRIMARY KEY(`id`)
            )
        """)
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_medication_library_name` ON `medication_library` (`name`)")

        // Step 2: Create the new medication_logs table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `medication_logs` (
                `id` TEXT NOT NULL, 
                `entry_id` TEXT NOT NULL, 
                `medication_id` TEXT NOT NULL, 
                `created_at` INTEGER NOT NULL, 
                PRIMARY KEY(`id`), 
                FOREIGN KEY(`entry_id`) REFERENCES `daily_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                FOREIGN KEY(`medication_id`) REFERENCES `medication_library`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_medication_logs_entry_id` ON `medication_logs` (`entry_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_medication_logs_medication_id` ON `medication_logs` (`medication_id`)")

        // Step 3: Drop the old, now unused `medications` table
        db.execSQL("DROP TABLE IF EXISTS medications")
    }
}