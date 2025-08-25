package com.veleda.cyclewise.domain.providers

import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.repository.CycleRepository
import kotlinx.coroutines.flow.Flow

/**
 * A session-scoped provider that acts as the single source of truth
 * for the user's entire symptom library.
 */
class SymptomLibraryProvider(private val cycleRepository: CycleRepository) {
    val symptoms: Flow<List<Symptom>> = cycleRepository.getSymptomLibrary()
}