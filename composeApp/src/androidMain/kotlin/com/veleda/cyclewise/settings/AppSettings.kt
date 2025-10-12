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

class AppSettings(private val context: Context) {
    // Auto-Lock
    val autolockMinutes = context.dataStore.data.map { prefs -> prefs[AUTOLOCK_MIN] ?: 10 }
    suspend fun setAutolockMinutes(min: Int) {
        context.dataStore.edit { it[AUTOLOCK_MIN] = min }
    }

    // Expose the setting as a Flow, defaulting to 3
    val topSymptomsCount: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[TOP_SYMPTOMS_COUNT] ?: 3
    }
    // Provide a way to change the setting
    suspend fun setTopSymptomsCount(count: Int) {
        context.dataStore.edit { it[TOP_SYMPTOMS_COUNT] = count }
    }

    // Pre-populated symptoms
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
}
