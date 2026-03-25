package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.models.CustomTag
import com.veleda.cyclewise.domain.repository.PeriodRepository

/**
 * Validates and renames a custom tag in the library.
 *
 * Validates that the new name is not blank and does not conflict (case-insensitive)
 * with another tag in [currentLibrary]. The tag being renamed is excluded
 * from the conflict check so that a case-only change is permitted.
 */
class RenameCustomTagUseCase(private val repository: PeriodRepository) {

    /**
     * @param tagId          UUID of the custom tag to rename.
     * @param newName        the desired new name (will be trimmed).
     * @param currentLibrary the current custom tag library for conflict detection.
     * @return [RenameResult] indicating success or the specific validation failure.
     */
    suspend operator fun invoke(
        tagId: String,
        newName: String,
        currentLibrary: List<CustomTag>,
    ): RenameResult {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return RenameResult.BlankName

        val conflict = currentLibrary.any {
            it.id != tagId && it.name.equals(trimmed, ignoreCase = true)
        }
        if (conflict) return RenameResult.NameAlreadyExists

        repository.renameCustomTag(tagId, trimmed)
        return RenameResult.Success
    }
}
