package com.veleda.cyclewise.domain.providers

import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.repository.PeriodRepository
import kotlinx.coroutines.flow.Flow

/**
 * Session-scoped provider exposing the user's medication library as a reactive stream.
 *
 * Acts as the single source of truth for medication types across all screens.
 * Created when the session scope opens (after passphrase unlock) and destroyed on logout.
 *
 * @property medications Cold [Flow] of the full medication library, sorted by name ascending.
 *                       Emits the current snapshot on subscription and updates on any library change.
 */
class MedicationLibraryProvider(private val periodRepository: PeriodRepository) {
    val medications: Flow<List<Medication>> = periodRepository.getMedicationLibrary()
}