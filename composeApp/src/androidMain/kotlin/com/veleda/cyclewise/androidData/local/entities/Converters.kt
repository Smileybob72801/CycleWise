package com.veleda.cyclewise.androidData.local.entities

import androidx.room.TypeConverter
import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.domain.models.LibidoLevel
import com.veleda.cyclewise.domain.models.SymptomCategory
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

    @TypeConverter
    fun fromFlowIntensity(value: FlowIntensity?): String? {
        // Converts the enum to its String name for storing in the database.
        // E.g., FlowIntensity.HEAVY -> "HEAVY"
        return value?.name
    }

    @TypeConverter
    fun toFlowIntensity(value: String?): FlowIntensity? {
        // Converts a String from the database back into the corresponding enum.
        // E.g., "HEAVY" -> FlowIntensity.HEAVY
        return value?.let { FlowIntensity.valueOf(it) }
    }

    @TypeConverter
    fun fromLibidoLevel(value: LibidoLevel?): String? {
        return value?.name
    }

    @TypeConverter
    fun toLibidoLevel(value: String?): LibidoLevel? {
        return value?.let { LibidoLevel.valueOf(it) }
    }

    @TypeConverter
    fun fromSymptomCategory(value: SymptomCategory?): String? {
        return value?.name
    }

    @TypeConverter
    fun toSymptomCategory(value: String?): SymptomCategory? {
        return value?.let { SymptomCategory.valueOf(it) }
    }
}