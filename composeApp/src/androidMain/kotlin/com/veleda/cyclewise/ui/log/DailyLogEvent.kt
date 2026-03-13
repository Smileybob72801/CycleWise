package com.veleda.cyclewise.ui.log

import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.PeriodColor
import com.veleda.cyclewise.domain.models.PeriodConsistency
import com.veleda.cyclewise.domain.models.Symptom

/**
 * All events that can be dispatched to [DailyLogViewModel.onEvent].
 *
 * Includes data-loading events (dispatched internally by the ViewModel's init block),
 * user-interaction events (from the UI), the period toggle, and water actions.
 * Every user-interaction event auto-saves to the repository — there is no manual save event.
 */
sealed interface DailyLogEvent {
    /** Internal event dispatched when the initial log, symptom library, and medication library have been fetched. */
    data class LogLoaded(val log: FullDailyLog, val initialSymptoms: List<Symptom>, val initialMedications: List<Medication>) : DailyLogEvent

    /** Internal event dispatched when the symptom or medication library changes after init. */
    data class LibraryUpdated(val symptoms: List<Symptom>, val medications: List<Medication>) : DailyLogEvent

    /** The user changed the flow intensity for this day's period log. */
    data class FlowIntensityChanged(val intensity: FlowIntensity?) : DailyLogEvent

    /** The user changed the mood score (1-5 scale). */
    data class MoodScoreChanged(val score: Int) : DailyLogEvent

    /** The user changed the energy level (1-5 scale). */
    data class EnergyLevelChanged(val level: Int) : DailyLogEvent

    /** The user changed the libido score (1-5 scale). */
    data class LibidoScoreChanged(val score: Int) : DailyLogEvent

    /** The user changed the period blood color for this day's period log. */
    data class PeriodColorChanged(val color: PeriodColor?) : DailyLogEvent

    /** The user changed the period consistency for this day's period log. */
    data class PeriodConsistencyChanged(val consistency: PeriodConsistency?) : DailyLogEvent

    /** The user edited the free-text note. Debounced before auto-save. */
    data class NoteChanged(val text: String) : DailyLogEvent

    /** The user added a custom tag to this day's entry. */
    data class TagAdded(val tag: String) : DailyLogEvent

    /** The user removed a custom tag from this day's entry. */
    data class TagRemoved(val tag: String) : DailyLogEvent

    /** The user toggled an existing symptom on or off for this day. */
    data class SymptomToggled(val symptom: Symptom) : DailyLogEvent

    /** The user typed into the "add symptom" text field (used for UI state only, no persistence). */
    data class SymptomNameChanged(val name: String) : DailyLogEvent

    /** The user submitted a new symptom name to create in the library and immediately log. */
    data class CreateAndAddSymptom(val name: String) : DailyLogEvent

    /** The user toggled an existing medication on or off for this day. */
    data class MedicationToggled(val medication: Medication) : DailyLogEvent

    /** The user submitted a new medication name to create in the library and immediately log. */
    data class MedicationCreatedAndAdded(val name: String) : DailyLogEvent

    /**
     * Toggles the period state for this day.
     *
     * When [isOnPeriod] is `true`, calls [PeriodRepository.logPeriodDay] to mark the date
     * as a period day (extending, merging, or creating a period as needed). When `false`,
     * calls [PeriodRepository.unLogPeriodDay] to unmark it (shrinking, splitting, or
     * deleting the containing period). Uses the same repository methods as the Tracker's
     * long-press mark/unmark for consistent behavior.
     */
    data class PeriodToggled(val isOnPeriod: Boolean) : DailyLogEvent

    /** The user tapped the increment button on the water counter. */
    data object WaterIncrement : DailyLogEvent

    /** The user tapped the decrement button on the water counter. */
    data object WaterDecrement : DailyLogEvent

    // ── Educational ──────────────────────────────────────────────────

    /** The user tapped an info button to view educational content for the given [contentTag]. */
    data class ShowEducationalSheet(val contentTag: String) : DailyLogEvent

    /** The user dismissed the educational bottom sheet. */
    data object DismissEducationalSheet : DailyLogEvent

    /** The user dismissed the transient error message (e.g. Snackbar auto-dismissed). */
    data object ErrorDismissed : DailyLogEvent

    // ── Symptom Library Edit/Delete ─────────────────────────────────

    /** The user long-pressed a symptom chip to open its context menu. */
    data class SymptomLongPressed(val symptom: Symptom) : DailyLogEvent

    /** The user tapped "Rename" in the symptom context menu. */
    data class RenameSymptomClicked(val symptom: Symptom) : DailyLogEvent

    /** The user confirmed a symptom rename with the new name. */
    data class RenameSymptomConfirmed(val symptomId: String, val newName: String) : DailyLogEvent

    /** The user tapped "Delete" in the symptom context menu (fetches log count before showing dialog). */
    data class DeleteSymptomClicked(val symptom: Symptom) : DailyLogEvent

    /** The user confirmed deletion of a symptom from the library. */
    data class DeleteSymptomConfirmed(val symptomId: String) : DailyLogEvent

    /** The user dismissed the symptom context menu, rename dialog, or delete dialog. */
    data object SymptomEditDismissed : DailyLogEvent

    // ── Medication Library Edit/Delete ──────────────────────────────

    /** The user long-pressed a medication chip to open its context menu. */
    data class MedicationLongPressed(val medication: Medication) : DailyLogEvent

    /** The user tapped "Rename" in the medication context menu. */
    data class RenameMedicationClicked(val medication: Medication) : DailyLogEvent

    /** The user confirmed a medication rename with the new name. */
    data class RenameMedicationConfirmed(val medicationId: String, val newName: String) : DailyLogEvent

    /** The user tapped "Delete" in the medication context menu (fetches log count before showing dialog). */
    data class DeleteMedicationClicked(val medication: Medication) : DailyLogEvent

    /** The user confirmed deletion of a medication from the library. */
    data class DeleteMedicationConfirmed(val medicationId: String) : DailyLogEvent

    /** The user dismissed the medication context menu, rename dialog, or delete dialog. */
    data object MedicationEditDismissed : DailyLogEvent
}
