package com.veleda.cyclewise.domain.providers

import com.veleda.cyclewise.domain.models.CustomTag
import com.veleda.cyclewise.domain.repository.PeriodRepository
import kotlinx.coroutines.flow.Flow

/**
 * Session-scoped provider exposing the user's custom tag library as a reactive stream.
 *
 * Acts as the single source of truth for custom tag types across all screens.
 * Created when the session scope opens (after passphrase unlock) and destroyed on logout.
 *
 * @property customTags Cold [Flow] of the full custom tag library, sorted by name ascending.
 *                      Emits the current snapshot on subscription and updates on any library change.
 */
class CustomTagLibraryProvider(private val periodRepository: PeriodRepository) {
    val customTags: Flow<List<CustomTag>> = periodRepository.getCustomTagLibrary()
}
