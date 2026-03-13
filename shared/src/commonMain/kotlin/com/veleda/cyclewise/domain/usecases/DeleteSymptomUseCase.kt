package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.repository.PeriodRepository

/**
 * Deletes a symptom from the library and provides a pre-delete log count.
 *
 * The actual log cleanup happens at the database level via CASCADE, but the
 * [getLogCount] method lets the UI show a warning before the user confirms.
 */
class DeleteSymptomUseCase(private val repository: PeriodRepository) {

    /** Returns the number of daily log entries that reference [symptomId]. */
    suspend fun getLogCount(symptomId: String): Int =
        repository.getSymptomLogCount(symptomId)

    /** Deletes the symptom identified by [symptomId] from the library. */
    suspend operator fun invoke(symptomId: String) {
        repository.deleteSymptom(symptomId)
    }
}
