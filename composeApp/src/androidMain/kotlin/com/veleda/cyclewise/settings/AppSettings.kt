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

class AppSettings(private val context: Context) {
    // Auto-Lock
    val autolockMinutes = context.dataStore.data.map { prefs -> prefs[AUTOLOCK_MIN] ?: 10 }
    suspend fun setAutolockMinutes(min: Int) {
        context.dataStore.edit { it[AUTOLOCK_MIN] = min }
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
