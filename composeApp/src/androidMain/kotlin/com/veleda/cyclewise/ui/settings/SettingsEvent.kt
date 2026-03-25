package com.veleda.cyclewise.ui.settings

import android.net.Uri
import com.veleda.cyclewise.ui.theme.ThemeMode

/**
 * All events that can be dispatched to [SettingsViewModel.onEvent].
 *
 * Covers every user interaction on the Settings pager: security changes, display
 * and phase visibility toggles, phase and heatmap color customization, reminder
 * configuration, and dialog show/dismiss actions. Each event maps to a pure state
 * transition in [SettingsViewModel.onEvent] plus optional side effects (DataStore
 * writes, [ReminderScheduler] calls).
 *
 * Events are grouped by the page they belong to: Security, Appearance, Colors,
 * Notifications, and About.
 */
sealed interface SettingsEvent {

    // ── Security (page 0) ────────────────────────────────────────────

    /** User selected a new auto-lock timeout from the segmented button row. */
    data class AutolockChanged(val minutes: Int) : SettingsEvent

    /** User tapped "Change Passphrase" to open the change passphrase dialog. */
    data object ChangePassphraseRequested : SettingsEvent

    /** User dismissed the change passphrase dialog without submitting. */
    data object ChangePassphraseDismissed : SettingsEvent

    /**
     * User submitted the change passphrase form.
     *
     * The ViewModel validates the inputs, verifies the current passphrase against the
     * encrypted database, and re-keys the database with the new passphrase-derived key.
     *
     * @property current       the user's current passphrase for verification.
     * @property newPassphrase the desired new passphrase (must be >= 8 characters).
     * @property confirmation  re-entry of the new passphrase (must match [newPassphrase]).
     */
    data class ChangePassphraseSubmitted(
        val current: String,
        val newPassphrase: String,
        val confirmation: String,
    ) : SettingsEvent

    /** User acknowledged the passphrase change success dialog and confirmed they saved the new passphrase. */
    data object ChangePassphraseSuccessAcknowledged : SettingsEvent

    // ── Security (page 0) — Backup & Restore ────────────────────────

    /** User tapped "Export Backup" to begin the export flow. */
    data object ExportBackupClicked : SettingsEvent

    /** SAF returned the URI for the export file. */
    data class ExportToUri(val uri: Uri) : SettingsEvent

    /** User tapped "Import Backup" to begin the import flow. */
    data object ImportBackupClicked : SettingsEvent

    /** SAF returned the URI of the selected `.rwbackup` file. */
    data class ImportFileSelected(val uri: Uri) : SettingsEvent

    /** User confirmed the metadata preview dialog to proceed with import. */
    data object ImportMetadataConfirmed : SettingsEvent

    /** User submitted the passphrase for the backup. */
    data class ImportPassphraseEntered(val passphrase: String) : SettingsEvent

    /** User confirmed the first overwrite warning dialog. */
    data object ImportFirstWarningConfirmed : SettingsEvent

    /** User updated the text in the "type OVERWRITE" confirmation field. */
    data class ImportConfirmTextChanged(val text: String) : SettingsEvent

    /** User typed "OVERWRITE" and tapped confirm — execute the import. */
    data object ImportSecondConfirmed : SettingsEvent

    /** User cancelled the import at any step. */
    data object ImportDismissed : SettingsEvent

    // ── Appearance (page 1) — Insights ─────────────────────────────

    /** User adjusted the "top symptoms" slider (1-5). */
    data class TopSymptomsCountChanged(val count: Int) : SettingsEvent

    // ── Appearance (page 1) — Display toggles ──────────────────────

    /** User toggled "Show Mood in summary". */
    data class ShowMoodToggled(val enabled: Boolean) : SettingsEvent

    /** User toggled "Show Energy in summary". */
    data class ShowEnergyToggled(val enabled: Boolean) : SettingsEvent

    /** User toggled "Show Libido in summary". */
    data class ShowLibidoToggled(val enabled: Boolean) : SettingsEvent

    // ── Appearance (page 1) — Theme ─────────────────────────────────

    /** User selected a new theme mode from the segmented button row. */
    data class ThemeModeChanged(val mode: ThemeMode) : SettingsEvent

    // ── Appearance (page 1) — Phase visibility toggles ─────────────

    /** User toggled "Show Follicular phase on calendar". */
    data class ShowFollicularToggled(val enabled: Boolean) : SettingsEvent

    /** User toggled "Show Ovulation phase on calendar". */
    data class ShowOvulationToggled(val enabled: Boolean) : SettingsEvent

    /** User toggled "Show Luteal phase on calendar". */
    data class ShowLutealToggled(val enabled: Boolean) : SettingsEvent

    // ── Colors (page 2) — Phase color customization ────────────────

    /** User changed the Menstruation phase hex color. */
    data class MenstruationColorChanged(val hex: String) : SettingsEvent

    /** User changed the Follicular phase hex color. */
    data class FollicularColorChanged(val hex: String) : SettingsEvent

    /** User changed the Ovulation phase hex color. */
    data class OvulationColorChanged(val hex: String) : SettingsEvent

    /** User changed the Luteal phase hex color. */
    data class LutealColorChanged(val hex: String) : SettingsEvent

    /** User tapped "Reset to Defaults" for all phase colors. */
    data object ResetPhaseColorsToDefaults : SettingsEvent

    // ── Colors (page 2) — Heatmap color customization ──────────────

    /**
     * User changed a heatmap metric color.
     *
     * @property metricKey The [HeatmapMetric.key] identifying which metric (e.g. "mood", "energy").
     * @property hex       The new 6-char hex color string (no '#' prefix).
     */
    data class HeatmapColorChanged(val metricKey: String, val hex: String) : SettingsEvent

    /** User tapped "Reset to Defaults" for all heatmap metric colors. */
    data object ResetHeatmapColorsToDefaults : SettingsEvent

    // ── About (page 4) — Tutorial ──────────────────────────────────

    /** User tapped "Reset Tutorial Hints" to re-show guided walkthroughs. */
    data object ResetTutorialHints : SettingsEvent

    // ── Notifications (page 3) — Period prediction reminder ────────

    /** User toggled the period prediction reminder switch. */
    data class PeriodReminderToggled(val enabled: Boolean) : SettingsEvent

    /** User selected a "days before" chip for the period prediction reminder. */
    data class PeriodDaysBeforeChanged(val days: Int) : SettingsEvent

    /** User accepted the period reminder privacy dialog. */
    data object PeriodPrivacyAccepted : SettingsEvent

    // ── Notifications (page 3) — Medication reminder ───────────────

    /** User toggled the daily medication reminder switch. */
    data class MedicationReminderToggled(val enabled: Boolean) : SettingsEvent

    /** User changed the medication reminder hour slider. */
    data class MedicationHourChanged(val hour: Int) : SettingsEvent

    /** User changed the medication reminder minute slider. */
    data class MedicationMinuteChanged(val minute: Int) : SettingsEvent

    // ── Notifications (page 3) — Hydration reminder ────────────────

    /** User toggled the hydration reminder switch. */
    data class HydrationReminderToggled(val enabled: Boolean) : SettingsEvent

    /** User changed the daily hydration goal cups slider. */
    data class HydrationGoalCupsChanged(val cups: Int) : SettingsEvent

    /** User selected a hydration frequency chip (2h, 3h, 4h). */
    data class HydrationFrequencyChanged(val hours: Int) : SettingsEvent

    /** User changed the hydration active window start hour. */
    data class HydrationStartHourChanged(val hour: Int) : SettingsEvent

    /** User changed the hydration active window end hour. */
    data class HydrationEndHourChanged(val hour: Int) : SettingsEvent

    // ── About (page 4) — Dialogs ───────────────────────────────────

    /** User tapped the About list item to show the about dialog. */
    data object ShowAboutDialog : SettingsEvent

    /** User dismissed the about dialog. */
    data object DismissAboutDialog : SettingsEvent

    /** User or ViewModel requests the privacy dialog to be shown. */
    data object ShowPrivacyDialog : SettingsEvent

    /** User dismissed the privacy dialog without accepting. */
    data object DismissPrivacyDialog : SettingsEvent

    /** Notification permission was denied; show rationale text. */
    data object ShowPermissionRationale : SettingsEvent

    /** Permission rationale has been acknowledged or dismissed. */
    data object DismissPermissionRationale : SettingsEvent

    // ── Security (page 0) — Data Management ────────────────────────

    /** User tapped "Delete All Data" to begin the deletion flow. */
    data object DeleteAllDataRequested : SettingsEvent

    /** User dismissed any delete confirmation dialog without proceeding. */
    data object DeleteAllDataCancelled : SettingsEvent

    /** User confirmed the first warning dialog and should see the second confirmation. */
    data object DeleteAllDataFirstConfirmed : SettingsEvent

    /** User updated the text field in the second confirmation dialog. */
    data class DeleteConfirmTextChanged(val text: String) : SettingsEvent

    /** User typed "DELETE" and tapped the final confirm button — execute the wipe. */
    data object DeleteAllDataConfirmed : SettingsEvent

    // ── About (page 4) — Legal ─────────────────────────────────────

    /** User tapped "Privacy Policy" to view the privacy policy dialog. */
    data object ShowPrivacyPolicyDialog : SettingsEvent

    /** User dismissed the privacy policy dialog. */
    data object DismissPrivacyPolicyDialog : SettingsEvent

    /** User tapped "Terms of Service" to view the terms dialog. */
    data object ShowTermsOfServiceDialog : SettingsEvent

    /** User dismissed the terms of service dialog. */
    data object DismissTermsOfServiceDialog : SettingsEvent

    // ── Colors (page 2) — Educational ──────────────────────────────

    /** The user tapped an info button to view educational content for the given [contentTag]. */
    data class ShowEducationalSheet(val contentTag: String) : SettingsEvent

    /** The user dismissed the educational bottom sheet. */
    data object DismissEducationalSheet : SettingsEvent
}
