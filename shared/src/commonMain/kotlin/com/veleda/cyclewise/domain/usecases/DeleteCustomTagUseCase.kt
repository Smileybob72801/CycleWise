package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.repository.PeriodRepository

/**
 * Deletes a custom tag from the library and provides a pre-delete log count.
 *
 * The actual log cleanup happens at the database level via CASCADE, but the
 * [getLogCount] method lets the UI show a warning before the user confirms.
 */
class DeleteCustomTagUseCase(private val repository: PeriodRepository) {

    /** Returns the number of daily log entries that reference [tagId]. */
    suspend fun getLogCount(tagId: String): Int =
        repository.getCustomTagLogCount(tagId)

    /** Deletes the custom tag identified by [tagId] from the library. */
    suspend operator fun invoke(tagId: String) {
        repository.deleteCustomTag(tagId)
    }
}
