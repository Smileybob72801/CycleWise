package com.veleda.cyclewise.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veleda.cyclewise.androidData.local.database.PeriodDatabase
import com.veleda.cyclewise.androidData.local.draft.LockedWaterDraft
import com.veleda.cyclewise.di.SESSION_SCOPE
import com.veleda.cyclewise.domain.models.WaterIntake
import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.core.component.KoinComponent
import org.koin.core.parameter.parametersOf
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class PassphraseUiState(
    val isUnlocking: Boolean = false
)

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

    @OptIn(ExperimentalTime::class)
    private suspend fun syncWaterDrafts(sessionScope: org.koin.core.scope.Scope) {
        val drafts = lockedWaterDraft.readAll()
        if (drafts.isEmpty()) return

        val repository = sessionScope.get<PeriodRepository>()
        val existing = repository.getWaterIntakeForDates(drafts.keys.toList())
            .associateBy { it.date }
        val now = Clock.System.now()
        val syncedDates = mutableSetOf<LocalDate>()

        for ((date, draftCups) in drafts) {
            try {
                val dbEntry = existing[date]
                val dbCups = dbEntry?.cups ?: 0

                if (draftCups > dbCups) {
                    repository.upsertWaterIntake(
                        WaterIntake(
                            date = date,
                            cups = draftCups,
                            createdAt = dbEntry?.createdAt ?: now,
                            updatedAt = now
                        )
                    )
                }
                syncedDates += date
            } catch (e: Exception) {
                Log.e("WaterSync", "Failed to sync water for $date", e)
            }
        }

        if (syncedDates.isNotEmpty()) {
            lockedWaterDraft.clearDates(syncedDates)
        }
    }
}