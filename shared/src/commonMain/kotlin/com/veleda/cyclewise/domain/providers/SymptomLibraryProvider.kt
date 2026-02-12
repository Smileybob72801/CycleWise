package com.veleda.cyclewise.domain.providers

import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.repository.PeriodRepository
import kotlinx.coroutines.flow.Flow

/**
 * Session-scoped provider exposing the user's symptom library as a reactive stream.
 *
 * Acts as the single source of truth for symptom types across all screens.
 * Created when the session scope opens (after passphrase unlock) and destroyed on logout.
 *
 * @property symptoms Cold [Flow] of the full symptom library, sorted by name ascending.
 *                    Emits the current snapshot on subscription and updates on any library change.
 */
class SymptomLibraryProvider(private val periodRepository: PeriodRepository) {
    val symptoms: Flow<List<Symptom>> = periodRepository.getSymptomLibrary()
}