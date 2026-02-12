package com.veleda.cyclewise.androidData.local.draft

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.waterDraftStore by preferencesDataStore("locked_water_draft")
private val PAYLOAD_KEY = stringPreferencesKey("payload")

const val MAX_DRAFT_CUPS = 99

@Serializable
data class WaterDraftPayload(
    val version: Int = 1,
    val lastActiveDate: String = "",
    val entries: List<WaterDraftEntry> = emptyList()
)

@Serializable
data class WaterDraftEntry(
    val date: String,
    val cups: Int
)

/**
 * DataStore-backed draft storage for water intake tracked before authentication.
 *
 * Allows the user to log water cups on the lock screen without unlocking the encrypted
 * database. Drafts are stored as a JSON-serialized [WaterDraftPayload] in plaintext
 * DataStore preferences. On successful unlock, [WaterDraftSyncer] transfers qualifying
 * drafts into the encrypted database.
 *
 * **Auto-prune:** [setCups] removes entries older than 29 days.
 * **Day rollover:** [ensureRolledOver] detects day changes and resets today's entry.
 * **Max cups:** Clamped to [MAX_DRAFT_CUPS] (99).
 */
class LockedWaterDraft(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    val drafts: Flow<Map<LocalDate, Int>> = context.waterDraftStore.data.map { prefs ->
        val raw = prefs[PAYLOAD_KEY] ?: return@map emptyMap()
        parsePayload(raw).entries.mapNotNull { entry ->
            val date = runCatching { LocalDate.parse(entry.date) }.getOrNull() ?: return@mapNotNull null
            val cups = entry.cups.coerceIn(0, MAX_DRAFT_CUPS)
            if (cups > 0) date to cups else null
        }.toMap()
    }

    suspend fun setCups(date: LocalDate, cups: Int) {
        val clamped = cups.coerceIn(0, MAX_DRAFT_CUPS)
        context.waterDraftStore.edit { prefs ->
            val payload = parsePayload(prefs[PAYLOAD_KEY])
            val cutoff = date.minus(29, DateTimeUnit.DAY)

            val mutableEntries = payload.entries
                .mapNotNull { entry ->
                    val d = runCatching { LocalDate.parse(entry.date) }.getOrNull() ?: return@mapNotNull null
                    if (d < cutoff) null else entry
                }
                .filter { it.date != date.toString() }
                .toMutableList()

            if (clamped > 0) {
                mutableEntries.add(WaterDraftEntry(date.toString(), clamped))
            }

            val updated = payload.copy(
                entries = mutableEntries.sortedBy { it.date }
            )
            prefs[PAYLOAD_KEY] = json.encodeToString(updated)
        }
    }

    suspend fun readAll(): Map<LocalDate, Int> = drafts.first()

    suspend fun clearDates(dates: Set<LocalDate>) {
        val dateStrings = dates.map { it.toString() }.toSet()
        context.waterDraftStore.edit { prefs ->
            val payload = parsePayload(prefs[PAYLOAD_KEY])
            val updated = payload.copy(
                entries = payload.entries.filter { it.date !in dateStrings }
            )
            prefs[PAYLOAD_KEY] = json.encodeToString(updated)
        }
    }

    suspend fun ensureRolledOver(today: LocalDate) {
        context.waterDraftStore.edit { prefs ->
            val raw = prefs[PAYLOAD_KEY]

            if (raw == null) {
                val default = WaterDraftPayload(
                    version = 1,
                    lastActiveDate = today.toString(),
                    entries = emptyList()
                )
                prefs[PAYLOAD_KEY] = json.encodeToString(default)
                return@edit
            }

            val payload = parsePayload(raw)
            val lastActive = runCatching { LocalDate.parse(payload.lastActiveDate) }.getOrNull()

            if (lastActive == null) {
                val reset = WaterDraftPayload(
                    version = 1,
                    lastActiveDate = today.toString(),
                    entries = emptyList()
                )
                prefs[PAYLOAD_KEY] = json.encodeToString(reset)
                return@edit
            }

            if (lastActive == today) {
                return@edit
            }

            val cutoff = today.minus(29, DateTimeUnit.DAY)
            val prunedEntries = payload.entries.mapNotNull { entry ->
                val d = runCatching { LocalDate.parse(entry.date) }.getOrNull() ?: return@mapNotNull null
                if (d < cutoff) null else entry
            }
            val withoutToday = prunedEntries.filter { it.date != today.toString() }

            val updated = payload.copy(
                lastActiveDate = today.toString(),
                entries = withoutToday.sortedBy { it.date }
            )
            prefs[PAYLOAD_KEY] = json.encodeToString(updated)
        }
    }

    private fun parsePayload(raw: String?): WaterDraftPayload {
        if (raw.isNullOrBlank()) return WaterDraftPayload()
        return runCatching { json.decodeFromString<WaterDraftPayload>(raw) }
            .getOrDefault(WaterDraftPayload())
    }
}
