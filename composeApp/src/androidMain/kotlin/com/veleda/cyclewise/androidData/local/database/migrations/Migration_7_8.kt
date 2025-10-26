package com.veleda.cyclewise.androidData.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.benasher44.uuid.uuid4
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object Migration_7_8 : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create the new period_logs table.
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `period_logs` (
                `id` TEXT NOT NULL, 
                `entry_id` TEXT NOT NULL, 
                `flow_intensity` TEXT NOT NULL, 
                `created_at` INTEGER NOT NULL, 
                `updated_at` INTEGER NOT NULL, 
                PRIMARY KEY(`id`), 
                FOREIGN KEY(`entry_id`) REFERENCES `daily_entries`(`id`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_period_logs_entry_id` ON `period_logs` (`entry_id`)")

        // 2. Create the temporary NEW daily_entries table without flow/spotting columns.
        db.execSQL("""
            CREATE TABLE `daily_entries_new` (
                `id` TEXT NOT NULL, 
                `entry_date` TEXT NOT NULL, 
                `day_in_cycle` INTEGER NOT NULL, 
                `mood_score` INTEGER, 
                `energy_level` INTEGER, 
                `libido_level` TEXT, 
                `custom_tags` TEXT NOT NULL, 
                `note` TEXT, 
                `cycle_phase` TEXT, 
                `created_at` INTEGER NOT NULL, 
                `updated_at` INTEGER NOT NULL, 
                PRIMARY KEY(`id`)
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_entries_entry_date` ON `daily_entries_new` (`entry_date`)")

        // 3. Migrate flow_intensity data to period_logs.
        // Also map 'spotting' data to a system symptom log (simplified to just migrating flow data for this refactor).
        db.query("SELECT id, flow_intensity, created_at, updated_at FROM daily_entries WHERE flow_intensity IS NOT NULL").use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    val entryId = cursor.getString(0)
                    val flowIntensity = cursor.getString(1)
                    val createdAt = cursor.getLong(2)
                    val updatedAt = cursor.getLong(3)

                    // Insert into period_logs
                    db.execSQL("INSERT INTO period_logs (id, entry_id, flow_intensity, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
                        arrayOf(uuid4().toString(), entryId, flowIntensity, createdAt, updatedAt)
                    )
                } while (cursor.moveToNext())
            }
        }

        // 4. Copy the *non-flow* data from the old table to the new one.
        // Columns removed: flow_intensity, spotting.
        db.execSQL("""
            INSERT INTO `daily_entries_new` (id, entry_date, day_in_cycle, mood_score, energy_level, libido_level, custom_tags, note, cycle_phase, created_at, updated_at)
            SELECT id, entry_date, day_in_cycle, mood_score, energy_level, libido_level, custom_tags, note, cycle_phase, created_at, updated_at
            FROM `daily_entries`
        """)

        // 5. Drop the old daily_entries table.
        db.execSQL("DROP TABLE `daily_entries`")

        // 6. Rename the new table to the original name.
        db.execSQL("ALTER TABLE `daily_entries_new` RENAME TO `daily_entries`")
    }
}