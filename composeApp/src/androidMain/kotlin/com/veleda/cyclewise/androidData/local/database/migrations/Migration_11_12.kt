package com.veleda.cyclewise.androidData.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v11 -> v12: Changes FK delete action from RESTRICT to CASCADE on `symptom_logs.symptom_id`
 * and `medication_logs.medication_id`.
 *
 * This allows deleting a symptom or medication from the library without first removing
 * every log entry that references it. SQLite lacks ALTER CONSTRAINT, so both tables are
 * rebuilt using the standard create-temp → insert → drop → rename → recreate-indices pattern.
 *
 * **symptom_logs:** `entry_id` FK stays CASCADE; `symptom_id` FK changes RESTRICT → CASCADE.
 * **medication_logs:** `entry_id` FK stays CASCADE; `medication_id` FK changes RESTRICT → CASCADE.
 */
object Migration_11_12 : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ── symptom_logs ────────────────────────────────────────────────
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `symptom_logs_new` (
                `id` TEXT NOT NULL,
                `entry_id` TEXT NOT NULL,
                `symptom_id` TEXT NOT NULL,
                `severity` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`entry_id`) REFERENCES `daily_entries`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`symptom_id`) REFERENCES `symptom_library`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """)
        db.execSQL("""
            INSERT INTO `symptom_logs_new`
            SELECT `id`, `entry_id`, `symptom_id`, `severity`, `created_at`
            FROM `symptom_logs`
        """)
        db.execSQL("DROP TABLE `symptom_logs`")
        db.execSQL("ALTER TABLE `symptom_logs_new` RENAME TO `symptom_logs`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_symptom_logs_entry_id` ON `symptom_logs`(`entry_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_symptom_logs_symptom_id` ON `symptom_logs`(`symptom_id`)")

        // ── medication_logs ─────────────────────────────────────────────
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `medication_logs_new` (
                `id` TEXT NOT NULL,
                `entry_id` TEXT NOT NULL,
                `medication_id` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`entry_id`) REFERENCES `daily_entries`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`medication_id`) REFERENCES `medication_library`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """)
        db.execSQL("""
            INSERT INTO `medication_logs_new`
            SELECT `id`, `entry_id`, `medication_id`, `created_at`
            FROM `medication_logs`
        """)
        db.execSQL("DROP TABLE `medication_logs`")
        db.execSQL("ALTER TABLE `medication_logs_new` RENAME TO `medication_logs`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_medication_logs_entry_id` ON `medication_logs`(`entry_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_medication_logs_medication_id` ON `medication_logs`(`medication_id`)")
    }
}
