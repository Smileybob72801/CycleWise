package com.veleda.cyclewise.androidData.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v10 -> v11: Makes `flow_intensity` nullable in `period_logs`.
 *
 * A [PeriodLog] can now exist with a null `flow_intensity` — its presence alone
 * indicates that the day is a period day, without requiring the user to select a
 * flow level. SQLite lacks ALTER COLUMN, so the table is rebuilt without the
 * NOT NULL constraint on `flow_intensity`.
 *
 * The INSERT uses `COALESCE` on `created_at` and `updated_at` to handle legacy
 * rows where these columns may be NULL (pre-zero-key-fix databases). A fallback
 * of `0` (epoch) satisfies the NOT NULL constraint without losing any data.
 */
object Migration_10_11 : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `period_logs_new` (
                `id` TEXT NOT NULL,
                `entry_id` TEXT NOT NULL,
                `flow_intensity` TEXT,
                `period_color` TEXT,
                `period_consistency` TEXT,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`entry_id`) REFERENCES `daily_entries`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """)
        db.execSQL("""
            INSERT INTO `period_logs_new`
            SELECT `id`, `entry_id`, `flow_intensity`, `period_color`, `period_consistency`,
                   COALESCE(`created_at`, 0), COALESCE(`updated_at`, 0)
            FROM `period_logs`
        """)
        db.execSQL("DROP TABLE `period_logs`")
        db.execSQL("ALTER TABLE `period_logs_new` RENAME TO `period_logs`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_period_logs_entry_id` ON `period_logs`(`entry_id`)")
    }
}
