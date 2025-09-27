package com.veleda.cyclewise.ui.auth

/**
 * Defines all user interactions that can occur on the Passphrase screen.
 */
sealed interface PassphraseEvent {
    /** The user has entered their passphrase and tapped the Unlock button. */
    data class UnlockClicked(val passphrase: String) : PassphraseEvent
}