package com.veleda.cyclewise.domain.providers

import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.repository.PeriodRepository
import kotlinx.coroutines.flow.Flow

/**
 * A session-scoped provider that acts as the single source of truth
 * for the user's entire medication library.
 */
class MedicationLibraryProvider(private val periodRepository: PeriodRepository) {
    val medications: Flow<List<Medication>> = periodRepository.getMedicationLibrary()
}