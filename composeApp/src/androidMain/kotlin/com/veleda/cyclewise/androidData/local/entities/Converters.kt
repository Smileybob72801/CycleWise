package com.veleda.cyclewise.androidData.local.entities

import androidx.room.TypeConverter
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime

/**
 * Room type converters for kotlinx-datetime types.
 */
object Converters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate): String = value.toString()

    @TypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)

    @OptIn(ExperimentalTime::class)
    @TypeConverter
    fun fromInstant(value: Instant): Long = value.toEpochMilliseconds()

    @OptIn(ExperimentalTime::class)
    @TypeConverter
    fun toInstant(value: Long): Instant = Instant.fromEpochMilliseconds(value)
}