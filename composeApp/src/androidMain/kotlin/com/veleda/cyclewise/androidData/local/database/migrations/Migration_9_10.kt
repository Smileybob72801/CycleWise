package com.veleda.cyclewise.androidData.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v9 -> v10: Converts `libido_level TEXT` to `libido_score INTEGER` in `daily_entries`,
 * and adds `period_color TEXT` and `period_consistency TEXT` columns to `period_logs`.
 *
 * **Libido migration mapping:** LOW -> 2, MEDIUM -> 3, HIGH -> 4, NULL -> NULL.
 * Headroom is preserved at 1 and 5 for the new 1-5 scale.
 *
 * The `daily_entries` table is rebuilt (SQLite lacks ALTER COLUMN) while `period_logs`
 * uses simple ALTER TABLE ADD COLUMN statements.
 */
object Migration_9_10 : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Rebuild daily_entries: replace libido_level TEXT with libido_score INTEGER
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `daily_entries_new` (
                `id` TEXT NOT NULL,
                `entry_date` TEXT NOT NULL,
                `day_in_cycle` INTEGER NOT NULL,
                `mood_score` INTEGER,
                `energy_level` INTEGER,
                `libido_score` INTEGER,
                `custom_tags` TEXT NOT NULL,
                `note` TEXT,
                `cycle_phase` TEXT,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """)

        db.execSQL("""
            INSERT INTO `daily_entries_new` (
                id, entry_date, day_in_cycle, mood_score, energy_level, libido_score,
                custom_tags, note, cycle_phase, created_at, updated_at
            )
            SELECT
                id, entry_date, day_in_cycle, mood_score, energy_level,
                CASE libido_level
                    WHEN 'LOW' THEN 2
                    WHEN 'MEDIUM' THEN 3
                    WHEN 'HIGH' THEN 4
                    ELSE NULL
                END,
                custom_tags, note, cycle_phase, created_at, updated_at
            FROM `daily_entries`
        """)

        db.execSQL("DROP TABLE `daily_entries`")
        db.execSQL("ALTER TABLE `daily_entries_new` RENAME TO `daily_entries`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_entries_entry_date` ON `daily_entries`(`entry_date`)")

        // 2. Add new columns to period_logs
        db.execSQL("ALTER TABLE `period_logs` ADD COLUMN `period_color` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `period_logs` ADD COLUMN `period_consistency` TEXT DEFAULT NULL")
    }
}
