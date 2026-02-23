package com.veleda.cyclewise.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veleda.cyclewise.reminders.ReminderScheduler
import com.veleda.cyclewise.settings.AppSettings
import com.veleda.cyclewise.ui.theme.ThemeMode
import com.veleda.cyclewise.ui.tracker.CyclePhaseColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Settings pager screen.
 *
 * All preference values are held here so the composable tree is a pure function of this state.
 * Dialog visibility flags (about, privacy, permission rationale) are also managed here
 * instead of via local `remember { mutableStateOf() }` to keep state centralized.
 *
 * @property themeMode                 User-selected theme mode (default [ThemeMode.SYSTEM]).
 * @property autolockMinutes          Auto-lock timeout in minutes (default 10).
 * @property topSymptomsCount         Number of top symptoms shown in insights (default 3).
 * @property showMood                 Whether mood is shown in the daily log summary.
 * @property showEnergy               Whether energy is shown in the daily log summary.
 * @property showLibido               Whether libido is shown in the daily log summary.
 * @property showFollicular           Whether the Follicular phase tint is visible on the calendar.
 * @property showOvulation            Whether the Ovulation phase tint is visible on the calendar.
 * @property showLuteal               Whether the Luteal phase tint is visible on the calendar.
 * @property menstruationColorHex     6-char hex color for the Menstruation phase.
 * @property follicularColorHex       6-char hex color for the Follicular phase.
 * @property ovulationColorHex        6-char hex color for the Ovulation phase.
 * @property lutealColorHex           6-char hex color for the Luteal phase.
 * @property periodReminderEnabled    Whether the period prediction reminder is on.
 * @property periodDaysBefore         Days before predicted period to notify (1-3).
 * @property periodPrivacyAccepted    Whether the user has accepted the period privacy notice.
 * @property medicationReminderEnabled Whether the daily medication reminder is on.
 * @property medicationHour           Hour for the medication reminder (0-23).
 * @property medicationMinute         Minute for the medication reminder (0-59).
 * @property hydrationReminderEnabled Whether the hydration reminder is on.
 * @property hydrationGoalCups        Daily water goal in cups.
 * @property hydrationFrequencyHours  Interval between hydration reminders in hours.
 * @property hydrationStartHour       Active window start hour for hydration reminders.
 * @property hydrationEndHour         Active window end hour for hydration reminders.
 * @property showAboutDialog          Whether the About dialog is visible.
 * @property showPrivacyDialog        Whether the period privacy dialog is visible.
 * @property showPermissionRationale  Whether the notification permission rationale is shown.
 */
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autolockMinutes: Int = 10,
    val topSymptomsCount: Int = 3,
    val showMood: Boolean = true,
    val showEnergy: Boolean = true,
    val showLibido: Boolean = true,
    val showFollicular: Boolean = true,
    val showOvulation: Boolean = true,
    val showLuteal: Boolean = true,
    val menstruationColorHex: String = CyclePhaseColors.DEFAULT_MENSTRUATION_HEX,
    val follicularColorHex: String = CyclePhaseColors.DEFAULT_FOLLICULAR_HEX,
    val ovulationColorHex: String = CyclePhaseColors.DEFAULT_OVULATION_HEX,
    val lutealColorHex: String = CyclePhaseColors.DEFAULT_LUTEAL_HEX,
    val periodReminderEnabled: Boolean = false,
    val periodDaysBefore: Int = 2,
    val periodPrivacyAccepted: Boolean = false,
    val medicationReminderEnabled: Boolean = false,
    val medicationHour: Int = 9,
    val medicationMinute: Int = 0,
    val hydrationReminderEnabled: Boolean = false,
    val hydrationGoalCups: Int = 8,
    val hydrationFrequencyHours: Int = 3,
    val hydrationStartHour: Int = 8,
    val hydrationEndHour: Int = 20,
    val showAboutDialog: Boolean = false,
    val showPrivacyDialog: Boolean = false,
    val showPermissionRationale: Boolean = false,
)

/**
 * Settings screen ViewModel following the MVI-inspired pattern.
 *
 * Collects all [AppSettings] preference flows in its `init` block via individual
 * `Flow.onEach { }.launchIn(viewModelScope)` collectors. User interactions are dispatched
 * through [onEvent], which applies a pure [reduce] for state updates and then launches
 * side effects (DataStore writes, [ReminderScheduler] calls) in `viewModelScope`.
 *
 * Singleton-scoped (no database access needed). Session-specific operations
 * (Lock Now, Debug Seeder) are handled at the Screen level via Koin scope access.
 *
 * @param appSettings       The [AppSettings] for reading/writing preferences.
 * @param reminderScheduler The [ReminderScheduler] for notification scheduling.
 */
class SettingsViewModel(
    private val appSettings: AppSettings,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())

    /** Observable settings UI state. */
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Collect each AppSettings flow individually to populate state.
        // This avoids the 22+ param combine() limitation.
        appSettings.themeMode
            .onEach { value -> _uiState.update { it.copy(themeMode = ThemeMode.fromKey(value)) } }
            .launchIn(viewModelScope)

        appSettings.autolockMinutes
            .onEach { value -> _uiState.update { it.copy(autolockMinutes = value) } }
            .launchIn(viewModelScope)

        appSettings.topSymptomsCount
            .onEach { value -> _uiState.update { it.copy(topSymptomsCount = value) } }
            .launchIn(viewModelScope)

        appSettings.showMoodInSummary
            .onEach { value -> _uiState.update { it.copy(showMood = value) } }
            .launchIn(viewModelScope)

        appSettings.showEnergyInSummary
            .onEach { value -> _uiState.update { it.copy(showEnergy = value) } }
            .launchIn(viewModelScope)

        appSettings.showLibidoInSummary
            .onEach { value -> _uiState.update { it.copy(showLibido = value) } }
            .launchIn(viewModelScope)

        appSettings.showFollicularPhase
            .onEach { value -> _uiState.update { it.copy(showFollicular = value) } }
            .launchIn(viewModelScope)

        appSettings.showOvulationPhase
            .onEach { value -> _uiState.update { it.copy(showOvulation = value) } }
            .launchIn(viewModelScope)

        appSettings.showLutealPhase
            .onEach { value -> _uiState.update { it.copy(showLuteal = value) } }
            .launchIn(viewModelScope)

        appSettings.menstruationColor
            .onEach { value -> _uiState.update { it.copy(menstruationColorHex = value) } }
            .launchIn(viewModelScope)

        appSettings.follicularColor
            .onEach { value -> _uiState.update { it.copy(follicularColorHex = value) } }
            .launchIn(viewModelScope)

        appSettings.ovulationColor
            .onEach { value -> _uiState.update { it.copy(ovulationColorHex = value) } }
            .launchIn(viewModelScope)

        appSettings.lutealColor
            .onEach { value -> _uiState.update { it.copy(lutealColorHex = value) } }
            .launchIn(viewModelScope)

        appSettings.reminderPeriodEnabled
            .onEach { value -> _uiState.update { it.copy(periodReminderEnabled = value) } }
            .launchIn(viewModelScope)

        appSettings.reminderPeriodDaysBefore
            .onEach { value -> _uiState.update { it.copy(periodDaysBefore = value) } }
            .launchIn(viewModelScope)

        appSettings.reminderPeriodPrivacyAccepted
            .onEach { value -> _uiState.update { it.copy(periodPrivacyAccepted = value) } }
            .launchIn(viewModelScope)

        appSettings.reminderMedicationEnabled
            .onEach { value -> _uiState.update { it.copy(medicationReminderEnabled = value) } }
            .launchIn(viewModelScope)

        appSettings.reminderMedicationHour
            .onEach { value -> _uiState.update { it.copy(medicationHour = value) } }
            .launchIn(viewModelScope)

        appSettings.reminderMedicationMinute
            .onEach { value -> _uiState.update { it.copy(medicationMinute = value) } }
            .launchIn(viewModelScope)

        appSettings.reminderHydrationEnabled
            .onEach { value -> _uiState.update { it.copy(hydrationReminderEnabled = value) } }
            .launchIn(viewModelScope)

        appSettings.reminderHydrationGoalCups
            .onEach { value -> _uiState.update { it.copy(hydrationGoalCups = value) } }
            .launchIn(viewModelScope)

        appSettings.reminderHydrationFrequencyHours
            .onEach { value -> _uiState.update { it.copy(hydrationFrequencyHours = value) } }
            .launchIn(viewModelScope)

        appSettings.reminderHydrationStartHour
            .onEach { value -> _uiState.update { it.copy(hydrationStartHour = value) } }
            .launchIn(viewModelScope)

        appSettings.reminderHydrationEndHour
            .onEach { value -> _uiState.update { it.copy(hydrationEndHour = value) } }
            .launchIn(viewModelScope)
    }

    /**
     * The single public entry point for all UI interactions.
     *
     * Updates state synchronously via [reduce], then launches side effects
     * (DataStore writes, scheduler calls) asynchronously in [viewModelScope].
     */
    fun onEvent(event: SettingsEvent) {
        _uiState.update { reduce(it, event) }

        // Side effects: persist to DataStore and schedule/cancel reminders.
        when (event) {
            is SettingsEvent.ThemeModeChanged -> viewModelScope.launch {
                appSettings.setThemeMode(event.mode.key)
            }

            is SettingsEvent.AutolockChanged -> viewModelScope.launch {
                appSettings.setAutolockMinutes(event.minutes)
            }

            is SettingsEvent.TopSymptomsCountChanged -> viewModelScope.launch {
                appSettings.setTopSymptomsCount(event.count)
            }

            is SettingsEvent.ShowMoodToggled -> viewModelScope.launch {
                appSettings.setShowMoodInSummary(event.enabled)
            }

            is SettingsEvent.ShowEnergyToggled -> viewModelScope.launch {
                appSettings.setShowEnergyInSummary(event.enabled)
            }

            is SettingsEvent.ShowLibidoToggled -> viewModelScope.launch {
                appSettings.setShowLibidoInSummary(event.enabled)
            }

            is SettingsEvent.ShowFollicularToggled -> viewModelScope.launch {
                appSettings.setShowFollicularPhase(event.enabled)
            }

            is SettingsEvent.ShowOvulationToggled -> viewModelScope.launch {
                appSettings.setShowOvulationPhase(event.enabled)
            }

            is SettingsEvent.ShowLutealToggled -> viewModelScope.launch {
                appSettings.setShowLutealPhase(event.enabled)
            }

            is SettingsEvent.MenstruationColorChanged -> viewModelScope.launch {
                appSettings.setMenstruationColor(event.hex)
            }

            is SettingsEvent.FollicularColorChanged -> viewModelScope.launch {
                appSettings.setFollicularColor(event.hex)
            }

            is SettingsEvent.OvulationColorChanged -> viewModelScope.launch {
                appSettings.setOvulationColor(event.hex)
            }

            is SettingsEvent.LutealColorChanged -> viewModelScope.launch {
                appSettings.setLutealColor(event.hex)
            }

            is SettingsEvent.ResetPhaseColorsToDefaults -> viewModelScope.launch {
                appSettings.setMenstruationColor(CyclePhaseColors.DEFAULT_MENSTRUATION_HEX)
                appSettings.setFollicularColor(CyclePhaseColors.DEFAULT_FOLLICULAR_HEX)
                appSettings.setOvulationColor(CyclePhaseColors.DEFAULT_OVULATION_HEX)
                appSettings.setLutealColor(CyclePhaseColors.DEFAULT_LUTEAL_HEX)
            }

            is SettingsEvent.PeriodReminderToggled -> viewModelScope.launch {
                appSettings.setReminderPeriodEnabled(event.enabled)
                reminderScheduler.schedulePeriodPrediction(event.enabled)
            }

            is SettingsEvent.PeriodDaysBeforeChanged -> viewModelScope.launch {
                appSettings.setReminderPeriodDaysBefore(event.days)
            }

            is SettingsEvent.PeriodPrivacyAccepted -> viewModelScope.launch {
                appSettings.setReminderPeriodPrivacyAccepted(true)
                appSettings.setReminderPeriodEnabled(true)
                reminderScheduler.schedulePeriodPrediction(true)
            }

            is SettingsEvent.MedicationReminderToggled -> {
                val state = _uiState.value
                viewModelScope.launch {
                    appSettings.setReminderMedicationEnabled(event.enabled)
                    reminderScheduler.scheduleMedication(
                        event.enabled,
                        state.medicationHour,
                        state.medicationMinute
                    )
                }
            }

            is SettingsEvent.MedicationHourChanged -> {
                val state = _uiState.value
                viewModelScope.launch {
                    appSettings.setReminderMedicationHour(event.hour)
                    reminderScheduler.scheduleMedication(true, event.hour, state.medicationMinute)
                }
            }

            is SettingsEvent.MedicationMinuteChanged -> {
                val state = _uiState.value
                viewModelScope.launch {
                    appSettings.setReminderMedicationMinute(event.minute)
                    reminderScheduler.scheduleMedication(true, state.medicationHour, event.minute)
                }
            }

            is SettingsEvent.HydrationReminderToggled -> {
                val state = _uiState.value
                viewModelScope.launch {
                    appSettings.setReminderHydrationEnabled(event.enabled)
                    reminderScheduler.scheduleHydration(event.enabled, state.hydrationFrequencyHours)
                }
            }

            is SettingsEvent.HydrationGoalCupsChanged -> viewModelScope.launch {
                appSettings.setReminderHydrationGoalCups(event.cups)
            }

            is SettingsEvent.HydrationFrequencyChanged -> viewModelScope.launch {
                appSettings.setReminderHydrationFrequencyHours(event.hours)
                reminderScheduler.scheduleHydration(true, event.hours)
            }

            is SettingsEvent.HydrationStartHourChanged -> viewModelScope.launch {
                appSettings.setReminderHydrationStartHour(event.hour)
            }

            is SettingsEvent.HydrationEndHourChanged -> viewModelScope.launch {
                appSettings.setReminderHydrationEndHour(event.hour)
            }

            // Dialog events are UI-only state changes — no side effects needed.
            is SettingsEvent.ShowAboutDialog,
            is SettingsEvent.DismissAboutDialog,
            is SettingsEvent.ShowPrivacyDialog,
            is SettingsEvent.DismissPrivacyDialog,
            is SettingsEvent.ShowPermissionRationale,
            is SettingsEvent.DismissPermissionRationale -> { /* state-only */ }
        }
    }

    /**
     * Pure function that returns the new [SettingsUiState] for a given event.
     *
     * Contains no side effects — all DataStore writes and scheduler calls are
     * handled in [onEvent] after the state has been updated.
     */
    private fun reduce(state: SettingsUiState, event: SettingsEvent): SettingsUiState {
        return when (event) {
            is SettingsEvent.ThemeModeChanged -> state.copy(themeMode = event.mode)
            is SettingsEvent.AutolockChanged -> state.copy(autolockMinutes = event.minutes)
            is SettingsEvent.TopSymptomsCountChanged -> state.copy(topSymptomsCount = event.count)

            is SettingsEvent.ShowMoodToggled -> state.copy(showMood = event.enabled)
            is SettingsEvent.ShowEnergyToggled -> state.copy(showEnergy = event.enabled)
            is SettingsEvent.ShowLibidoToggled -> state.copy(showLibido = event.enabled)

            is SettingsEvent.ShowFollicularToggled -> state.copy(showFollicular = event.enabled)
            is SettingsEvent.ShowOvulationToggled -> state.copy(showOvulation = event.enabled)
            is SettingsEvent.ShowLutealToggled -> state.copy(showLuteal = event.enabled)

            is SettingsEvent.MenstruationColorChanged -> state.copy(menstruationColorHex = event.hex)
            is SettingsEvent.FollicularColorChanged -> state.copy(follicularColorHex = event.hex)
            is SettingsEvent.OvulationColorChanged -> state.copy(ovulationColorHex = event.hex)
            is SettingsEvent.LutealColorChanged -> state.copy(lutealColorHex = event.hex)
            is SettingsEvent.ResetPhaseColorsToDefaults -> state.copy(
                menstruationColorHex = CyclePhaseColors.DEFAULT_MENSTRUATION_HEX,
                follicularColorHex = CyclePhaseColors.DEFAULT_FOLLICULAR_HEX,
                ovulationColorHex = CyclePhaseColors.DEFAULT_OVULATION_HEX,
                lutealColorHex = CyclePhaseColors.DEFAULT_LUTEAL_HEX,
            )

            is SettingsEvent.PeriodReminderToggled -> state.copy(periodReminderEnabled = event.enabled)
            is SettingsEvent.PeriodDaysBeforeChanged -> state.copy(periodDaysBefore = event.days)
            is SettingsEvent.PeriodPrivacyAccepted -> state.copy(
                periodPrivacyAccepted = true,
                periodReminderEnabled = true,
                showPrivacyDialog = false,
            )

            is SettingsEvent.MedicationReminderToggled -> state.copy(medicationReminderEnabled = event.enabled)
            is SettingsEvent.MedicationHourChanged -> state.copy(medicationHour = event.hour)
            is SettingsEvent.MedicationMinuteChanged -> state.copy(medicationMinute = event.minute)

            is SettingsEvent.HydrationReminderToggled -> state.copy(hydrationReminderEnabled = event.enabled)
            is SettingsEvent.HydrationGoalCupsChanged -> state.copy(hydrationGoalCups = event.cups)
            is SettingsEvent.HydrationFrequencyChanged -> state.copy(hydrationFrequencyHours = event.hours)
            is SettingsEvent.HydrationStartHourChanged -> state.copy(hydrationStartHour = event.hour)
            is SettingsEvent.HydrationEndHourChanged -> state.copy(hydrationEndHour = event.hour)

            is SettingsEvent.ShowAboutDialog -> state.copy(showAboutDialog = true)
            is SettingsEvent.DismissAboutDialog -> state.copy(showAboutDialog = false)
            is SettingsEvent.ShowPrivacyDialog -> state.copy(showPrivacyDialog = true)
            is SettingsEvent.DismissPrivacyDialog -> state.copy(showPrivacyDialog = false)
            is SettingsEvent.ShowPermissionRationale -> state.copy(showPermissionRationale = true)
            is SettingsEvent.DismissPermissionRationale -> state.copy(showPermissionRationale = false)
        }
    }
}
