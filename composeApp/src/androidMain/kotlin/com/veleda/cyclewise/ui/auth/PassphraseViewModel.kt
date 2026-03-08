package com.veleda.cyclewise.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veleda.cyclewise.session.SessionManager
import com.veleda.cyclewise.settings.AppSettings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** Minimum passphrase length enforced during first-time setup. */
internal const val MIN_PASSPHRASE_LENGTH = 8

/** Maximum passphrase length accepted by the input fields. */
internal const val MAX_PASSPHRASE_LENGTH = 256

/**
 * UI state for the passphrase screen.
 *
 * @property isUnlocking        `true` while an unlock attempt is in progress; used by the UI
 *                              to show a loading indicator and by [reduce] as a re-entrancy
 *                              guard to ignore duplicate tap events.
 * @property isFirstTime        `true` when the user has never completed setup (derived from
 *                              `AppSettings.isPrepopulated`). When `true` the UI shows the
 *                              onboarding setup flow instead of the unlock screen.
 * @property isFirstTimeLoaded  `true` once [isFirstTime] has been resolved from DataStore.
 *                              The UI should not render until this is `true` to avoid a
 *                              flash of the wrong screen.
 * @property passphraseError    Inline error for the passphrase field during setup (e.g. too
 *                              short). `null` means no error.
 * @property confirmationError  Inline error for the confirmation field during setup (e.g.
 *                              mismatch). `null` means no error.
 */
data class PassphraseUiState(
    val isUnlocking: Boolean = false,
    val isFirstTime: Boolean = false,
    val isFirstTimeLoaded: Boolean = false,
    val passphraseError: String? = null,
    val confirmationError: String? = null,
)

/**
 * Handles passphrase unlock and session scope lifecycle.
 *
 * On [PassphraseEvent.UnlockClicked], delegates to [SessionManager.openSession] which:
 * 1. Closes any stale session scope to force full passphrase re-validation.
 * 2. Creates a fresh Koin session scope and resolves the encrypted database.
 * 3. Force-opens the database so SQLCipher consumes the real key.
 * 4. Prepopulates the symptom library on first unlock.
 * 5. Syncs water drafts from [LockedWaterDraft] into the database.
 * 6. Emits [PassphraseEffect.NavigateToTracker] on success.
 *
 * Uses a pure [reduce] function for synchronous state transitions (validation,
 * re-entrancy guard). Side effects (session operations) are launched in [onEvent]
 * only when [reduce] transitions [PassphraseUiState.isUnlocking] from false to true.
 *
 * **Error handling:** On failure, the session scope is closed and [PassphraseEffect.ShowError] is emitted.
 *
 * Singleton-scoped (survives screen rotation).
 *
 * @param appSettings    Used to determine first-time vs returning user.
 * @param sessionManager Centralizes all session scope lifecycle operations.
 */
class PassphraseViewModel(
    private val appSettings: AppSettings,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PassphraseUiState())

    /** Observable UI state consumed by the passphrase Compose screen. */
    val uiState: StateFlow<PassphraseUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<PassphraseEffect>(replay = 0)

    /**
     * One-shot side-effects consumed by the UI (navigation, error toasts).
     *
     * Uses `replay = 0` so effects are not replayed on recomposition or resubscription.
     */
    val effect: SharedFlow<PassphraseEffect> = _effect.asSharedFlow()

    init {
        viewModelScope.launch {
            val prepopulated = appSettings.isPrepopulated.first()
            _uiState.update {
                it.copy(
                    isFirstTime = !prepopulated,
                    isFirstTimeLoaded = true,
                )
            }
        }
    }

    /**
     * Single entry-point for all screen-level user interactions.
     *
     * Applies a pure state update via [reduce], then launches the unlock side effect
     * only when the state transitions to [PassphraseUiState.isUnlocking].
     */
    fun onEvent(event: PassphraseEvent) {
        val wasUnlocking = _uiState.value.isUnlocking
        _uiState.update { reduce(it, event) }

        // Launch the unlock side effect only when reduce transitioned isUnlocking false→true
        if (!wasUnlocking && _uiState.value.isUnlocking) {
            val passphrase = when (event) {
                is PassphraseEvent.UnlockClicked -> event.passphrase
                is PassphraseEvent.SetupClicked -> event.passphrase
            }
            launchUnlock(passphrase)
        }
    }

    /**
     * Pure function that returns the new [PassphraseUiState] for a given event.
     *
     * Contains no side effects — session operations are handled in [onEvent]
     * after the state has been updated.
     *
     * Handles:
     * - **UnlockClicked**: re-entrancy guard — if already unlocking, returns state unchanged.
     *   Otherwise sets `isUnlocking = true`.
     * - **SetupClicked**: validates passphrase length and confirmation match. Sets error
     *   fields on failure, or `isUnlocking = true` on success.
     */
    private fun reduce(state: PassphraseUiState, event: PassphraseEvent): PassphraseUiState {
        return when (event) {
            is PassphraseEvent.UnlockClicked -> {
                if (state.isUnlocking) state
                else state.copy(isUnlocking = true)
            }
            is PassphraseEvent.SetupClicked -> {
                val cleared = state.copy(passphraseError = null, confirmationError = null)
                when {
                    event.passphrase.length < MIN_PASSPHRASE_LENGTH ->
                        cleared.copy(passphraseError = "too_short")
                    event.passphrase != event.confirmation ->
                        cleared.copy(confirmationError = "mismatch")
                    cleared.isUnlocking -> cleared
                    else -> cleared.copy(isUnlocking = true)
                }
            }
        }
    }

    /**
     * Orchestrates passphrase validation and session scope creation via [SessionManager].
     *
     * Delegates the entire scope lifecycle to [SessionManager.openSession], which handles
     * stale scope cleanup, scope creation, key derivation, database force-open,
     * prepopulation, and water draft sync.
     *
     * ## Error handling
     * On any exception the session scope is closed (destroying the database and
     * zeroizing SQLCipher's key copy) and [PassphraseEffect.ShowError] is emitted.
     *
     * @param passphrase the raw user-entered passphrase string.
     */
    private fun launchUnlock(passphrase: String) {
        val isSetup = _uiState.value.isFirstTime
        viewModelScope.launch {
            try {
                sessionManager.openSession(passphrase)
                if (isSetup) {
                    _effect.emit(PassphraseEffect.SetupComplete)
                } else {
                    _effect.emit(PassphraseEffect.NavigateToTracker)
                }
            } catch (e: Exception) {
                Log.e("PassphraseUnlock", "Unlock failed: ${e.message}")
                sessionManager.closeSession()
                _effect.emit(PassphraseEffect.ShowError("Failed to unlock. Wrong passphrase?"))
            } finally {
                _uiState.update { it.copy(isUnlocking = false) }
            }
        }
    }
}
