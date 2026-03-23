package com.veleda.cyclewise.androidData.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.veleda.cyclewise.androidData.local.entities.CustomTagEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the `custom_tag_library` table.
 *
 * Manages the user's unique custom tag types. Names are unique (IGNORE on conflict).
 * Results from [getAllCustomTags] are sorted by name ascending.
 */
@Dao
interface CustomTagDao {
    @Query("SELECT * FROM custom_tag_library ORDER BY name ASC")
    fun getAllCustomTags(): Flow<List<CustomTagEntity>>

    @Query("SELECT * FROM custom_tag_library WHERE name = :name LIMIT 1")
    suspend fun getCustomTagByName(name: String): CustomTagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(customTag: CustomTagEntity)

    /** Renames a custom tag in the library by its UUID. */
    @Query("UPDATE custom_tag_library SET name = :newName WHERE id = :id")
    suspend fun updateName(id: String, newName: String)

    /** Deletes a custom tag from the library by its UUID. CASCADE removes associated logs. */
    @Query("DELETE FROM custom_tag_library WHERE id = :id")
    suspend fun deleteById(id: String)

    /** Returns the number of custom tag log entries that reference the given [tagId]. */
    @Query("SELECT COUNT(*) FROM custom_tag_logs WHERE tag_id = :tagId")
    suspend fun countLogsForTag(tagId: String): Int
}
