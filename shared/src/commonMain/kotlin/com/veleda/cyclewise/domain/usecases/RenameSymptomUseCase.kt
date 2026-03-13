package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.models.Symptom
import com.veleda.cyclewise.domain.repository.PeriodRepository

/**
 * Validates and renames a symptom in the library.
 *
 * Validates that the new name is not blank and does not conflict (case-insensitive)
 * with another symptom in [currentLibrary]. The symptom being renamed is excluded
 * from the conflict check so that a case-only change (e.g. "headache" → "Headache")
 * is permitted.
 */
class RenameSymptomUseCase(private val repository: PeriodRepository) {

    /**
     * @param symptomId      UUID of the symptom to rename.
     * @param newName        the desired new name (will be trimmed).
     * @param currentLibrary the current symptom library for conflict detection.
     * @return [RenameResult] indicating success or the specific validation failure.
     */
    suspend operator fun invoke(
        symptomId: String,
        newName: String,
        currentLibrary: List<Symptom>,
    ): RenameResult {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return RenameResult.BlankName

        val conflict = currentLibrary.any {
            it.id != symptomId && it.name.equals(trimmed, ignoreCase = true)
        }
        if (conflict) return RenameResult.NameAlreadyExists

        repository.renameSymptom(symptomId, trimmed)
        return RenameResult.Success
    }
}
