package com.veleda.cyclewise.ui.auth

import android.net.Uri

/**
 * Defines all user interactions that can occur on the Passphrase screen.
 */
sealed interface PassphraseEvent {
    /** The user has entered their passphrase and tapped the Unlock button. */
    data class UnlockClicked(val passphrase: String) : PassphraseEvent

    /**
     * The first-time user has created a passphrase and tapped the Create button.
     *
     * Semantically distinct from [UnlockClicked] — the ViewModel validates the
     * passphrase length and confirmation match before delegating to the same
     * unlock sequence (key derivation, database creation, session scope,
     * symptom prepopulation, water draft sync, navigation).
     *
     * @property passphrase    the new passphrase entered by the user.
     * @property confirmation  the confirmation re-entry; must match [passphrase].
     */
    data class SetupClicked(
        val passphrase: String,
        val confirmation: String,
    ) : PassphraseEvent

    // ── Backup Import ───────────────────────────────────────────────

    /** User tapped "Import Backup" from the unlock or setup screen. */
    data object ImportBackupClicked : PassphraseEvent

    /** SAF returned the URI of the selected `.rwbackup` file. */
    data class ImportFileSelected(val uri: Uri) : PassphraseEvent

    /** User confirmed the metadata preview dialog. */
    data object ImportMetadataConfirmed : PassphraseEvent

    /** User submitted the passphrase for the backup. */
    data class ImportPassphraseEntered(val passphrase: String) : PassphraseEvent

    /** User confirmed the first overwrite warning dialog. */
    data object ImportFirstWarningConfirmed : PassphraseEvent

    /** User updated the text in the "type OVERWRITE" confirmation field. */
    data class ImportConfirmTextChanged(val text: String) : PassphraseEvent

    /** User typed "OVERWRITE" and confirmed — execute the import. */
    data object ImportSecondConfirmed : PassphraseEvent

    /** User cancelled the import at any step. */
    data object ImportDismissed : PassphraseEvent
}

/**
 * One-time side effects emitted by [PassphraseViewModel].
 *
 * These are consumed once by the UI layer and are not replayed on recomposition.
 */
sealed interface PassphraseEffect {
    /**
     * Emitted after a successful passphrase unlock.
     *
     * Signals the NavHost to navigate away from the passphrase screen to the
     * tracker screen. By the time this is emitted, the session scope is active,
     * the database is open, and all DAOs are available.
     */
    data object NavigateToTracker : PassphraseEffect

    /**
     * Emitted after a successful first-time passphrase setup.
     *
     * The UI shows a success animation before proceeding to navigation.
     * By the time this is emitted, the session scope is active.
     */
    data object SetupComplete : PassphraseEffect

    /**
     * Emitted when unlock fails (wrong passphrase, database corruption, etc.).
     *
     * The session scope has already been closed before this effect is emitted,
     * so no stale database references remain in the DI graph.
     *
     * @property message a user-facing error string suitable for display in a
     *           toast or inline error on the passphrase screen.
     */
    data class ShowError(val message: String) : PassphraseEffect

    /** Launch the SAF file picker for selecting a `.rwbackup` file to import. */
    data object LaunchImportPicker : PassphraseEffect
}