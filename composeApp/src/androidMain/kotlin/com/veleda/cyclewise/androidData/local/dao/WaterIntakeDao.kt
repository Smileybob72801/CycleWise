package com.veleda.cyclewise.androidData.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.veleda.cyclewise.androidData.local.entities.WaterIntakeEntity

@Dao
interface WaterIntakeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(intake: WaterIntakeEntity)

    @Query("SELECT * FROM water_intake WHERE date = :date LIMIT 1")
    suspend fun getForDate(date: String): WaterIntakeEntity?

    @Query("SELECT * FROM water_intake WHERE date IN (:dates)")
    suspend fun getForDates(dates: List<String>): List<WaterIntakeEntity>
}
