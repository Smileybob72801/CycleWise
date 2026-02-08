package com.veleda.cyclewise.ui.log

import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.models.Symptom

sealed interface DailyLogEvent {
    data class LogLoaded(val log: FullDailyLog?, val initialSymptoms: List<Symptom>, val initialMedications: List<Medication>) : DailyLogEvent
    data class LibraryUpdated(val symptoms: List<Symptom>, val medications: List<Medication>) : DailyLogEvent
    data class FlowIntensityChanged(val intensity: FlowIntensity?) : DailyLogEvent
    data class MoodScoreChanged(val score: Int) : DailyLogEvent
    data class NoteChanged(val text: String) : DailyLogEvent
    data class TagAdded(val tag: String) : DailyLogEvent
    data class TagRemoved(val tag: String) : DailyLogEvent
    data class SymptomToggled(val symptom: Symptom) : DailyLogEvent
    data class SymptomNameChanged(val name: String) : DailyLogEvent
    data class CreateAndAddSymptom(val name: String) : DailyLogEvent
    data class MedicationToggled(val medication: Medication) : DailyLogEvent
    data class MedicationCreatedAndAdded(val name: String) : DailyLogEvent
    object SaveLog : DailyLogEvent
}

/**
 * One-time side effects emitted by [DailyLogViewModel].
 */
sealed interface DailyLogEffect {
    object NavigateBack : DailyLogEffect
}