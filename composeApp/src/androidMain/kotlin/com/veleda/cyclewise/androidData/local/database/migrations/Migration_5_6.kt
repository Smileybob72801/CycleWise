package com.veleda.cyclewise.androidData.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.benasher44.uuid.uuid4

/**
 * v5 -> v6: Introduces the normalized symptom library.
 *
 * Creates `symptom_library` (unique name index) and `symptom_logs` (with FKs to
 * `daily_entries` CASCADE and `symptom_library` RESTRICT). Migrates existing data from
 * the old `symptoms` table: extracts unique symptom types into the library, maps old log
 * entries to the new FK-based schema, then drops the old table.
 */
object Migration_5_6 : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Step 1: Create the new symptom_library table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `symptom_library` (
                `id` TEXT NOT NULL, 
                `name` TEXT NOT NULL, 
                `category` TEXT NOT NULL, 
                `created_at` INTEGER NOT NULL, 
                PRIMARY KEY(`id`)
            )
        """)
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_symptom_library_name` ON `symptom_library` (`name`)")

        // Step 2: Create the new symptom_logs table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `symptom_logs` (
                `id` TEXT NOT NULL, 
                `entry_id` TEXT NOT NULL, 
                `symptom_id` TEXT NOT NULL, 
                `severity` INTEGER NOT NULL, 
                `created_at` INTEGER NOT NULL, 
                PRIMARY KEY(`id`), 
                FOREIGN KEY(`entry_id`) REFERENCES `daily_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                FOREIGN KEY(`symptom_id`) REFERENCES `symptom_library`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_symptom_logs_entry_id` ON `symptom_logs` (`entry_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_symptom_logs_symptom_id` ON `symptom_logs` (`symptom_id`)")

        // Step 3: Migrate existing data from the old `symptoms` table
        val nameToUuid = mutableMapOf<String, String>()
        val now = System.currentTimeMillis()

        // Find unique symptom types from the old table
        db.query("SELECT DISTINCT symptom_type FROM symptoms").use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    val name = cursor.getString(0)
                    val newUuid = uuid4().toString()
                    nameToUuid[name] = newUuid
                    // Insert each unique name into the new library table with a default category
                    db.execSQL("INSERT INTO symptom_library (id, name, category, created_at) VALUES (?, ?, ?, ?)", arrayOf(newUuid, name, "OTHER", now))
                } while (cursor.moveToNext())
            }
        }

        // Migrate the old log entries to the new table
        db.query("SELECT id, entry_id, symptom_type, severity FROM symptoms").use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    val logId = cursor.getString(0)
                    val entryId = cursor.getString(1)
                    val type = cursor.getString(2)
                    val severity = cursor.getInt(3)
                    val symptomUuid = nameToUuid[type]
                    if (symptomUuid != null) {
                        db.execSQL("INSERT INTO symptom_logs (id, entry_id, symptom_id, severity, created_at) VALUES (?, ?, ?, ?, ?)", arrayOf(logId, entryId, symptomUuid, severity, now))
                    }
                } while (cursor.moveToNext())
            }
        }

        // Step 4: Drop the old `symptoms` table
        db.execSQL("DROP TABLE symptoms")
    }
}