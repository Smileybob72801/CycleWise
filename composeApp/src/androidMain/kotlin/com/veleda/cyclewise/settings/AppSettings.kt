package com.veleda.cyclewise.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("app_settings")
private val AUTOLOCK_MIN = intPreferencesKey("autolock_min")
private val TOP_SYMPTOMS_COUNT = intPreferencesKey("top_symptoms_count")
private val SHOW_MOOD_IN_SUMMARY = booleanPreferencesKey("show_mood_in_summary")
private val SHOW_ENERGY_IN_SUMMARY = booleanPreferencesKey("show_energy_in_summary")
private val SHOW_LIBIDO_IN_SUMMARY = booleanPreferencesKey("show_libido_in_summary")
private val SHOW_FOLLICULAR_PHASE = booleanPreferencesKey("show_follicular_phase")
private val SHOW_OVULATION_PHASE = booleanPreferencesKey("show_ovulation_phase")
private val SHOW_LUTEAL_PHASE = booleanPreferencesKey("show_luteal_phase")
private val MENSTRUATION_COLOR = stringPreferencesKey("menstruation_color")
private val FOLLICULAR_COLOR = stringPreferencesKey("follicular_color")
private val OVULATION_COLOR = stringPreferencesKey("ovulation_color")
private val LUTEAL_COLOR = stringPreferencesKey("luteal_color")

// --- Reminder preferences ---
private val REMINDER_PERIOD_ENABLED = booleanPreferencesKey("reminder_period_enabled")
private val REMINDER_PERIOD_DAYS_BEFORE = intPreferencesKey("reminder_period_days_before")
private val REMINDER_PERIOD_PRIVACY_ACCEPTED = booleanPreferencesKey("reminder_period_privacy_accepted")
private val REMINDER_MEDICATION_ENABLED = booleanPreferencesKey("reminder_medication_enabled")
private val REMINDER_MEDICATION_HOUR = intPreferencesKey("reminder_medication_hour")
private val REMINDER_MEDICATION_MINUTE = intPreferencesKey("reminder_medication_minute")
private val REMINDER_HYDRATION_ENABLED = booleanPreferencesKey("reminder_hydration_enabled")
private val REMINDER_HYDRATION_GOAL_CUPS = intPreferencesKey("reminder_hydration_goal_cups")
private val REMINDER_HYDRATION_FREQUENCY_HOURS = intPreferencesKey("reminder_hydration_frequency_hours")
private val REMINDER_HYDRATION_START_HOUR = intPreferencesKey("reminder_hydration_start_hour")
private val REMINDER_HYDRATION_END_HOUR = intPreferencesKey("reminder_hydration_end_hour")
private val CACHED_PREDICTED_PERIOD_DATE = stringPreferencesKey("cached_predicted_period_date")
private val THEME_MODE = stringPreferencesKey("theme_mode")

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
 * @property showFollicularPhase   Whether the Follicular phase tint is visible on the calendar (default: true).
 * @property showOvulationPhase    Whether the Ovulation phase tint is visible on the calendar (default: true).
 * @property showLutealPhase       Whether the Luteal phase tint is visible on the calendar (default: true).
 * @property menstruationColor     Custom hex color for Menstruation phase (6-char, no '#'; default: "EF9A9A").
 * @property follicularColor       Custom hex color for Follicular phase (6-char, no '#'; default: "80CBC4").
 * @property ovulationColor        Custom hex color for Ovulation phase (6-char, no '#'; default: "FFCC80").
 * @property lutealColor           Custom hex color for Luteal phase (6-char, no '#'; default: "B39DDB").
 * @property reminderPeriodEnabled          Whether the period prediction reminder is enabled (default: false).
 * @property reminderPeriodDaysBefore       Days before predicted period to notify (1–3, default: 2).
 * @property reminderPeriodPrivacyAccepted  Whether the user acknowledged the privacy notice for period reminders (default: false).
 * @property reminderMedicationEnabled      Whether the daily medication reminder is enabled (default: false).
 * @property reminderMedicationHour         Hour of day for medication reminder (0–23, default: 9).
 * @property reminderMedicationMinute       Minute of hour for medication reminder (0–59, default: 0).
 * @property reminderHydrationEnabled       Whether the hydration reminder is enabled (default: false).
 * @property reminderHydrationGoalCups      Daily water goal in cups (default: 8).
 * @property reminderHydrationFrequencyHours Interval between hydration reminders in hours (2/3/4, default: 3).
 * @property reminderHydrationStartHour     Active window start hour (0–23, default: 8).
 * @property reminderHydrationEndHour       Active window end hour (0–23, default: 20).
 * @property cachedPredictedPeriodDate      ISO date cache for the period prediction worker (e.g. "2026-03-15").
 * @property themeMode                     User-selected theme mode key ("system", "light", or "dark"; default: "system").
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

    /** Whether the Follicular phase tint is visible on the calendar. */
    val showFollicularPhase: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[SHOW_FOLLICULAR_PHASE] ?: true }

    /** Persists the user's preference for showing the Follicular phase on the calendar. */
    suspend fun setShowFollicularPhase(show: Boolean) {
        context.dataStore.edit { it[SHOW_FOLLICULAR_PHASE] = show }
    }

    /** Whether the Ovulation phase tint is visible on the calendar. */
    val showOvulationPhase: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[SHOW_OVULATION_PHASE] ?: true }

    /** Persists the user's preference for showing the Ovulation phase on the calendar. */
    suspend fun setShowOvulationPhase(show: Boolean) {
        context.dataStore.edit { it[SHOW_OVULATION_PHASE] = show }
    }

    /** Whether the Luteal phase tint is visible on the calendar. */
    val showLutealPhase: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[SHOW_LUTEAL_PHASE] ?: true }

    /** Persists the user's preference for showing the Luteal phase on the calendar. */
    suspend fun setShowLutealPhase(show: Boolean) {
        context.dataStore.edit { it[SHOW_LUTEAL_PHASE] = show }
    }

    /** Custom hex color for the Menstruation phase (6-char, no '#'). */
    val menstruationColor: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[MENSTRUATION_COLOR] ?: "EF9A9A" }

    /** Persists the user's custom hex color for the Menstruation phase. */
    suspend fun setMenstruationColor(hex: String) {
        context.dataStore.edit { it[MENSTRUATION_COLOR] = hex }
    }

    /** Custom hex color for the Follicular phase (6-char, no '#'). */
    val follicularColor: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[FOLLICULAR_COLOR] ?: "80CBC4" }

    /** Persists the user's custom hex color for the Follicular phase. */
    suspend fun setFollicularColor(hex: String) {
        context.dataStore.edit { it[FOLLICULAR_COLOR] = hex }
    }

    /** Custom hex color for the Ovulation phase (6-char, no '#'). */
    val ovulationColor: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[OVULATION_COLOR] ?: "FFCC80" }

    /** Persists the user's custom hex color for the Ovulation phase. */
    suspend fun setOvulationColor(hex: String) {
        context.dataStore.edit { it[OVULATION_COLOR] = hex }
    }

    /** Custom hex color for the Luteal phase (6-char, no '#'). */
    val lutealColor: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[LUTEAL_COLOR] ?: "B39DDB" }

    /** Persists the user's custom hex color for the Luteal phase. */
    suspend fun setLutealColor(hex: String) {
        context.dataStore.edit { it[LUTEAL_COLOR] = hex }
    }

    // ── Reminder preferences ──────────────────────────────────────────

    /** Whether the period prediction reminder is enabled. */
    val reminderPeriodEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[REMINDER_PERIOD_ENABLED] ?: false }

    /** Persists the user's preference for the period prediction reminder toggle. */
    suspend fun setReminderPeriodEnabled(enabled: Boolean) {
        context.dataStore.edit { it[REMINDER_PERIOD_ENABLED] = enabled }
    }

    /** Days before the predicted period to send the notification (1–3). */
    val reminderPeriodDaysBefore: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[REMINDER_PERIOD_DAYS_BEFORE] ?: 2 }

    /** Persists the number of days before a predicted period to notify. */
    suspend fun setReminderPeriodDaysBefore(days: Int) {
        context.dataStore.edit { it[REMINDER_PERIOD_DAYS_BEFORE] = days }
    }

    /** Whether the user has acknowledged the privacy dialog for period reminders. */
    val reminderPeriodPrivacyAccepted: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[REMINDER_PERIOD_PRIVACY_ACCEPTED] ?: false }

    /** Persists acknowledgement of the period reminder privacy notice. */
    suspend fun setReminderPeriodPrivacyAccepted(accepted: Boolean) {
        context.dataStore.edit { it[REMINDER_PERIOD_PRIVACY_ACCEPTED] = accepted }
    }

    /** Whether the daily medication reminder is enabled. */
    val reminderMedicationEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[REMINDER_MEDICATION_ENABLED] ?: false }

    /** Persists the user's preference for the medication reminder toggle. */
    suspend fun setReminderMedicationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[REMINDER_MEDICATION_ENABLED] = enabled }
    }

    /** Hour of day for the medication reminder (0–23). */
    val reminderMedicationHour: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[REMINDER_MEDICATION_HOUR] ?: 9 }

    /** Persists the medication reminder hour. */
    suspend fun setReminderMedicationHour(hour: Int) {
        context.dataStore.edit { it[REMINDER_MEDICATION_HOUR] = hour }
    }

    /** Minute of hour for the medication reminder (0–59). */
    val reminderMedicationMinute: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[REMINDER_MEDICATION_MINUTE] ?: 0 }

    /** Persists the medication reminder minute. */
    suspend fun setReminderMedicationMinute(minute: Int) {
        context.dataStore.edit { it[REMINDER_MEDICATION_MINUTE] = minute }
    }

    /** Whether the hydration reminder is enabled. */
    val reminderHydrationEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[REMINDER_HYDRATION_ENABLED] ?: false }

    /** Persists the user's preference for the hydration reminder toggle. */
    suspend fun setReminderHydrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[REMINDER_HYDRATION_ENABLED] = enabled }
    }

    /** Daily water goal in cups. */
    val reminderHydrationGoalCups: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[REMINDER_HYDRATION_GOAL_CUPS] ?: 8 }

    /** Persists the daily hydration goal. */
    suspend fun setReminderHydrationGoalCups(cups: Int) {
        context.dataStore.edit { it[REMINDER_HYDRATION_GOAL_CUPS] = cups }
    }

    /** Interval between hydration reminders in hours (2/3/4). */
    val reminderHydrationFrequencyHours: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[REMINDER_HYDRATION_FREQUENCY_HOURS] ?: 3 }

    /** Persists the hydration reminder frequency. */
    suspend fun setReminderHydrationFrequencyHours(hours: Int) {
        context.dataStore.edit { it[REMINDER_HYDRATION_FREQUENCY_HOURS] = hours }
    }

    /** Active window start hour for hydration reminders (0–23). */
    val reminderHydrationStartHour: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[REMINDER_HYDRATION_START_HOUR] ?: 8 }

    /** Persists the hydration reminder active window start hour. */
    suspend fun setReminderHydrationStartHour(hour: Int) {
        context.dataStore.edit { it[REMINDER_HYDRATION_START_HOUR] = hour }
    }

    /** Active window end hour for hydration reminders (0–23). */
    val reminderHydrationEndHour: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[REMINDER_HYDRATION_END_HOUR] ?: 20 }

    /** Persists the hydration reminder active window end hour. */
    suspend fun setReminderHydrationEndHour(hour: Int) {
        context.dataStore.edit { it[REMINDER_HYDRATION_END_HOUR] = hour }
    }

    /** ISO date cache for the period prediction worker (e.g. "2026-03-15"). */
    val cachedPredictedPeriodDate: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[CACHED_PREDICTED_PERIOD_DATE] ?: "" }

    /** Persists the cached predicted period date for the background worker. */
    suspend fun setCachedPredictedPeriodDate(date: String) {
        context.dataStore.edit { it[CACHED_PREDICTED_PERIOD_DATE] = date }
    }

    // ── Theme preferences ─────────────────────────────────────────────

    /** User-selected theme mode key ("system", "light", or "dark"). */
    val themeMode: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[THEME_MODE] ?: "system" }

    /** Persists the user's selected theme mode. */
    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }
}
