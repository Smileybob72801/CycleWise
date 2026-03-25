package com.veleda.cyclewise.ui.settings

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veleda.cyclewise.domain.models.BackupMetadata
import com.veleda.cyclewise.domain.models.EducationalArticle
import com.veleda.cyclewise.domain.providers.EducationalContentProvider
import com.veleda.cyclewise.domain.usecases.DeleteAllDataUseCase
import com.veleda.cyclewise.reminders.ReminderScheduler
import com.veleda.cyclewise.services.BackupException
import com.veleda.cyclewise.services.BackupManager
import com.veleda.cyclewise.session.ChangePassphraseResult
import com.veleda.cyclewise.session.SessionManager
import com.veleda.cyclewise.settings.AppSettings
import com.veleda.cyclewise.ui.auth.MIN_PASSPHRASE_LENGTH
import com.veleda.cyclewise.ui.backup.ImportStep
import com.veleda.cyclewise.ui.coachmark.HintPreferences
import com.veleda.cyclewise.ui.theme.ThemeMode
import com.veleda.cyclewise.ui.tracker.CyclePhaseColors
import com.veleda.cyclewise.ui.tracker.HeatmapMetricColors
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

/**
 * One-shot side effects emitted by [SettingsViewModel] and consumed by the UI.
 *
 * Follows the same pattern as [PassphraseEffect] — uses `replay = 0` so effects
 * are not replayed on recomposition or resubscription.
 */
sealed interface SettingsEffect {
    /** All data has been deleted; the UI should navigate to the passphrase setup screen. */
    data object DataDeleted : SettingsEffect

    /** The passphrase was changed successfully; the UI should show a confirmation toast. */
    data object PassphraseChanged : SettingsEffect

    /** Launch the SAF file picker for creating a backup file. */
    data class LaunchExportPicker(val suggestedName: String) : SettingsEffect

    /** Launch the SAF file picker for selecting a `.rwbackup` file to import. */
    data object LaunchImportPicker : SettingsEffect

    /** Backup was exported successfully; show a success message. */
    data object ExportSuccess : SettingsEffect

    /** Backup was imported successfully; navigate to passphrase screen for re-login. */
    data object BackupImported : SettingsEffect
}

/**
 * UI state for the Security settings page (page 0).
 *
 * Contains autolock timeout, passphrase change dialog state, and data deletion
 * confirmation flow.
 *
 * @property autolockMinutes              Auto-lock timeout in minutes (default 10).
 * @property showChangePassphraseDialog   Whether the Change Passphrase dialog is visible.
 * @property changePassphraseError        Error key for the change passphrase dialog (`"wrong_current"`,
 *                                        `"too_short"`, `"mismatch"`, or `null`).
 * @property isChangingPassphrase         Whether a passphrase change is currently in progress.
 * @property showPassphraseSuccessDialog  Whether the post-change success dialog is visible.
 * @property showDeleteFirstConfirmation  Whether the first "Delete All Data?" warning dialog is visible.
 * @property showDeleteSecondConfirmation Whether the second "type DELETE" confirmation dialog is visible.
 * @property deleteConfirmText            Current text in the second dialog's confirmation field.
 * @property isDeletingData               Whether a data wipe is currently in progress.
 * @property isExporting                  Whether a backup export is in progress.
 * @property exportError                  Error message from the last export attempt, or `null`.
 * @property importStep                   Current step in the multi-dialog import flow.
 * @property importMetadata               Parsed metadata from the selected backup archive.
 * @property importUri                    SAF URI of the selected backup file.
 * @property importPassphraseError        Inline error for the backup passphrase dialog.
 * @property importConfirmText            Current text in the "type OVERWRITE" confirmation field.
 * @property importError                  Error message from the import flow, or `null`.
 * @property isVerifyingPassphrase        Whether passphrase verification is in progress.
 * @property importPassphrase             The verified passphrase for the import (stored transiently
 *                                        during the confirmation flow, cleared after import).
 */
data class SecuritySettingsState(
    val autolockMinutes: Int = 10,
    val showChangePassphraseDialog: Boolean = false,
    val changePassphraseError: String? = null,
    val isChangingPassphrase: Boolean = false,
    val showPassphraseSuccessDialog: Boolean = false,
    val showDeleteFirstConfirmation: Boolean = false,
    val showDeleteSecondConfirmation: Boolean = false,
    val deleteConfirmText: String = "",
    val isDeletingData: Boolean = false,
    val isExporting: Boolean = false,
    val exportError: String? = null,
    val importStep: ImportStep = ImportStep.IDLE,
    val importMetadata: BackupMetadata? = null,
    val importUri: Uri? = null,
    val importPassphraseError: String? = null,
    val importConfirmText: String = "",
    val importError: String? = null,
    val isVerifyingPassphrase: Boolean = false,
    val importPassphrase: String? = null,
)

/**
 * UI state for the Appearance settings page (page 1).
 *
 * Contains theme mode, display toggles, phase visibility, and top symptoms count.
 *
 * @property themeMode         User-selected theme mode (default [ThemeMode.SYSTEM]).
 * @property showMood          Whether mood is shown in the daily log summary.
 * @property showEnergy        Whether energy is shown in the daily log summary.
 * @property showLibido        Whether libido is shown in the daily log summary.
 * @property showFollicular    Whether the Follicular phase tint is visible on the calendar.
 * @property showOvulation     Whether the Ovulation phase tint is visible on the calendar.
 * @property showLuteal        Whether the Luteal phase tint is visible on the calendar.
 * @property topSymptomsCount  Number of top symptoms shown in insights (default 3).
 */
data class AppearanceSettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showMood: Boolean = true,
    val showEnergy: Boolean = true,
    val showLibido: Boolean = true,
    val showFollicular: Boolean = true,
    val showOvulation: Boolean = true,
    val showLuteal: Boolean = true,
    val topSymptomsCount: Int = 3,
)

/**
 * UI state for the Colors settings page (page 2).
 *
 * Contains all phase and heatmap color customization hex values plus the
 * educational bottom sheet articles.
 *
 * @property menstruationColorHex           6-char hex color for the Menstruation phase.
 * @property follicularColorHex             6-char hex color for the Follicular phase.
 * @property ovulationColorHex              6-char hex color for the Ovulation phase.
 * @property lutealColorHex                 6-char hex color for the Luteal phase.
 * @property heatmapMoodColorHex            6-char hex color for the Mood heatmap metric.
 * @property heatmapEnergyColorHex          6-char hex color for the Energy heatmap metric.
 * @property heatmapLibidoColorHex          6-char hex color for the Libido heatmap metric.
 * @property heatmapWaterIntakeColorHex     6-char hex color for the Water Intake heatmap metric.
 * @property heatmapSymptomSeverityColorHex 6-char hex color for the Symptom Severity heatmap metric.
 * @property heatmapFlowIntensityColorHex   6-char hex color for the Flow Intensity heatmap metric.
 * @property heatmapMedicationCountColorHex 6-char hex color for the Medication Count heatmap metric.
 * @property educationalArticles            Articles to display in the educational bottom sheet, or null when the sheet is hidden.
 */
data class ColorsSettingsState(
    val menstruationColorHex: String = CyclePhaseColors.DEFAULT_MENSTRUATION_HEX,
    val follicularColorHex: String = CyclePhaseColors.DEFAULT_FOLLICULAR_HEX,
    val ovulationColorHex: String = CyclePhaseColors.DEFAULT_OVULATION_HEX,
    val lutealColorHex: String = CyclePhaseColors.DEFAULT_LUTEAL_HEX,
    val heatmapMoodColorHex: String = HeatmapMetricColors.DEFAULT_MOOD_HEX,
    val heatmapEnergyColorHex: String = HeatmapMetricColors.DEFAULT_ENERGY_HEX,
    val heatmapLibidoColorHex: String = HeatmapMetricColors.DEFAULT_LIBIDO_HEX,
    val heatmapWaterIntakeColorHex: String = HeatmapMetricColors.DEFAULT_WATER_INTAKE_HEX,
    val heatmapSymptomSeverityColorHex: String = HeatmapMetricColors.DEFAULT_SYMPTOM_SEVERITY_HEX,
    val heatmapFlowIntensityColorHex: String = HeatmapMetricColors.DEFAULT_FLOW_INTENSITY_HEX,
    val heatmapMedicationCountColorHex: String = HeatmapMetricColors.DEFAULT_MEDICATION_COUNT_HEX,
    val educationalArticles: List<EducationalArticle>? = null,
)

/**
 * UI state for the Notifications settings page (page 3).
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
 * UI state for the About settings page (page 4).
 *
 * @property showAboutDialog           Whether the About dialog is visible.
 * @property showPrivacyPolicyDialog   Whether the Privacy Policy dialog is visible.
 * @property showTermsOfServiceDialog  Whether the Terms of Service dialog is visible.
 * @property showHintResetConfirmation Whether the hint-reset confirmation toast should show.
 */
data class AboutSettingsState(
    val showAboutDialog: Boolean = false,
    val showPrivacyPolicyDialog: Boolean = false,
    val showTermsOfServiceDialog: Boolean = false,
    val showHintResetConfirmation: Boolean = false,
)

/**
 * Settings screen ViewModel following the MVI pattern with pure reduce functions.
 *
 * Exposes five separate [StateFlow]s — one per pager page — so that changes to one
 * page's state only trigger recomposition of that page. Each sub-state has a
 * corresponding pure reduce function ([reduceSecurity], [reduceAppearance],
 * [reduceColors], [reduceNotification], [reduceAbout]) that handles synchronous
 * state transitions.
 *
 * Collects all [AppSettings] preference flows in its `init` block via individual
 * `Flow.onEach { }.launchIn(viewModelScope)` collectors. User interactions are dispatched
 * through [onEvent], which applies a pure state update via the appropriate reduce
 * function, then launches side effects (DataStore writes, [ReminderScheduler] calls)
 * in `viewModelScope`.
 *
 * Singleton-scoped (no database access needed). Session-specific operations
 * (Lock Now, Debug Seeder) are handled at the Screen level via [SessionManager]
 * or Koin scope access.
 *
 * @param appSettings          The [AppSettings] for reading/writing preferences.
 * @param reminderScheduler    The [ReminderScheduler] for notification scheduling.
 * @param hintPreferences      The [HintPreferences] for managing tutorial hint state.
 * @param deleteAllDataUseCase The [DeleteAllDataUseCase] for wiping all user data.
 * @param sessionManager       The [SessionManager] for session scope lifecycle operations.
 */
class SettingsViewModel(
    private val appSettings: AppSettings,
    private val reminderScheduler: ReminderScheduler,
    private val educationalContentProvider: EducationalContentProvider,
    private val hintPreferences: HintPreferences,
    private val deleteAllDataUseCase: DeleteAllDataUseCase,
    private val sessionManager: SessionManager,
    private val backupManager: BackupManager,
) : ViewModel() {

    private val _securityState = MutableStateFlow(SecuritySettingsState())

    /** Observable state for the Security settings page. */
    val securityState: StateFlow<SecuritySettingsState> = _securityState.asStateFlow()

    private val _appearanceState = MutableStateFlow(AppearanceSettingsState())

    /** Observable state for the Appearance settings page. */
    val appearanceState: StateFlow<AppearanceSettingsState> = _appearanceState.asStateFlow()

    private val _colorsState = MutableStateFlow(ColorsSettingsState())

    /** Observable state for the Colors settings page. */
    val colorsState: StateFlow<ColorsSettingsState> = _colorsState.asStateFlow()

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
        // ── Security state collectors ────────────────────────────────
        appSettings.autolockMinutes
            .onEach { value -> _securityState.update { it.copy(autolockMinutes = value) } }
            .launchIn(viewModelScope)

        // ── Appearance state collectors ──────────────────────────────
        appSettings.topSymptomsCount
            .onEach { value -> _appearanceState.update { it.copy(topSymptomsCount = value) } }
            .launchIn(viewModelScope)

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

        // ── Colors state collectors ──────────────────────────────────
        appSettings.menstruationColor
            .onEach { value -> _colorsState.update { it.copy(menstruationColorHex = value) } }
            .launchIn(viewModelScope)

        appSettings.follicularColor
            .onEach { value -> _colorsState.update { it.copy(follicularColorHex = value) } }
            .launchIn(viewModelScope)

        appSettings.ovulationColor
            .onEach { value -> _colorsState.update { it.copy(ovulationColorHex = value) } }
            .launchIn(viewModelScope)

        appSettings.lutealColor
            .onEach { value -> _colorsState.update { it.copy(lutealColorHex = value) } }
            .launchIn(viewModelScope)

        appSettings.heatmapMoodColor
            .onEach { value -> _colorsState.update { it.copy(heatmapMoodColorHex = value) } }
            .launchIn(viewModelScope)

        appSettings.heatmapEnergyColor
            .onEach { value -> _colorsState.update { it.copy(heatmapEnergyColorHex = value) } }
            .launchIn(viewModelScope)

        appSettings.heatmapLibidoColor
            .onEach { value -> _colorsState.update { it.copy(heatmapLibidoColorHex = value) } }
            .launchIn(viewModelScope)

        appSettings.heatmapWaterIntakeColor
            .onEach { value -> _colorsState.update { it.copy(heatmapWaterIntakeColorHex = value) } }
            .launchIn(viewModelScope)

        appSettings.heatmapSymptomSeverityColor
            .onEach { value -> _colorsState.update { it.copy(heatmapSymptomSeverityColorHex = value) } }
            .launchIn(viewModelScope)

        appSettings.heatmapFlowIntensityColor
            .onEach { value -> _colorsState.update { it.copy(heatmapFlowIntensityColorHex = value) } }
            .launchIn(viewModelScope)

        appSettings.heatmapMedicationCountColor
            .onEach { value -> _colorsState.update { it.copy(heatmapMedicationCountColorHex = value) } }
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
     * Applies a pure state update via the appropriate reduce function, then launches
     * side effects (DataStore writes, scheduler calls) asynchronously in [viewModelScope].
     */
    fun onEvent(event: SettingsEvent) {
        when (event) {
            // ── Security state events ────────────────────────────────
            is SettingsEvent.AutolockChanged,
            is SettingsEvent.ChangePassphraseRequested,
            is SettingsEvent.ChangePassphraseDismissed,
            is SettingsEvent.ChangePassphraseSubmitted,
            is SettingsEvent.ChangePassphraseSuccessAcknowledged,
            is SettingsEvent.DeleteAllDataRequested,
            is SettingsEvent.DeleteAllDataCancelled,
            is SettingsEvent.DeleteAllDataFirstConfirmed,
            is SettingsEvent.DeleteConfirmTextChanged,
            is SettingsEvent.DeleteAllDataConfirmed,
            is SettingsEvent.ExportBackupClicked,
            is SettingsEvent.ExportToUri,
            is SettingsEvent.ImportBackupClicked,
            is SettingsEvent.ImportFileSelected,
            is SettingsEvent.ImportMetadataConfirmed,
            is SettingsEvent.ImportPassphraseEntered,
            is SettingsEvent.ImportFirstWarningConfirmed,
            is SettingsEvent.ImportConfirmTextChanged,
            is SettingsEvent.ImportSecondConfirmed,
            is SettingsEvent.ImportDismissed ->
                _securityState.update { reduceSecurity(it, event) }

            // ── Appearance state events ──────────────────────────────
            is SettingsEvent.ThemeModeChanged,
            is SettingsEvent.ShowMoodToggled,
            is SettingsEvent.ShowEnergyToggled,
            is SettingsEvent.ShowLibidoToggled,
            is SettingsEvent.ShowFollicularToggled,
            is SettingsEvent.ShowOvulationToggled,
            is SettingsEvent.ShowLutealToggled,
            is SettingsEvent.TopSymptomsCountChanged ->
                _appearanceState.update { reduceAppearance(it, event) }

            // ── Colors state events ──────────────────────────────────
            is SettingsEvent.MenstruationColorChanged,
            is SettingsEvent.FollicularColorChanged,
            is SettingsEvent.OvulationColorChanged,
            is SettingsEvent.LutealColorChanged,
            is SettingsEvent.ResetPhaseColorsToDefaults,
            is SettingsEvent.HeatmapColorChanged,
            is SettingsEvent.ResetHeatmapColorsToDefaults,
            is SettingsEvent.ShowEducationalSheet,
            is SettingsEvent.DismissEducationalSheet ->
                _colorsState.update { reduceColors(it, event) }

            // ── Notification state events ────────────────────────────
            is SettingsEvent.PeriodReminderToggled,
            is SettingsEvent.PeriodDaysBeforeChanged,
            is SettingsEvent.PeriodPrivacyAccepted,
            is SettingsEvent.MedicationReminderToggled,
            is SettingsEvent.MedicationHourChanged,
            is SettingsEvent.MedicationMinuteChanged,
            is SettingsEvent.HydrationReminderToggled,
            is SettingsEvent.HydrationGoalCupsChanged,
            is SettingsEvent.HydrationFrequencyChanged,
            is SettingsEvent.HydrationStartHourChanged,
            is SettingsEvent.HydrationEndHourChanged,
            is SettingsEvent.ShowPrivacyDialog,
            is SettingsEvent.DismissPrivacyDialog,
            is SettingsEvent.ShowPermissionRationale,
            is SettingsEvent.DismissPermissionRationale ->
                _notificationState.update { reduceNotification(it, event) }

            // ── About state events ───────────────────────────────────
            is SettingsEvent.ShowAboutDialog,
            is SettingsEvent.DismissAboutDialog,
            is SettingsEvent.ShowPrivacyPolicyDialog,
            is SettingsEvent.DismissPrivacyPolicyDialog,
            is SettingsEvent.ShowTermsOfServiceDialog,
            is SettingsEvent.DismissTermsOfServiceDialog,
            is SettingsEvent.ResetTutorialHints ->
                _aboutState.update { reduceAbout(it, event) }
        }

        // ── Side effects ─────────────────────────────────────────────
        when (event) {
            is SettingsEvent.AutolockChanged ->
                viewModelScope.launch { appSettings.setAutolockMinutes(event.minutes) }

            is SettingsEvent.TopSymptomsCountChanged ->
                viewModelScope.launch { appSettings.setTopSymptomsCount(event.count) }

            is SettingsEvent.ChangePassphraseSubmitted -> {
                if (_securityState.value.isChangingPassphrase) {
                    launchChangePassphrase(event)
                }
            }

            is SettingsEvent.ChangePassphraseSuccessAcknowledged -> {
                viewModelScope.launch {
                    sessionManager.closeSession()
                    _effect.emit(SettingsEffect.PassphraseChanged)
                }
            }

            is SettingsEvent.ResetTutorialHints -> {
                viewModelScope.launch {
                    hintPreferences.resetAll()
                    _aboutState.update { it.copy(showHintResetConfirmation = true) }
                }
            }

            is SettingsEvent.DeleteAllDataConfirmed -> {
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        sessionManager.closeSession()
                        deleteAllDataUseCase()
                    }
                    _effect.emit(SettingsEffect.DataDeleted)
                }
            }

            is SettingsEvent.ThemeModeChanged ->
                viewModelScope.launch { appSettings.setThemeMode(event.mode.key) }

            is SettingsEvent.ShowMoodToggled ->
                viewModelScope.launch { appSettings.setShowMoodInSummary(event.enabled) }

            is SettingsEvent.ShowEnergyToggled ->
                viewModelScope.launch { appSettings.setShowEnergyInSummary(event.enabled) }

            is SettingsEvent.ShowLibidoToggled ->
                viewModelScope.launch { appSettings.setShowLibidoInSummary(event.enabled) }

            is SettingsEvent.ShowFollicularToggled ->
                viewModelScope.launch { appSettings.setShowFollicularPhase(event.enabled) }

            is SettingsEvent.ShowOvulationToggled ->
                viewModelScope.launch { appSettings.setShowOvulationPhase(event.enabled) }

            is SettingsEvent.ShowLutealToggled ->
                viewModelScope.launch { appSettings.setShowLutealPhase(event.enabled) }

            is SettingsEvent.MenstruationColorChanged ->
                viewModelScope.launch { appSettings.setMenstruationColor(event.hex) }

            is SettingsEvent.FollicularColorChanged ->
                viewModelScope.launch { appSettings.setFollicularColor(event.hex) }

            is SettingsEvent.OvulationColorChanged ->
                viewModelScope.launch { appSettings.setOvulationColor(event.hex) }

            is SettingsEvent.LutealColorChanged ->
                viewModelScope.launch { appSettings.setLutealColor(event.hex) }

            is SettingsEvent.ResetPhaseColorsToDefaults -> {
                viewModelScope.launch {
                    appSettings.setMenstruationColor(CyclePhaseColors.DEFAULT_MENSTRUATION_HEX)
                    appSettings.setFollicularColor(CyclePhaseColors.DEFAULT_FOLLICULAR_HEX)
                    appSettings.setOvulationColor(CyclePhaseColors.DEFAULT_OVULATION_HEX)
                    appSettings.setLutealColor(CyclePhaseColors.DEFAULT_LUTEAL_HEX)
                }
            }

            is SettingsEvent.HeatmapColorChanged -> viewModelScope.launch {
                when (event.metricKey) {
                    "mood" -> appSettings.setHeatmapMoodColor(event.hex)
                    "energy" -> appSettings.setHeatmapEnergyColor(event.hex)
                    "libido" -> appSettings.setHeatmapLibidoColor(event.hex)
                    "water" -> appSettings.setHeatmapWaterIntakeColor(event.hex)
                    "symptom_severity" -> appSettings.setHeatmapSymptomSeverityColor(event.hex)
                    "flow" -> appSettings.setHeatmapFlowIntensityColor(event.hex)
                    "medications" -> appSettings.setHeatmapMedicationCountColor(event.hex)
                }
            }

            is SettingsEvent.ResetHeatmapColorsToDefaults -> viewModelScope.launch {
                appSettings.setHeatmapMoodColor(HeatmapMetricColors.DEFAULT_MOOD_HEX)
                appSettings.setHeatmapEnergyColor(HeatmapMetricColors.DEFAULT_ENERGY_HEX)
                appSettings.setHeatmapLibidoColor(HeatmapMetricColors.DEFAULT_LIBIDO_HEX)
                appSettings.setHeatmapWaterIntakeColor(HeatmapMetricColors.DEFAULT_WATER_INTAKE_HEX)
                appSettings.setHeatmapSymptomSeverityColor(HeatmapMetricColors.DEFAULT_SYMPTOM_SEVERITY_HEX)
                appSettings.setHeatmapFlowIntensityColor(HeatmapMetricColors.DEFAULT_FLOW_INTENSITY_HEX)
                appSettings.setHeatmapMedicationCountColor(HeatmapMetricColors.DEFAULT_MEDICATION_COUNT_HEX)
            }

            is SettingsEvent.ShowEducationalSheet -> {
                val articles = educationalContentProvider.getByTag(event.contentTag)
                _colorsState.update { it.copy(educationalArticles = articles.ifEmpty { null }) }
            }

            is SettingsEvent.PeriodReminderToggled -> {
                viewModelScope.launch {
                    appSettings.setReminderPeriodEnabled(event.enabled)
                    reminderScheduler.schedulePeriodPrediction(event.enabled)
                }
            }

            is SettingsEvent.PeriodDaysBeforeChanged ->
                viewModelScope.launch { appSettings.setReminderPeriodDaysBefore(event.days) }

            is SettingsEvent.PeriodPrivacyAccepted -> {
                viewModelScope.launch {
                    appSettings.setReminderPeriodPrivacyAccepted(true)
                    appSettings.setReminderPeriodEnabled(true)
                    reminderScheduler.schedulePeriodPrediction(true)
                }
            }

            is SettingsEvent.MedicationReminderToggled -> {
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
                val state = _notificationState.value
                viewModelScope.launch {
                    appSettings.setReminderMedicationHour(event.hour)
                    reminderScheduler.scheduleMedication(true, event.hour, state.medicationMinute)
                }
            }

            is SettingsEvent.MedicationMinuteChanged -> {
                val state = _notificationState.value
                viewModelScope.launch {
                    appSettings.setReminderMedicationMinute(event.minute)
                    reminderScheduler.scheduleMedication(true, state.medicationHour, event.minute)
                }
            }

            is SettingsEvent.HydrationReminderToggled -> {
                val state = _notificationState.value
                viewModelScope.launch {
                    appSettings.setReminderHydrationEnabled(event.enabled)
                    reminderScheduler.scheduleHydration(event.enabled, state.hydrationFrequencyHours)
                }
            }

            is SettingsEvent.HydrationGoalCupsChanged ->
                viewModelScope.launch { appSettings.setReminderHydrationGoalCups(event.cups) }

            is SettingsEvent.HydrationFrequencyChanged -> {
                viewModelScope.launch {
                    appSettings.setReminderHydrationFrequencyHours(event.hours)
                    reminderScheduler.scheduleHydration(true, event.hours)
                }
            }

            is SettingsEvent.HydrationStartHourChanged ->
                viewModelScope.launch { appSettings.setReminderHydrationStartHour(event.hour) }

            is SettingsEvent.HydrationEndHourChanged ->
                viewModelScope.launch { appSettings.setReminderHydrationEndHour(event.hour) }

            is SettingsEvent.ExportBackupClicked -> {
                viewModelScope.launch {
                    _effect.emit(
                        SettingsEffect.LaunchExportPicker(BackupManager.suggestedFilename()),
                    )
                }
            }

            is SettingsEvent.ExportToUri -> launchExport(event.uri)

            is SettingsEvent.ImportBackupClicked -> {
                viewModelScope.launch { _effect.emit(SettingsEffect.LaunchImportPicker) }
            }

            is SettingsEvent.ImportFileSelected -> launchValidateBackup(event.uri)

            is SettingsEvent.ImportPassphraseEntered -> launchVerifyPassphrase(event.passphrase)

            is SettingsEvent.ImportSecondConfirmed -> launchImport()

            // State-only events — no side effects needed.
            is SettingsEvent.ChangePassphraseRequested,
            is SettingsEvent.ChangePassphraseDismissed,
            is SettingsEvent.ShowPrivacyPolicyDialog,
            is SettingsEvent.DismissPrivacyPolicyDialog,
            is SettingsEvent.ShowTermsOfServiceDialog,
            is SettingsEvent.DismissTermsOfServiceDialog,
            is SettingsEvent.DeleteAllDataRequested,
            is SettingsEvent.DeleteAllDataCancelled,
            is SettingsEvent.DeleteAllDataFirstConfirmed,
            is SettingsEvent.DeleteConfirmTextChanged,
            is SettingsEvent.DismissEducationalSheet,
            is SettingsEvent.ShowPrivacyDialog,
            is SettingsEvent.DismissPrivacyDialog,
            is SettingsEvent.ShowPermissionRationale,
            is SettingsEvent.DismissPermissionRationale,
            is SettingsEvent.ShowAboutDialog,
            is SettingsEvent.DismissAboutDialog,
            is SettingsEvent.ImportMetadataConfirmed,
            is SettingsEvent.ImportFirstWarningConfirmed,
            is SettingsEvent.ImportConfirmTextChanged,
            is SettingsEvent.ImportDismissed -> { /* state-only */ }
        }
    }

    /**
     * Pure function that returns the new [SecuritySettingsState] for a given event.
     *
     * Handles autolock, passphrase change validation, and data deletion confirmation
     * flow. Contains no side effects.
     */
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun reduceSecurity(state: SecuritySettingsState, event: SettingsEvent): SecuritySettingsState {
        return when (event) {
            is SettingsEvent.AutolockChanged ->
                state.copy(autolockMinutes = event.minutes)

            is SettingsEvent.ChangePassphraseRequested ->
                state.copy(showChangePassphraseDialog = true)

            is SettingsEvent.ChangePassphraseDismissed ->
                state.copy(
                    showChangePassphraseDialog = false,
                    changePassphraseError = null,
                    showPassphraseSuccessDialog = false,
                )

            is SettingsEvent.ChangePassphraseSubmitted -> {
                when {
                    event.newPassphrase.length < MIN_PASSPHRASE_LENGTH ->
                        state.copy(changePassphraseError = "too_short")
                    event.newPassphrase != event.confirmation ->
                        state.copy(changePassphraseError = "mismatch")
                    else ->
                        state.copy(isChangingPassphrase = true, changePassphraseError = null)
                }
            }

            is SettingsEvent.ChangePassphraseSuccessAcknowledged ->
                state.copy(
                    showChangePassphraseDialog = false,
                    showPassphraseSuccessDialog = false,
                )

            is SettingsEvent.DeleteAllDataRequested ->
                state.copy(showDeleteFirstConfirmation = true)

            is SettingsEvent.DeleteAllDataCancelled ->
                state.copy(
                    showDeleteFirstConfirmation = false,
                    showDeleteSecondConfirmation = false,
                    deleteConfirmText = "",
                )

            is SettingsEvent.DeleteAllDataFirstConfirmed ->
                state.copy(
                    showDeleteFirstConfirmation = false,
                    showDeleteSecondConfirmation = true,
                    deleteConfirmText = "",
                )

            is SettingsEvent.DeleteConfirmTextChanged ->
                state.copy(deleteConfirmText = event.text)

            is SettingsEvent.DeleteAllDataConfirmed ->
                state.copy(
                    showDeleteSecondConfirmation = false,
                    deleteConfirmText = "",
                    isDeletingData = true,
                )

            // ── Backup & Restore ────────────────────────────────────────

            is SettingsEvent.ExportBackupClicked -> state // effect-only
            is SettingsEvent.ExportToUri -> state.copy(isExporting = true, exportError = null)

            is SettingsEvent.ImportBackupClicked -> state // effect-only
            is SettingsEvent.ImportFileSelected -> state // side effect validates

            is SettingsEvent.ImportMetadataConfirmed ->
                state.copy(importStep = ImportStep.PASSPHRASE_ENTRY, importPassphraseError = null)

            is SettingsEvent.ImportPassphraseEntered ->
                state.copy(isVerifyingPassphrase = true, importPassphraseError = null)

            is SettingsEvent.ImportFirstWarningConfirmed ->
                state.copy(importStep = ImportStep.SECOND_CONFIRM, importConfirmText = "")

            is SettingsEvent.ImportConfirmTextChanged ->
                state.copy(importConfirmText = event.text)

            is SettingsEvent.ImportSecondConfirmed ->
                state.copy(importStep = ImportStep.IMPORTING, importConfirmText = "")

            is SettingsEvent.ImportDismissed -> state.copy(
                importStep = ImportStep.IDLE,
                importMetadata = null,
                importUri = null,
                importPassphraseError = null,
                importConfirmText = "",
                importError = null,
                isVerifyingPassphrase = false,
                importPassphrase = null,
            )

            else -> state
        }
    }

    /**
     * Pure function that returns the new [AppearanceSettingsState] for a given event.
     *
     * Handles theme mode, display toggles, phase visibility, and top symptoms count.
     * Contains no side effects — DataStore writes are handled in [onEvent].
     */
    private fun reduceAppearance(state: AppearanceSettingsState, event: SettingsEvent): AppearanceSettingsState {
        return when (event) {
            is SettingsEvent.ThemeModeChanged ->
                state.copy(themeMode = event.mode)

            is SettingsEvent.ShowMoodToggled ->
                state.copy(showMood = event.enabled)

            is SettingsEvent.ShowEnergyToggled ->
                state.copy(showEnergy = event.enabled)

            is SettingsEvent.ShowLibidoToggled ->
                state.copy(showLibido = event.enabled)

            is SettingsEvent.ShowFollicularToggled ->
                state.copy(showFollicular = event.enabled)

            is SettingsEvent.ShowOvulationToggled ->
                state.copy(showOvulation = event.enabled)

            is SettingsEvent.ShowLutealToggled ->
                state.copy(showLuteal = event.enabled)

            is SettingsEvent.TopSymptomsCountChanged ->
                state.copy(topSymptomsCount = event.count)

            else -> state
        }
    }

    /**
     * Pure function that returns the new [ColorsSettingsState] for a given event.
     *
     * Handles phase colors, heatmap colors, and educational sheet visibility.
     * Contains no side effects — content provider queries and DataStore writes
     * are handled in [onEvent].
     */
    private fun reduceColors(state: ColorsSettingsState, event: SettingsEvent): ColorsSettingsState {
        return when (event) {
            is SettingsEvent.MenstruationColorChanged ->
                state.copy(menstruationColorHex = event.hex)

            is SettingsEvent.FollicularColorChanged ->
                state.copy(follicularColorHex = event.hex)

            is SettingsEvent.OvulationColorChanged ->
                state.copy(ovulationColorHex = event.hex)

            is SettingsEvent.LutealColorChanged ->
                state.copy(lutealColorHex = event.hex)

            is SettingsEvent.ResetPhaseColorsToDefaults ->
                state.copy(
                    menstruationColorHex = CyclePhaseColors.DEFAULT_MENSTRUATION_HEX,
                    follicularColorHex = CyclePhaseColors.DEFAULT_FOLLICULAR_HEX,
                    ovulationColorHex = CyclePhaseColors.DEFAULT_OVULATION_HEX,
                    lutealColorHex = CyclePhaseColors.DEFAULT_LUTEAL_HEX,
                )

            is SettingsEvent.HeatmapColorChanged -> when (event.metricKey) {
                "mood" -> state.copy(heatmapMoodColorHex = event.hex)
                "energy" -> state.copy(heatmapEnergyColorHex = event.hex)
                "libido" -> state.copy(heatmapLibidoColorHex = event.hex)
                "water" -> state.copy(heatmapWaterIntakeColorHex = event.hex)
                "symptom_severity" -> state.copy(heatmapSymptomSeverityColorHex = event.hex)
                "flow" -> state.copy(heatmapFlowIntensityColorHex = event.hex)
                "medications" -> state.copy(heatmapMedicationCountColorHex = event.hex)
                else -> state
            }

            is SettingsEvent.ResetHeatmapColorsToDefaults ->
                state.copy(
                    heatmapMoodColorHex = HeatmapMetricColors.DEFAULT_MOOD_HEX,
                    heatmapEnergyColorHex = HeatmapMetricColors.DEFAULT_ENERGY_HEX,
                    heatmapLibidoColorHex = HeatmapMetricColors.DEFAULT_LIBIDO_HEX,
                    heatmapWaterIntakeColorHex = HeatmapMetricColors.DEFAULT_WATER_INTAKE_HEX,
                    heatmapSymptomSeverityColorHex = HeatmapMetricColors.DEFAULT_SYMPTOM_SEVERITY_HEX,
                    heatmapFlowIntensityColorHex = HeatmapMetricColors.DEFAULT_FLOW_INTENSITY_HEX,
                    heatmapMedicationCountColorHex = HeatmapMetricColors.DEFAULT_MEDICATION_COUNT_HEX,
                )

            is SettingsEvent.ShowEducationalSheet -> state

            is SettingsEvent.DismissEducationalSheet ->
                state.copy(educationalArticles = null)

            else -> state
        }
    }

    /**
     * Pure function that returns the new [NotificationSettingsState] for a given event.
     *
     * Handles period, medication, and hydration reminder configuration, plus
     * privacy dialog and permission rationale visibility. Contains no side effects.
     */
    private fun reduceNotification(state: NotificationSettingsState, event: SettingsEvent): NotificationSettingsState {
        return when (event) {
            is SettingsEvent.PeriodReminderToggled ->
                state.copy(periodReminderEnabled = event.enabled)

            is SettingsEvent.PeriodDaysBeforeChanged ->
                state.copy(periodDaysBefore = event.days)

            is SettingsEvent.PeriodPrivacyAccepted ->
                state.copy(
                    periodPrivacyAccepted = true,
                    periodReminderEnabled = true,
                    showPrivacyDialog = false,
                )

            is SettingsEvent.MedicationReminderToggled ->
                state.copy(medicationReminderEnabled = event.enabled)

            is SettingsEvent.MedicationHourChanged ->
                state.copy(medicationHour = event.hour)

            is SettingsEvent.MedicationMinuteChanged ->
                state.copy(medicationMinute = event.minute)

            is SettingsEvent.HydrationReminderToggled ->
                state.copy(hydrationReminderEnabled = event.enabled)

            is SettingsEvent.HydrationGoalCupsChanged ->
                state.copy(hydrationGoalCups = event.cups)

            is SettingsEvent.HydrationFrequencyChanged ->
                state.copy(hydrationFrequencyHours = event.hours)

            is SettingsEvent.HydrationStartHourChanged ->
                state.copy(hydrationStartHour = event.hour)

            is SettingsEvent.HydrationEndHourChanged ->
                state.copy(hydrationEndHour = event.hour)

            is SettingsEvent.ShowPrivacyDialog ->
                state.copy(showPrivacyDialog = true)

            is SettingsEvent.DismissPrivacyDialog ->
                state.copy(showPrivacyDialog = false)

            is SettingsEvent.ShowPermissionRationale ->
                state.copy(showPermissionRationale = true)

            is SettingsEvent.DismissPermissionRationale ->
                state.copy(showPermissionRationale = false)

            else -> state
        }
    }

    /**
     * Pure function that returns the new [AboutSettingsState] for a given event.
     *
     * Handles about dialog, legal dialog visibility, and tutorial hint reset.
     * Contains no side effects.
     */
    private fun reduceAbout(state: AboutSettingsState, event: SettingsEvent): AboutSettingsState {
        return when (event) {
            is SettingsEvent.ShowAboutDialog ->
                state.copy(showAboutDialog = true)

            is SettingsEvent.DismissAboutDialog ->
                state.copy(showAboutDialog = false)

            is SettingsEvent.ShowPrivacyPolicyDialog ->
                state.copy(showPrivacyPolicyDialog = true)

            is SettingsEvent.DismissPrivacyPolicyDialog ->
                state.copy(showPrivacyPolicyDialog = false)

            is SettingsEvent.ShowTermsOfServiceDialog ->
                state.copy(showTermsOfServiceDialog = true)

            is SettingsEvent.DismissTermsOfServiceDialog ->
                state.copy(showTermsOfServiceDialog = false)

            is SettingsEvent.ResetTutorialHints -> state

            else -> state
        }
    }

    /**
     * Validates inputs via [reduceSecurity] and delegates the passphrase change to [SessionManager].
     *
     * Called only when [reduceSecurity] transitions [SecuritySettingsState.isChangingPassphrase]
     * to `true` (validation passed). Delegates to [SessionManager.changePassphrase] which
     * handles key derivation, fingerprint verification, database rekey, and fingerprint
     * update on the IO dispatcher.
     *
     * On any failure, sets [SecuritySettingsState.changePassphraseError] and clears the
     * loading state.
     */
    private fun launchChangePassphrase(event: SettingsEvent.ChangePassphraseSubmitted) {
        viewModelScope.launch {
            val result = sessionManager.changePassphrase(event.current, event.newPassphrase)
            val errorKey = when (result) {
                ChangePassphraseResult.Success -> null
                ChangePassphraseResult.WrongCurrent -> "wrong_current"
                ChangePassphraseResult.VerificationFailed -> "verification_failed"
                ChangePassphraseResult.Failed -> "failed"
            }

            if (errorKey != null) {
                _securityState.update {
                    it.copy(changePassphraseError = errorKey, isChangingPassphrase = false)
                }
            } else {
                _securityState.update {
                    it.copy(
                        isChangingPassphrase = false,
                        changePassphraseError = null,
                        showPassphraseSuccessDialog = true,
                    )
                }
            }
        }
    }

    // ── Backup & Restore side effects ───────────────────────────────

    /**
     * Exports the encrypted database to the SAF URI.
     *
     * Gets the open database from [SessionManager], runs WAL checkpoint and
     * ZIP creation via [BackupManager], then emits [SettingsEffect.ExportSuccess]
     * or updates state with the error.
     */
    private fun launchExport(uri: Uri) {
        viewModelScope.launch {
            try {
                val db = sessionManager.getOpenDatabase()
                    ?: throw BackupException.IoError("No active session")
                backupManager.exportBackup(uri, db)
                _securityState.update { it.copy(isExporting = false) }
                _effect.emit(SettingsEffect.ExportSuccess)
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Log.e("SettingsVM", "Export failed", e)
                _securityState.update {
                    it.copy(isExporting = false, exportError = e.message)
                }
            }
        }
    }

    /**
     * Validates the selected backup file and shows the metadata preview dialog.
     */
    private fun launchValidateBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                val metadata = backupManager.validateBackup(uri)
                _securityState.update {
                    it.copy(
                        importStep = ImportStep.METADATA_PREVIEW,
                        importMetadata = metadata,
                        importUri = uri,
                        importError = null,
                    )
                }
            } catch (e: BackupException.SchemaVersionTooNew) {
                _securityState.update {
                    it.copy(importError = "schema_too_new:${e.backup}:${e.current}")
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Log.e("SettingsVM", "Backup validation failed", e)
                _securityState.update { it.copy(importError = e.message) }
            }
        }
    }

    /**
     * Verifies the passphrase against the imported backup's database.
     *
     * On success, advances to the first overwrite warning step.
     * On failure, shows an inline error on the passphrase dialog.
     */
    private fun launchVerifyPassphrase(passphrase: String) {
        viewModelScope.launch {
            val uri = _securityState.value.importUri ?: return@launch
            try {
                backupManager.verifyPassphrase(uri, passphrase)
                _securityState.update {
                    it.copy(
                        importStep = ImportStep.FIRST_WARNING,
                        isVerifyingPassphrase = false,
                        importPassphraseError = null,
                        importPassphrase = passphrase,
                    )
                }
            } catch (e: BackupException.WrongPassphrase) {
                _securityState.update {
                    it.copy(
                        isVerifyingPassphrase = false,
                        importPassphraseError = "wrong_passphrase",
                    )
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Log.e("SettingsVM", "Passphrase verification failed", e)
                _securityState.update {
                    it.copy(
                        isVerifyingPassphrase = false,
                        importPassphraseError = e.message,
                    )
                }
            }
        }
    }

    /**
     * Executes the import: closes the session, replaces the database and salt,
     * and emits [SettingsEffect.BackupImported] to navigate to the passphrase screen.
     */
    private fun launchImport() {
        viewModelScope.launch {
            val uri = _securityState.value.importUri ?: return@launch
            try {
                withContext(Dispatchers.IO) {
                    sessionManager.closeSession()
                    backupManager.importBackup(uri)
                }
                _securityState.update {
                    it.copy(
                        importStep = ImportStep.IDLE,
                        importMetadata = null,
                        importUri = null,
                        importPassphrase = null,
                    )
                }
                _effect.emit(SettingsEffect.BackupImported)
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Log.e("SettingsVM", "Import failed", e)
                _securityState.update {
                    it.copy(
                        importStep = ImportStep.IDLE,
                        importError = e.message,
                        importPassphrase = null,
                    )
                }
            }
        }
    }
}
