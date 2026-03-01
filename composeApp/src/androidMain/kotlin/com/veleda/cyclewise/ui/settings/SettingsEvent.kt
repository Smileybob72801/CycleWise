package com.veleda.cyclewise.ui.settings

import com.veleda.cyclewise.ui.theme.ThemeMode

/**
 * All events that can be dispatched to [SettingsViewModel.onEvent].
 *
 * Covers every user interaction on the Settings pager: security changes, display
 * and phase visibility toggles, phase color customization, reminder configuration,
 * and dialog show/dismiss actions. Each event maps to a pure state transition in
 * [SettingsViewModel.reduce] plus optional side effects (DataStore writes,
 * [ReminderScheduler] calls).
 */
sealed interface SettingsEvent {

    // ── Security ─────────────────────────────────────────────────────

    /** User selected a new auto-lock timeout from the segmented button row. */
    data class AutolockChanged(val minutes: Int) : SettingsEvent

    // ── Insights ─────────────────────────────────────────────────────

    /** User adjusted the "top symptoms" slider (1-5). */
    data class TopSymptomsCountChanged(val count: Int) : SettingsEvent

    // ── Display toggles ──────────────────────────────────────────────

    /** User toggled "Show Mood in summary". */
    data class ShowMoodToggled(val enabled: Boolean) : SettingsEvent

    /** User toggled "Show Energy in summary". */
    data class ShowEnergyToggled(val enabled: Boolean) : SettingsEvent

    /** User toggled "Show Libido in summary". */
    data class ShowLibidoToggled(val enabled: Boolean) : SettingsEvent

    // ── Theme ─────────────────────────────────────────────────────────

    /** User selected a new theme mode from the segmented button row. */
    data class ThemeModeChanged(val mode: ThemeMode) : SettingsEvent

    // ── Phase visibility toggles ─────────────────────────────────────

    /** User toggled "Show Follicular phase on calendar". */
    data class ShowFollicularToggled(val enabled: Boolean) : SettingsEvent

    /** User toggled "Show Ovulation phase on calendar". */
    data class ShowOvulationToggled(val enabled: Boolean) : SettingsEvent

    /** User toggled "Show Luteal phase on calendar". */
    data class ShowLutealToggled(val enabled: Boolean) : SettingsEvent

    // ── Phase color customization ────────────────────────────────────

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

    // ── Tutorial ──────────────────────────────────────────────────────

    /** User tapped "Reset Tutorial Hints" to re-show guided walkthroughs. */
    data object ResetTutorialHints : SettingsEvent

    // ── Period prediction reminder ───────────────────────────────────

    /** User toggled the period prediction reminder switch. */
    data class PeriodReminderToggled(val enabled: Boolean) : SettingsEvent

    /** User selected a "days before" chip for the period prediction reminder. */
    data class PeriodDaysBeforeChanged(val days: Int) : SettingsEvent

    /** User accepted the period reminder privacy dialog. */
    data object PeriodPrivacyAccepted : SettingsEvent

    // ── Medication reminder ──────────────────────────────────────────

    /** User toggled the daily medication reminder switch. */
    data class MedicationReminderToggled(val enabled: Boolean) : SettingsEvent

    /** User changed the medication reminder hour slider. */
    data class MedicationHourChanged(val hour: Int) : SettingsEvent

    /** User changed the medication reminder minute slider. */
    data class MedicationMinuteChanged(val minute: Int) : SettingsEvent

    // ── Hydration reminder ───────────────────────────────────────────

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

    // ── Dialogs ──────────────────────────────────────────────────────

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

    // ── Legal ───────────────────────────────────────────────────────

    /** User tapped "Privacy Policy" to view the privacy policy dialog. */
    data object ShowPrivacyPolicyDialog : SettingsEvent

    /** User dismissed the privacy policy dialog. */
    data object DismissPrivacyPolicyDialog : SettingsEvent

    /** User tapped "Terms of Service" to view the terms dialog. */
    data object ShowTermsOfServiceDialog : SettingsEvent

    /** User dismissed the terms of service dialog. */
    data object DismissTermsOfServiceDialog : SettingsEvent

    // ── Educational ──────────────────────────────────────────────────

    /** The user tapped an info button to view educational content for the given [contentTag]. */
    data class ShowEducationalSheet(val contentTag: String) : SettingsEvent

    /** The user dismissed the educational bottom sheet. */
    data object DismissEducationalSheet : SettingsEvent
}
