package com.veleda.cyclewise.androidData.local.entities

import androidx.room.TypeConverter
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Room type converters for kotlinx-datetime types.
 */
object Converters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate): String = value.toString()

    @TypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)

    @TypeConverter
    fun localDateToEpochDays(date: LocalDate): Long = date.toEpochDays()

    @TypeConverter
    fun epochDaysToLocalDate(days: Long): LocalDate = LocalDate.fromEpochDays(days)

    @OptIn(ExperimentalTime::class)
    @TypeConverter
    fun fromInstant(value: Instant): Long = value.toEpochMilliseconds()

    @OptIn(ExperimentalTime::class)
    @TypeConverter
    fun toInstant(value: Long): Instant = Instant.fromEpochMilliseconds(value)

    @TypeConverter
    fun fromStringList(tags: List<String>): String {
        return Json.encodeToString(tags)
    }

    @TypeConverter
    fun toStringList(tagsJson: String): List<String> {
        return Json.decodeFromString(tagsJson)
    }
}