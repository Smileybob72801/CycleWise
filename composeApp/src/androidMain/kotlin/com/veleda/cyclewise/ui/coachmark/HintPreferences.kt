package com.veleda.cyclewise.ui.coachmark

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Dedicated DataStore instance for coach mark hint preferences, separate from [AppSettings]. */
private val Context.hintDataStore by preferencesDataStore("hint_preferences")

/**
 * Persists which coach mark hints the user has already seen.
 *
 * Uses a dedicated DataStore (`"hint_preferences"`) to avoid bloating the main
 * app preferences file. Each [HintKey] maps to a boolean preference key.
 *
 * Registered as a Koin singleton so hint state survives session lock/unlock cycles.
 *
 * @param context Application context for DataStore access.
 */
class HintPreferences(private val context: Context) {

    /**
     * Returns a [Flow] emitting whether the hint identified by [key] has been dismissed.
     *
     * Emits `false` when the key has never been persisted (first launch).
     */
    fun isHintSeen(key: HintKey): Flow<Boolean> {
        val prefKey = booleanPreferencesKey("hint_seen_${key.name}")
        return context.hintDataStore.data.map { prefs -> prefs[prefKey] ?: false }
    }

    /** Marks the hint identified by [key] as seen so it will not be shown again. */
    suspend fun markHintSeen(key: HintKey) {
        val prefKey = booleanPreferencesKey("hint_seen_${key.name}")
        context.hintDataStore.edit { it[prefKey] = true }
    }

    /** Clears all hint-seen flags, causing all walkthroughs to replay on next visit. */
    suspend fun resetAll() {
        context.hintDataStore.edit { it.clear() }
    }
}
