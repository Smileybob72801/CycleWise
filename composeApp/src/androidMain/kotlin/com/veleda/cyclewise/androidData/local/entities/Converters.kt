package com.veleda.cyclewise.androidData.local.entities

import androidx.room.TypeConverter
import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.domain.models.PeriodColor
import com.veleda.cyclewise.domain.models.PeriodConsistency
import com.veleda.cyclewise.domain.models.SymptomCategory
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime

/**
 * Room type converters for non-primitive column types.
 *
 * Serialization formats:
 * - [LocalDate] <-> ISO-8601 string (e.g., "2025-01-15")
 * - [Instant] <-> epoch milliseconds ([Long])
 * - [FlowIntensity], [PeriodColor], [PeriodConsistency], [SymptomCategory] <-> enum name string (e.g., "HEAVY")
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

    @TypeConverter
    fun fromFlowIntensity(value: FlowIntensity?): String? = value?.name

    @TypeConverter
    fun toFlowIntensity(value: String?): FlowIntensity? = value?.let { FlowIntensity.valueOf(it) }

    @TypeConverter
    fun fromPeriodColor(value: PeriodColor?): String? = value?.name

    @TypeConverter
    fun toPeriodColor(value: String?): PeriodColor? = value?.let { PeriodColor.valueOf(it) }

    @TypeConverter
    fun fromPeriodConsistency(value: PeriodConsistency?): String? = value?.name

    @TypeConverter
    fun toPeriodConsistency(value: String?): PeriodConsistency? = value?.let { PeriodConsistency.valueOf(it) }

    @TypeConverter
    fun fromSymptomCategory(value: SymptomCategory?): String? {
        return value?.name
    }

    @TypeConverter
    fun toSymptomCategory(value: String?): SymptomCategory? {
        return value?.let { SymptomCategory.valueOf(it) }
    }
}