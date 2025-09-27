package com.veleda.cyclewise.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veleda.cyclewise.androidData.local.database.CycleDatabase
import com.veleda.cyclewise.di.SESSION_SCOPE
import com.veleda.cyclewise.domain.repository.CycleRepository
import com.veleda.cyclewise.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.parameter.parametersOf

data class PassphraseUiState(
    val isUnlocking: Boolean = false,
    val error: String? = null
)

class PassphraseViewModel(
    private val appSettings: AppSettings
) : ViewModel(), KoinComponent {

    private val _uiState = MutableStateFlow(PassphraseUiState())
    val uiState: StateFlow<PassphraseUiState> = _uiState.asStateFlow()

    private val _unlockSuccess = MutableSharedFlow<Unit>(replay = 0)
    val unlockSuccess: SharedFlow<Unit> = _unlockSuccess.asSharedFlow()

    fun onEvent(event: PassphraseEvent) {
        when (event) {
            is PassphraseEvent.UnlockClicked -> unlock(event.passphrase)
        }
    }

    private fun unlock(passphrase: String) {
        if (_uiState.value.isUnlocking) return

        viewModelScope.launch {
            _uiState.update { it.copy(isUnlocking = true, error = null) }
            try {
                val needsPrepopulation = !appSettings.isPrepopulated.first()

                withContext(Dispatchers.IO) {
                    val koin = getKoin()
                    val sessionScope = koin.getScopeOrNull("session")
                        ?: koin.createScope(
                            scopeId = "session",
                            qualifier = SESSION_SCOPE
                        )

                    sessionScope.get<CycleDatabase> { parametersOf(passphrase) }

                    if (needsPrepopulation) {
                        val repository = sessionScope.get<CycleRepository>()
                        repository.prepopulateSymptomLibrary()
                        appSettings.setPrepopulated(true)
                    }
                }

                _unlockSuccess.emit(Unit)

            } catch (e: Exception) {
                android.util.Log.e("PassphraseUnlock", "Unlock failed with exception", e)
                getKoin().getScopeOrNull("session")?.close()
                _uiState.update { it.copy(error = "Failed to unlock. Wrong passphrase?") }
            } finally {
                _uiState.update { it.copy(isUnlocking = false) }
            }
        }
    }
}