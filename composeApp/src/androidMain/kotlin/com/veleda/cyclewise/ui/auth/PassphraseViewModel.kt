package com.veleda.cyclewise.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veleda.cyclewise.androidData.local.database.PeriodDatabase
import com.veleda.cyclewise.androidData.local.draft.LockedWaterDraft
import com.veleda.cyclewise.di.SESSION_SCOPE
import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.parameter.parametersOf

data class PassphraseUiState(
    val isUnlocking: Boolean = false
)

/**
 * Handles passphrase unlock and session scope lifecycle.
 *
 * On [PassphraseEvent.UnlockClicked]:
 * 1. Derives the encryption key via [PassphraseService] (on [Dispatchers.IO]).
 * 2. Creates or reuses the Koin session scope and opens the encrypted database.
 * 3. Prepopulates the symptom library on first unlock.
 * 4. Syncs water drafts from [LockedWaterDraft] into the database.
 * 5. Emits [PassphraseEffect.NavigateToTracker] on success.
 *
 * **Re-entrancy guard:** Ignores unlock requests while [PassphraseUiState.isUnlocking] is true.
 * **Error handling:** On failure, the session scope is closed and [PassphraseEffect.ShowError] is emitted.
 *
 * Singleton-scoped (survives screen rotation).
 */
class PassphraseViewModel(
    private val appSettings: AppSettings,
    private val lockedWaterDraft: LockedWaterDraft
) : ViewModel(), KoinComponent {

    private val _uiState = MutableStateFlow(PassphraseUiState())
    val uiState: StateFlow<PassphraseUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<PassphraseEffect>(replay = 0)
    val effect: SharedFlow<PassphraseEffect> = _effect.asSharedFlow()

    fun onEvent(event: PassphraseEvent) {
        when (event) {
            is PassphraseEvent.UnlockClicked -> unlock(event.passphrase)
        }
    }

    private fun unlock(passphrase: String) {
        if (_uiState.value.isUnlocking) return

        viewModelScope.launch {
            _uiState.update { it.copy(isUnlocking = true) }
            try {
                val needsPrepopulation = !appSettings.isPrepopulated.first()

                withContext(Dispatchers.IO) {
                    val koin = getKoin()
                    val sessionScope = koin.getScopeOrNull("session")
                        ?: koin.createScope(
                            scopeId = "session",
                            qualifier = SESSION_SCOPE
                        )

                    val db = sessionScope.get<PeriodDatabase> { parametersOf(passphrase) }
                    db.openHelper.writableDatabase

                    if (needsPrepopulation) {
                        val repository = sessionScope.get<PeriodRepository>()
                        repository.prepopulateSymptomLibrary()
                        appSettings.setPrepopulated(true)
                    }

                    syncWaterDrafts(sessionScope)
                }

                _effect.emit(PassphraseEffect.NavigateToTracker)

            } catch (e: Exception) {
                Log.e("PassphraseUnlock", "Unlock failed with exception", e)
                getKoin().getScopeOrNull("session")?.close()
                _effect.emit(PassphraseEffect.ShowError("Failed to unlock. Wrong passphrase?"))
            } finally {
                _uiState.update { it.copy(isUnlocking = false) }
            }
        }
    }

    private suspend fun syncWaterDrafts(sessionScope: org.koin.core.scope.Scope) {
        val repository = sessionScope.get<PeriodRepository>()
        val syncer = WaterDraftSyncer(lockedWaterDraft, repository)
        syncer.sync()
    }
}