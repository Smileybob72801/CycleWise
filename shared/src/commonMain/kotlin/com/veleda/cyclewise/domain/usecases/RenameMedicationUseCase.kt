package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.models.Medication
import com.veleda.cyclewise.domain.repository.PeriodRepository

/**
 * Validates and renames a medication in the library.
 *
 * Validates that the new name is not blank and does not conflict (case-insensitive)
 * with another medication in [currentLibrary]. The medication being renamed is excluded
 * from the conflict check so that a case-only change is permitted.
 */
class RenameMedicationUseCase(private val repository: PeriodRepository) {

    /**
     * @param medicationId   UUID of the medication to rename.
     * @param newName        the desired new name (will be trimmed).
     * @param currentLibrary the current medication library for conflict detection.
     * @return [RenameResult] indicating success or the specific validation failure.
     */
    suspend operator fun invoke(
        medicationId: String,
        newName: String,
        currentLibrary: List<Medication>,
    ): RenameResult {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return RenameResult.BlankName

        val conflict = currentLibrary.any {
            it.id != medicationId && it.name.equals(trimmed, ignoreCase = true)
        }
        if (conflict) return RenameResult.NameAlreadyExists

        repository.renameMedication(medicationId, trimmed)
        return RenameResult.Success
    }
}
