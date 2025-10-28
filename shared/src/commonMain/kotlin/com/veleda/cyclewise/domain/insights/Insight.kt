package com.veleda.cyclewise.domain.insights

import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.ExperimentalTime

/**
 * A sealed interface representing a single piece of generated insight
 * derived from the user's health data.
 */
sealed interface Insight {
    val id: String
    val title: String
    val description: String
    val priority: Int // Used for sorting; higher numbers are more important.
}

/**
 * An insight about the user's average cycle length.
 * @param averageDays The calculated average length of completed cycles.
 */
data class CycleLengthAverage(
    val averageDays: Double
) : Insight {
    override val id: String = "CYCLE_LENGTH_AVERAGE"
    override val title: String = "Average Cycle Length"
    override val description: String = "Based on completed cycles, your average cycle length is ${averageDays.roundToInt()} days."
    override val priority: Int = 100
}

/**
 * An insight that predicts the start date of the next menstrual period.
 * @param predictedDate The calculated raw date for the next period's start.
 * @param daysUntilPrediction The number of days from today until the predicted date.
 * @param formattedDateString The localized date string for display (set by ViewModel).
 */
data class NextPeriodPrediction @OptIn(ExperimentalTime::class) constructor(
    val predictedDate: LocalDate,
    val daysUntilPrediction: Int,
    val formattedDateString: String = predictedDate.toString() // Default is raw string for non-Android targets
) : Insight {
    override val id: String = "NEXT_PERIOD_PREDICTION"
    override val title: String = "Next Period Forecast"
    override val description: String
        get() {
            val days = daysUntilPrediction
            val dateStr = formattedDateString
            return when {
                days == 0 -> "Your next period is predicted to start today."
                days > 0 -> "Your next period is predicted to start in $days days (on $dateStr)."
                else -> "Your period was expected ${abs(days)} days ago (on $dateStr)."
            }
        }
    override val priority: Int = 110
}

/**
 * An insight that identifies a trend in cycle length over time.
 * @param trendDescription A user-facing string like "shortened", "lengthened", or "remained consistent".
 * @param changeInDays The number of days the cycle has changed by.
 */
data class CycleLengthTrend(
    val trendDescription: String,
    val changeInDays: Int
) : Insight {
    override val id: String = "CYCLE_LENGTH_TREND"
    override val title: String = "Cycle Length Trend"
    override val description: String = if (changeInDays == 0) {
        "Your cycle length has remained consistent over the last few months."
    } else {
        "Your average cycle length has $trendDescription by $changeInDays days over the last 3 months."
    }
    override val priority: Int = 95
}

/**
 * An advanced insight that identifies a strong correlation between a symptom
 * and a specific phase of the menstrual cycle.
 * @param symptomName The name of the recurring symptom.
 * @param phaseDescription A user-friendly description of the cycle phase (e.g., "during your period").
 * @param recurrenceRate A string representing the pattern's strength (e.g., "4 out of 5").
 * @param formattedPredictedDateString The localized date string for display (set by ViewModel).
 */
data class SymptomPhasePattern(
    val symptomName: String,
    val phaseDescription: String,
    val recurrenceRate: String,
    override val priority: Int,
    val predictedDate: LocalDate? = null,
    val chanceDescription: String? = null,
    val formattedPredictedDateString: String? = null
) : Insight {
    override val id: String = "PATTERN_${symptomName}_$phaseDescription"
    override val title: String = "Symptom Pattern Detected"
    override val description: String
    get() {
        val dateStr = formattedPredictedDateString
        val baseDescription = "You've logged '$symptomName' $phaseDescription in $recurrenceRate of your recent cycles."

        return if (dateStr != null && chanceDescription != null) {
            "$baseDescription Based on this, there is a $chanceDescription of it recurring on or around $dateStr."
        } else {
            baseDescription
        }
    }
}

/**
 * An advanced insight that identifies a strong correlation between mood and a
 * specific phase of the menstrual cycle, accounting for variable cycle lengths.
 * @param moodType A string like "low" or "high".
 * @param phaseDescription A user-friendly description of the phase (e.g., "on the last day of your cycle").
 * @param recurrenceRate A string representing the pattern's strength (e.g., "4 out of 5").
 */
data class MoodPhasePattern(
    val moodType: String,
    val phaseDescription: String,
    val recurrenceRate: String
) : Insight {
    override val id: String = "MOOD_PATTERN_${moodType}_$phaseDescription"
    override val title: String = "Mood Pattern Detected"
    override val description: String
    get() {
        val baseDescription = "You've logged a '$moodType' mood $phaseDescription in $recurrenceRate of your recent cycles."
        val suffix = if (moodType == "low") {
            "Being aware of this pattern can help you plan for self-care."
        } else {
            "Knowing this can help you schedule activities you enjoy."
        }
        return "$baseDescription $suffix"
    }
    override val priority: Int = 106
}

/**
 * An insight that lists the user's most frequently logged symptoms overall.
 * @param topSymptoms A list of the names of the most common symptoms.
 */
data class TopSymptomsInsight(
    val topSymptoms: List<String>
) : Insight {
    override val id: String = "TOP_SYMPTOMS_OVERALL"
    override val title: String = "Most Frequent Symptoms"
    override val description: String = "Your most commonly logged symptoms are: ${topSymptoms.joinToString(", ")}."
    override val priority: Int = 90
}