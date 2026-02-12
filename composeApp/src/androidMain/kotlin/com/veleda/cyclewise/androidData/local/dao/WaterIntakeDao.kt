package com.veleda.cyclewise.androidData.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.veleda.cyclewise.androidData.local.entities.WaterIntakeEntity

/**
 * Room DAO for the `water_intake` table.
 *
 * Uses the ISO-8601 date string as the primary key (one row per day, upsert semantics).
 * Date parameters are passed as pre-formatted strings (not [LocalDate]) because
 * the PK column is TEXT.
 */
@Dao
interface WaterIntakeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(intake: WaterIntakeEntity)

    @Query("SELECT * FROM water_intake WHERE date = :date LIMIT 1")
    suspend fun getForDate(date: String): WaterIntakeEntity?

    @Query("SELECT * FROM water_intake WHERE date IN (:dates)")
    suspend fun getForDates(dates: List<String>): List<WaterIntakeEntity>
}
