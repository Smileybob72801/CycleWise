package com.veleda.cyclewise.domain.usecases

/**
 * Outcome of a library item rename operation.
 *
 * Shared by both [RenameSymptomUseCase] and [RenameMedicationUseCase] since the
 * validation logic is identical.
 */
sealed interface RenameResult {
    /** The rename succeeded and the repository has been updated. */
    data object Success : RenameResult

    /** The new name was blank or whitespace-only. */
    data object BlankName : RenameResult

    /** Another item in the library already has this name (case-insensitive). */
    data object NameAlreadyExists : RenameResult
}
