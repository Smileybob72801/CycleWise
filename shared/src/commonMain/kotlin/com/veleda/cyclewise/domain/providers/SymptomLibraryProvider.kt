package com.veleda.cyclewise.domain.providers

import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.repository.PeriodRepository
import kotlinx.coroutines.flow.Flow

/**
 * A session-scoped provider that acts as the single source of truth
 * for the user's entire symptom library.
 */
class SymptomLibraryProvider(private val periodRepository: PeriodRepository) {
    val symptoms: Flow<List<Symptom>> = periodRepository.getSymptomLibrary()
}