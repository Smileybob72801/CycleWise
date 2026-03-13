package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.repository.PeriodRepository

/**
 * Deletes a medication from the library and provides a pre-delete log count.
 *
 * The actual log cleanup happens at the database level via CASCADE, but the
 * [getLogCount] method lets the UI show a warning before the user confirms.
 */
class DeleteMedicationUseCase(private val repository: PeriodRepository) {

    /** Returns the number of daily log entries that reference [medicationId]. */
    suspend fun getLogCount(medicationId: String): Int =
        repository.getMedicationLogCount(medicationId)

    /** Deletes the medication identified by [medicationId] from the library. */
    suspend operator fun invoke(medicationId: String) {
        repository.deleteMedication(medicationId)
    }
}
