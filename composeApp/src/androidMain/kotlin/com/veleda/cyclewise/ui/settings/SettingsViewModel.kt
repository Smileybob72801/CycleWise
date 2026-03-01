package com.veleda.cyclewise.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veleda.cyclewise.domain.models.EducationalArticle
import com.veleda.cyclewise.domain.providers.EducationalContentProvider
import com.veleda.cyclewise.domain.usecases.DeleteAllDataUseCase
import com.veleda.cyclewise.reminders.ReminderScheduler
import com.veleda.cyclewise.settings.AppSettings
import com.veleda.cyclewise.ui.coachmark.HintPreferences
import com.veleda.cyclewise.ui.theme.ThemeMode
import com.veleda.cyclewise.ui.tracker.CyclePhaseColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent

/**
 * One-shot side effects emitted by [SettingsViewModel] and consumed by the UI.
 *
 * Follows the same pattern as [PassphraseEffect] — uses `replay = 0` so effects
 * are not replayed on recomposition or resubscription.
 */
sealed interface SettingsEffect {
    /** All data has been deleted; the UI should navigate to the passphrase setup screen. */
    data object DataDeleted : SettingsEffect
}

/**
 * UI state for the General settings page (page 0).
 *
 * Contains security settings, insights configuration, tutorial hint state,
 * legal dialog visibility, and data management dialog state.
 *
 * @property autolockMinutes              Auto-lock timeout in minutes (default 10).
 * @property topSymptomsCount             Number of top symptoms shown in insights (default 3).
 * @property showHintResetConfirmation    Whether the hint-reset confirmation toast should show.
 * @property showPrivacyPolicyDialog      Whether the Privacy Policy dialog is visible.
 * @property showTermsOfServiceDialog     Whether the Terms of Service dialog is visible.
 * @property showDeleteFirstConfirmation  Whether the first "Delete All Data?" warning dialog is visible.
 * @property showDeleteSecondConfirmation Whether the second "type DELETE" confirmation dialog is visible.
 * @property deleteConfirmText            Current text in the second dialog's confirmation field.
 * @property isDeletingData               Whether a data wipe is currently in progress.
 */
data class GeneralSettingsState(
    val autolockMinutes: Int = 10,
    val topSymptomsCount: Int = 3,
    val showHintResetConfirmation: Boolean = false,
    val showPrivacyPolicyDialog: Boolean = false,
    val showTermsOfServiceDialog: Boolean = false,
    val showDeleteFirstConfirmation: Boolean = false,
    val showDeleteSecondConfirmation: Boolean = false,
    val deleteConfirmText: String = "",
    val isDeletingData: Boolean = false,
)

/**
 * UI state for the Appearance settings page (page 1).
 *
 * Contains theme mode, display toggles, phase visibility, phase colors,
 * and educational content state.
 *
 * @property themeMode             User-selected theme mode (default [ThemeMode.SYSTEM]).
 * @property showMood              Whether mood is shown in the daily log summary.
 * @property showEnergy            Whether energy is shown in the daily log summary.
 * @property showLibido            Whether libido is shown in the daily log summary.
 * @property showFollicular        Whether the Follicular phase tint is visible on the calendar.
 * @property showOvulation         Whether the Ovulation phase tint is visible on the calendar.
 * @property showLuteal            Whether the Luteal phase tint is visible on the calendar.
 * @property menstruationColorHex  6-char hex color for the Menstruation phase.
 * @property follicularColorHex    6-char hex color for the Follicular phase.
 * @property ovulationColorHex     6-char hex color for the Ovulation phase.
 * @property lutealColorHex        6-char hex color for the Luteal phase.
 * @property educationalArticles   Articles to display in the educational bottom sheet, or null when the sheet is hidden.
 */
data class AppearanceSettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
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
    val educationalArticles: List<EducationalArticle>? = null,
)

/**
 * UI state for the Notifications settings page (page 2).
 *
 * Contains all reminder configuration and related dialog visibility flags.
 *
 * @property periodReminderEnabled     Whether the period prediction reminder is on.
 * @property periodDaysBefore          Days before predicted period to notify (1-3).
 * @property periodPrivacyAccepted     Whether the user has accepted the period privacy notice.
 * @property medicationReminderEnabled Whether the daily medication reminder is on.
 * @property medicationHour            Hour for the medication reminder (0-23).
 * @property medicationMinute          Minute for the medication reminder (0-59).
 * @property hydrationReminderEnabled  Whether the hydration reminder is on.
 * @property hydrationGoalCups         Daily water goal in cups.
 * @property hydrationFrequencyHours   Interval between hydration reminders in hours.
 * @property hydrationStartHour        Active window start hour for hydration reminders.
 * @property hydrationEndHour          Active window end hour for hydration reminders.
 * @property showPermissionRationale   Whether the notification permission rationale is shown.
 * @property showPrivacyDialog         Whether the period privacy dialog is visible.
 */
data class NotificationSettingsState(
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
    val showPermissionRationale: Boolean = false,
    val showPrivacyDialog: Boolean = false,
)

/**
 * UI state for the About settings page (page 3).
 *
 * @property showAboutDialog Whether the About dialog is visible.
 */
data class AboutSettingsState(
    val showAboutDialog: Boolean = false,
)

/**
 * Settings screen ViewModel following the MVI-inspired pattern.
 *
 * Exposes four separate [StateFlow]s — one per pager page — so that changes to one
 * page's state only trigger recomposition of that page. This replaces the former
 * monolithic `SettingsUiState` to reduce recomposition scope on lower-end devices.
 *
 * Collects all [AppSettings] preference flows in its `init` block via individual
 * `Flow.onEach { }.launchIn(viewModelScope)` collectors. User interactions are dispatched
 * through [onEvent], which applies state updates directly to the relevant sub-state flow
 * and then launches side effects (DataStore writes, [ReminderScheduler] calls) in
 * `viewModelScope`.
 *
 * Singleton-scoped (no database access needed). Session-specific operations
 * (Lock Now, Debug Seeder) are handled at the Screen level via Koin scope access.
 *
 * @param appSettings          The [AppSettings] for reading/writing preferences.
 * @param reminderScheduler    The [ReminderScheduler] for notification scheduling.
 * @param hintPreferences      The [HintPreferences] for managing tutorial hint state.
 * @param deleteAllDataUseCase The [DeleteAllDataUseCase] for wiping all user data.
 */
class SettingsViewModel(
    private val appSettings: AppSettings,
    private val reminderScheduler: ReminderScheduler,
    private val educationalContentProvider: EducationalContentProvider,
    private val hintPreferences: HintPreferences,
    private val deleteAllDataUseCase: DeleteAllDataUseCase,
) : ViewModel(), KoinComponent {

    private val _generalState = MutableStateFlow(GeneralSettingsState())

    /** Observable state for the General settings page. */
    val generalState: StateFlow<GeneralSettingsState> = _generalState.asStateFlow()

    private val _appearanceState = MutableStateFlow(AppearanceSettingsState())

    /** Observable state for the Appearance settings page. */
    val appearanceState: StateFlow<AppearanceSettingsState> = _appearanceState.asStateFlow()

    private val _notificationState = MutableStateFlow(NotificationSettingsState())

    /** Observable state for the Notifications settings page. */
    val notificationState: StateFlow<NotificationSettingsState> = _notificationState.asStateFlow()

    private val _aboutState = MutableStateFlow(AboutSettingsState())

    /** Observable state for the About settings page. */
    val aboutState: StateFlow<AboutSettingsState> = _aboutState.asStateFlow()

    private val _effect = MutableSharedFlow<SettingsEffect>(replay = 0)

    /**
     * One-shot side effects consumed by the UI (navigation after data deletion).
     *
     * Uses `replay = 0` so effects are not replayed on recomposition or resubscription.
     */
    val effect: SharedFlow<SettingsEffect> = _effect.asSharedFlow()

    init {
        // ── General state collectors ─────────────────────────────────
        appSettings.autolockMinutes
            .onEach { value -> _generalState.update { it.copy(autolockMinutes = value) } }
            .launchIn(viewModelScope)

        appSettings.topSymptomsCount
            .onEach { value -> _generalState.update { it.copy(topSymptomsCount = value) } }
            .launchIn(viewModelScope)

        // ── Appearance state collectors ──────────────────────────────
        appSettings.themeMode
            .onEach { value -> _appearanceState.update { it.copy(themeMode = ThemeMode.fromKey(value)) } }
            .launchIn(viewModelScope)

        appSettings.showMoodInSummary
            .onEach { value -> _appearanceState.update { it.copy(showMood = value) } }
            .launchIn(viewModelScope)

        appSettings.showEnergyInSummary
            .onEach { value -> _appearanceState.update { it.copy(showEnergy = value) } }
            .launchIn(viewModelScope)

        appSettings.showLibidoInSummary
            .onEach { value -> _appearanceState.update { it.copy(showLibido = value) } }
            .launchIn(viewModelScope)

        appSettings.showFollicularPhase
            .onEach { value -> _appearanceState.update { it.copy(showFollicular = value) } }
            .launchIn(viewModelScope)

        appSettings.showOvulationPhase
            .onEach { value -> _appearanceState.update { it.copy(showOvulation = value) } }
            .launchIn(viewModelScope)

        appSettings.showLutealPhase
            .onEach { value -> _appearanceState.update { it.copy(showLuteal = value) } }
            .launchIn(viewModelScope)

        appSettings.menstruationColor
            .onEach { value -> _appearanceState.update { it.copy(menstruationColorHex = value) } }
            .launchIn(viewModelScope)

        appSettings.follicularColor
            .onEach { value -> _appearanceState.update { it.copy(follicularColorHex = value) } }
            .launchIn(viewModelScope)

        appSettings.ovulationColor
            .onEach { value -> _appearanceState.update { it.copy(ovulationColorHex = value) } }
            .launchIn(viewModelScope)

        appSettings.lutealColor
            .onEach { value -> _appearanceState.update { it.copy(lutealColorHex = value) } }
            .launchIn(viewModelScope)

        // ── Notification state collectors ────────────────────────────
        appSettings.reminderPeriodEnabled
            .onEach { value -> _notificationState.update { it.copy(periodReminderEnabled = value) } }
            .launchIn(viewModelScope)

        appSettings.reminderPeriodDaysBefore
            .onEach { value -> _notificationState.update { it.copy(periodDaysBefore = value) } }
            .launchIn(viewModelScope)

        appSettings.reminderPeriodPrivacyAccepted
            .onEach { value -> _notificationState.update { it.copy(periodPrivacyAccepted = value) } }
            .launchIn(viewModelScope)

        appSettings.reminderMedicationEnabled
            .onEach { value -> _notificationState.update { it.copy(medicationReminderEnabled = value) } }
            .launchIn(viewModelScope)

        appSettings.reminderMedicationHour
            .onEach { value -> _notificationState.update { it.copy(medicationHour = value) } }
            .launchIn(viewModelScope)

        appSettings.reminderMedicationMinute
            .onEach { value -> _notificationState.update { it.copy(medicationMinute = value) } }
            .launchIn(viewModelScope)

        appSettings.reminderHydrationEnabled
            .onEach { value -> _notificationState.update { it.copy(hydrationReminderEnabled = value) } }
            .launchIn(viewModelScope)

        appSettings.reminderHydrationGoalCups
            .onEach { value -> _notificationState.update { it.copy(hydrationGoalCups = value) } }
            .launchIn(viewModelScope)

        appSettings.reminderHydrationFrequencyHours
            .onEach { value -> _notificationState.update { it.copy(hydrationFrequencyHours = value) } }
            .launchIn(viewModelScope)

        appSettings.reminderHydrationStartHour
            .onEach { value -> _notificationState.update { it.copy(hydrationStartHour = value) } }
            .launchIn(viewModelScope)

        appSettings.reminderHydrationEndHour
            .onEach { value -> _notificationState.update { it.copy(hydrationEndHour = value) } }
            .launchIn(viewModelScope)
    }

    /**
     * The single public entry point for all UI interactions.
     *
     * Updates the relevant sub-state directly, then launches side effects
     * (DataStore writes, scheduler calls) asynchronously in [viewModelScope].
     */
    fun onEvent(event: SettingsEvent) {
        when (event) {
            // ── General state events ─────────────────────────────────
            is SettingsEvent.AutolockChanged -> {
                _generalState.update { it.copy(autolockMinutes = event.minutes) }
                viewModelScope.launch { appSettings.setAutolockMinutes(event.minutes) }
            }

            is SettingsEvent.TopSymptomsCountChanged -> {
                _generalState.update { it.copy(topSymptomsCount = event.count) }
                viewModelScope.launch { appSettings.setTopSymptomsCount(event.count) }
            }

            is SettingsEvent.ResetTutorialHints -> {
                viewModelScope.launch {
                    hintPreferences.resetAll()
                    _generalState.update { it.copy(showHintResetConfirmation = true) }
                }
            }

            is SettingsEvent.ShowPrivacyPolicyDialog ->
                _generalState.update { it.copy(showPrivacyPolicyDialog = true) }

            is SettingsEvent.DismissPrivacyPolicyDialog ->
                _generalState.update { it.copy(showPrivacyPolicyDialog = false) }

            is SettingsEvent.ShowTermsOfServiceDialog ->
                _generalState.update { it.copy(showTermsOfServiceDialog = true) }

            is SettingsEvent.DismissTermsOfServiceDialog ->
                _generalState.update { it.copy(showTermsOfServiceDialog = false) }

            is SettingsEvent.DeleteAllDataRequested ->
                _generalState.update { it.copy(showDeleteFirstConfirmation = true) }

            is SettingsEvent.DeleteAllDataCancelled ->
                _generalState.update {
                    it.copy(
                        showDeleteFirstConfirmation = false,
                        showDeleteSecondConfirmation = false,
                        deleteConfirmText = "",
                    )
                }

            is SettingsEvent.DeleteAllDataFirstConfirmed ->
                _generalState.update {
                    it.copy(
                        showDeleteFirstConfirmation = false,
                        showDeleteSecondConfirmation = true,
                        deleteConfirmText = "",
                    )
                }

            is SettingsEvent.DeleteConfirmTextChanged ->
                _generalState.update { it.copy(deleteConfirmText = event.text) }

            is SettingsEvent.DeleteAllDataConfirmed -> {
                _generalState.update {
                    it.copy(
                        showDeleteSecondConfirmation = false,
                        deleteConfirmText = "",
                        isDeletingData = true,
                    )
                }
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        getKoin().getScopeOrNull("session")?.close()
                        deleteAllDataUseCase()
                    }
                    _effect.emit(SettingsEffect.DataDeleted)
                }
            }

            // ── Appearance state events ──────────────────────────────
            is SettingsEvent.ThemeModeChanged -> {
                _appearanceState.update { it.copy(themeMode = event.mode) }
                viewModelScope.launch { appSettings.setThemeMode(event.mode.key) }
            }

            is SettingsEvent.ShowMoodToggled -> {
                _appearanceState.update { it.copy(showMood = event.enabled) }
                viewModelScope.launch { appSettings.setShowMoodInSummary(event.enabled) }
            }

            is SettingsEvent.ShowEnergyToggled -> {
                _appearanceState.update { it.copy(showEnergy = event.enabled) }
                viewModelScope.launch { appSettings.setShowEnergyInSummary(event.enabled) }
            }

            is SettingsEvent.ShowLibidoToggled -> {
                _appearanceState.update { it.copy(showLibido = event.enabled) }
                viewModelScope.launch { appSettings.setShowLibidoInSummary(event.enabled) }
            }

            is SettingsEvent.ShowFollicularToggled -> {
                _appearanceState.update { it.copy(showFollicular = event.enabled) }
                viewModelScope.launch { appSettings.setShowFollicularPhase(event.enabled) }
            }

            is SettingsEvent.ShowOvulationToggled -> {
                _appearanceState.update { it.copy(showOvulation = event.enabled) }
                viewModelScope.launch { appSettings.setShowOvulationPhase(event.enabled) }
            }

            is SettingsEvent.ShowLutealToggled -> {
                _appearanceState.update { it.copy(showLuteal = event.enabled) }
                viewModelScope.launch { appSettings.setShowLutealPhase(event.enabled) }
            }

            is SettingsEvent.MenstruationColorChanged -> {
                _appearanceState.update { it.copy(menstruationColorHex = event.hex) }
                viewModelScope.launch { appSettings.setMenstruationColor(event.hex) }
            }

            is SettingsEvent.FollicularColorChanged -> {
                _appearanceState.update { it.copy(follicularColorHex = event.hex) }
                viewModelScope.launch { appSettings.setFollicularColor(event.hex) }
            }

            is SettingsEvent.OvulationColorChanged -> {
                _appearanceState.update { it.copy(ovulationColorHex = event.hex) }
                viewModelScope.launch { appSettings.setOvulationColor(event.hex) }
            }

            is SettingsEvent.LutealColorChanged -> {
                _appearanceState.update { it.copy(lutealColorHex = event.hex) }
                viewModelScope.launch { appSettings.setLutealColor(event.hex) }
            }

            is SettingsEvent.ResetPhaseColorsToDefaults -> {
                _appearanceState.update {
                    it.copy(
                        menstruationColorHex = CyclePhaseColors.DEFAULT_MENSTRUATION_HEX,
                        follicularColorHex = CyclePhaseColors.DEFAULT_FOLLICULAR_HEX,
                        ovulationColorHex = CyclePhaseColors.DEFAULT_OVULATION_HEX,
                        lutealColorHex = CyclePhaseColors.DEFAULT_LUTEAL_HEX,
                    )
                }
                viewModelScope.launch {
                    appSettings.setMenstruationColor(CyclePhaseColors.DEFAULT_MENSTRUATION_HEX)
                    appSettings.setFollicularColor(CyclePhaseColors.DEFAULT_FOLLICULAR_HEX)
                    appSettings.setOvulationColor(CyclePhaseColors.DEFAULT_OVULATION_HEX)
                    appSettings.setLutealColor(CyclePhaseColors.DEFAULT_LUTEAL_HEX)
                }
            }

            is SettingsEvent.ShowEducationalSheet -> {
                val articles = educationalContentProvider.getByTag(event.contentTag)
                _appearanceState.update { it.copy(educationalArticles = articles.ifEmpty { null }) }
            }

            is SettingsEvent.DismissEducationalSheet ->
                _appearanceState.update { it.copy(educationalArticles = null) }

            // ── Notification state events ────────────────────────────
            is SettingsEvent.PeriodReminderToggled -> {
                _notificationState.update { it.copy(periodReminderEnabled = event.enabled) }
                viewModelScope.launch {
                    appSettings.setReminderPeriodEnabled(event.enabled)
                    reminderScheduler.schedulePeriodPrediction(event.enabled)
                }
            }

            is SettingsEvent.PeriodDaysBeforeChanged -> {
                _notificationState.update { it.copy(periodDaysBefore = event.days) }
                viewModelScope.launch { appSettings.setReminderPeriodDaysBefore(event.days) }
            }

            is SettingsEvent.PeriodPrivacyAccepted -> {
                _notificationState.update {
                    it.copy(
                        periodPrivacyAccepted = true,
                        periodReminderEnabled = true,
                        showPrivacyDialog = false,
                    )
                }
                viewModelScope.launch {
                    appSettings.setReminderPeriodPrivacyAccepted(true)
                    appSettings.setReminderPeriodEnabled(true)
                    reminderScheduler.schedulePeriodPrediction(true)
                }
            }

            is SettingsEvent.MedicationReminderToggled -> {
                _notificationState.update { it.copy(medicationReminderEnabled = event.enabled) }
                val state = _notificationState.value
                viewModelScope.launch {
                    appSettings.setReminderMedicationEnabled(event.enabled)
                    reminderScheduler.scheduleMedication(
                        event.enabled,
                        state.medicationHour,
                        state.medicationMinute,
                    )
                }
            }

            is SettingsEvent.MedicationHourChanged -> {
                _notificationState.update { it.copy(medicationHour = event.hour) }
                val state = _notificationState.value
                viewModelScope.launch {
                    appSettings.setReminderMedicationHour(event.hour)
                    reminderScheduler.scheduleMedication(true, event.hour, state.medicationMinute)
                }
            }

            is SettingsEvent.MedicationMinuteChanged -> {
                _notificationState.update { it.copy(medicationMinute = event.minute) }
                val state = _notificationState.value
                viewModelScope.launch {
                    appSettings.setReminderMedicationMinute(event.minute)
                    reminderScheduler.scheduleMedication(true, state.medicationHour, event.minute)
                }
            }

            is SettingsEvent.HydrationReminderToggled -> {
                _notificationState.update { it.copy(hydrationReminderEnabled = event.enabled) }
                val state = _notificationState.value
                viewModelScope.launch {
                    appSettings.setReminderHydrationEnabled(event.enabled)
                    reminderScheduler.scheduleHydration(event.enabled, state.hydrationFrequencyHours)
                }
            }

            is SettingsEvent.HydrationGoalCupsChanged -> {
                _notificationState.update { it.copy(hydrationGoalCups = event.cups) }
                viewModelScope.launch { appSettings.setReminderHydrationGoalCups(event.cups) }
            }

            is SettingsEvent.HydrationFrequencyChanged -> {
                _notificationState.update { it.copy(hydrationFrequencyHours = event.hours) }
                viewModelScope.launch {
                    appSettings.setReminderHydrationFrequencyHours(event.hours)
                    reminderScheduler.scheduleHydration(true, event.hours)
                }
            }

            is SettingsEvent.HydrationStartHourChanged -> {
                _notificationState.update { it.copy(hydrationStartHour = event.hour) }
                viewModelScope.launch { appSettings.setReminderHydrationStartHour(event.hour) }
            }

            is SettingsEvent.HydrationEndHourChanged -> {
                _notificationState.update { it.copy(hydrationEndHour = event.hour) }
                viewModelScope.launch { appSettings.setReminderHydrationEndHour(event.hour) }
            }

            is SettingsEvent.ShowPrivacyDialog ->
                _notificationState.update { it.copy(showPrivacyDialog = true) }

            is SettingsEvent.DismissPrivacyDialog ->
                _notificationState.update { it.copy(showPrivacyDialog = false) }

            is SettingsEvent.ShowPermissionRationale ->
                _notificationState.update { it.copy(showPermissionRationale = true) }

            is SettingsEvent.DismissPermissionRationale ->
                _notificationState.update { it.copy(showPermissionRationale = false) }

            // ── About state events ───────────────────────────────────
            is SettingsEvent.ShowAboutDialog ->
                _aboutState.update { it.copy(showAboutDialog = true) }

            is SettingsEvent.DismissAboutDialog ->
                _aboutState.update { it.copy(showAboutDialog = false) }
        }
    }
}
