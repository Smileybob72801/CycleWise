package com.veleda.cyclewise.androidData.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

/**
 * v12 -> v13: Promotes custom tags from inline JSON strings on `daily_entries` to first-class
 * library objects with their own tables, matching the symptom/medication library pattern.
 *
 * Creates:
 * - `custom_tag_library` — unique tag definitions (id, name, created_at)
 * - `custom_tag_logs` — per-day tag applications linking entries to library tags
 *
 * Data migration:
 * 1. Reads `custom_tags` JSON from every `daily_entries` row that has non-empty tags
 * 2. Deduplicates tag strings into `custom_tag_library` entries
 * 3. Creates `custom_tag_logs` rows linking each entry to its tags
 * 4. Clears the deprecated `custom_tags` column to `"[]"` (column kept to avoid table rebuild)
 */
object Migration_12_13 : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ── 1. Create custom_tag_library table ───────────────────────────
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `custom_tag_library` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_custom_tag_library_name` ON `custom_tag_library`(`name`)"
        )

        // ── 2. Create custom_tag_logs table ──────────────────────────────
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `custom_tag_logs` (
                `id` TEXT NOT NULL,
                `entry_id` TEXT NOT NULL,
                `tag_id` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`entry_id`) REFERENCES `daily_entries`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`tag_id`) REFERENCES `custom_tag_library`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_custom_tag_logs_entry_id` ON `custom_tag_logs`(`entry_id`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_custom_tag_logs_tag_id` ON `custom_tag_logs`(`tag_id`)"
        )

        // ── 3. Migrate existing JSON data ────────────────────────────────
        // In-memory cache of tag name → library ID to deduplicate across entries
        val tagNameToId = mutableMapOf<String, String>()

        val cursor = db.query(
            "SELECT id, custom_tags, created_at FROM daily_entries WHERE custom_tags != '[]' AND custom_tags != ''"
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                val entryId = c.getString(0)
                val tagsJson = c.getString(1)
                val createdAt = c.getLong(2)

                val tags = parseJsonStringArray(tagsJson)
                for (tagName in tags) {
                    val tagId = tagNameToId.getOrPut(tagName) {
                        val newId = UUID.randomUUID().toString()
                        db.execSQL(
                            "INSERT INTO custom_tag_library (id, name, created_at) VALUES (?, ?, ?)",
                            arrayOf(newId, tagName, createdAt)
                        )
                        newId
                    }

                    val logId = UUID.randomUUID().toString()
                    db.execSQL(
                        "INSERT INTO custom_tag_logs (id, entry_id, tag_id, created_at) VALUES (?, ?, ?, ?)",
                        arrayOf(logId, entryId, tagId, createdAt)
                    )
                }
            }
        }

        // ── 4. Clear deprecated column ───────────────────────────────────
        db.execSQL("UPDATE daily_entries SET custom_tags = '[]'")
    }

    /**
     * Parses a JSON string array (e.g. `["tag1","tag2"]`) into a list of strings.
     *
     * Uses simple string manipulation rather than kotlinx.serialization, which is not
     * available during Room migration execution.
     */
    internal fun parseJsonStringArray(json: String): List<String> {
        val trimmed = json.trim()
        if (trimmed == "[]" || trimmed.isBlank()) return emptyList()

        return trimmed
            .removeSurrounding("[", "]")
            .split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotBlank() }
    }
}
