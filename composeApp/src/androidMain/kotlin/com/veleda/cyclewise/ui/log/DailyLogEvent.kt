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
    data class LogLoaded(val log: FullDailyLog?, val initialSymptoms: List<Symptom>, val initialMedications: List<Medication>) : DailyLogEvent
    data class LibraryUpdated(val symptoms: List<Symptom>, val medications: List<Medication>) : DailyLogEvent
    data class FlowIntensityChanged(val intensity: FlowIntensity?) : DailyLogEvent
    data class MoodScoreChanged(val score: Int) : DailyLogEvent
    data class EnergyLevelChanged(val level: Int) : DailyLogEvent
    data class LibidoScoreChanged(val score: Int) : DailyLogEvent
    data class PeriodColorChanged(val color: PeriodColor?) : DailyLogEvent
    data class PeriodConsistencyChanged(val consistency: PeriodConsistency?) : DailyLogEvent
    data class NoteChanged(val text: String) : DailyLogEvent
    data class TagAdded(val tag: String) : DailyLogEvent
    data class TagRemoved(val tag: String) : DailyLogEvent
    data class SymptomToggled(val symptom: Symptom) : DailyLogEvent
    data class SymptomNameChanged(val name: String) : DailyLogEvent
    data class CreateAndAddSymptom(val name: String) : DailyLogEvent
    data class MedicationToggled(val medication: Medication) : DailyLogEvent
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

    object WaterIncrement : DailyLogEvent
    object WaterDecrement : DailyLogEvent
}
