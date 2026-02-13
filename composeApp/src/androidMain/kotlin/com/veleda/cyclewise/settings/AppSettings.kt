package com.veleda.cyclewise.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("app_settings")
private val AUTOLOCK_MIN = intPreferencesKey("autolock_min")
private val TOP_SYMPTOMS_COUNT = intPreferencesKey("top_symptoms_count")
private val SHOW_MOOD_IN_SUMMARY = booleanPreferencesKey("show_mood_in_summary")
private val SHOW_ENERGY_IN_SUMMARY = booleanPreferencesKey("show_energy_in_summary")
private val SHOW_LIBIDO_IN_SUMMARY = booleanPreferencesKey("show_libido_in_summary")

/**
 * DataStore-backed wrapper for user-configurable app preferences.
 *
 * All properties are exposed as cold [Flow]s that emit the current value on subscription.
 * Singleton-scoped (lives for the app process).
 *
 * @property autolockMinutes       Inactivity timeout before auto-lock, in minutes (default: 10).
 * @property topSymptomsCount      Number of top symptoms shown in the recurrence insight (default: 3).
 * @property isPrepopulated        Whether the default symptom library has been seeded on first unlock.
 * @property showMoodInSummary     Whether mood score is displayed in the log summary bottom sheet (default: true).
 * @property showEnergyInSummary   Whether energy level is displayed in the log summary bottom sheet (default: true).
 * @property showLibidoInSummary   Whether libido score is displayed in the log summary bottom sheet (default: true).
 */
class AppSettings(private val context: Context) {
    val autolockMinutes = context.dataStore.data.map { prefs -> prefs[AUTOLOCK_MIN] ?: 10 }
    suspend fun setAutolockMinutes(min: Int) {
        context.dataStore.edit { it[AUTOLOCK_MIN] = min }
    }

    val topSymptomsCount: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[TOP_SYMPTOMS_COUNT] ?: 3
    }

    suspend fun setTopSymptomsCount(count: Int) {
        context.dataStore.edit { it[TOP_SYMPTOMS_COUNT] = count }
    }

    private val IS_PREPOPULATED = booleanPreferencesKey("is_prepopulated")

    val isPrepopulated: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_PREPOPULATED] ?: false
        }
    suspend fun setPrepopulated(value: Boolean) {
        context.dataStore.edit { settings ->
            settings[IS_PREPOPULATED] = value
        }
    }

    /** Whether mood score is displayed in the log summary bottom sheet. */
    val showMoodInSummary: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[SHOW_MOOD_IN_SUMMARY] ?: true }

    /** Persists the user's preference for showing mood in the log summary sheet. */
    suspend fun setShowMoodInSummary(show: Boolean) {
        context.dataStore.edit { it[SHOW_MOOD_IN_SUMMARY] = show }
    }

    /** Whether energy level is displayed in the log summary bottom sheet. */
    val showEnergyInSummary: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[SHOW_ENERGY_IN_SUMMARY] ?: true }

    /** Persists the user's preference for showing energy in the log summary sheet. */
    suspend fun setShowEnergyInSummary(show: Boolean) {
        context.dataStore.edit { it[SHOW_ENERGY_IN_SUMMARY] = show }
    }

    /** Whether libido score is displayed in the log summary bottom sheet. */
    val showLibidoInSummary: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[SHOW_LIBIDO_IN_SUMMARY] ?: true }

    /** Persists the user's preference for showing libido in the log summary sheet. */
    suspend fun setShowLibidoInSummary(show: Boolean) {
        context.dataStore.edit { it[SHOW_LIBIDO_IN_SUMMARY] = show }
    }
}
