package com.veleda.cyclewise.androidData.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v2 -> v3: Creates the initial `symptoms` and `medication_logs` tables with entry_id FKs. */
object Migration_2_3 : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create the 'symptoms' table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `symptoms` (
                `id` TEXT NOT NULL,
                `entry_id` TEXT NOT NULL,
                `symptom_type` TEXT NOT NULL,
                `severity` INTEGER NOT NULL,
                `note` TEXT,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`entry_id`) REFERENCES `daily_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_symptoms_entry_id` ON `symptoms` (`entry_id`)")

        // Create the 'medication-logs' table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `medication_logs` (
                `id` TEXT NOT NULL,
                `entry_id` TEXT NOT NULL,
                `medication_name` TEXT NOT NULL,
                `note` TEXT,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`entry_id`) REFERENCES `daily_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_medications_entry_id` ON `medication_logs` (`entry_id`)")
    }
}
