package com.veleda.cyclewise.domain.insights

import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import com.veleda.cyclewise.domain.insights.analysis.CorrelationDirection
import com.veleda.cyclewise.domain.insights.analysis.CorrelationStrength
import com.veleda.cyclewise.domain.insights.analysis.MetricType
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.ExperimentalTime

/**
 * A single piece of generated insight derived from the user's health data.
 *
 * All insight types implement this sealed interface. Insights are deduplicated by [id]
 * and sorted by [priority] descending before being displayed.
 *
 * @property id          Unique identifier for deduplication (e.g., "CYCLE_LENGTH_AVERAGE").
 * @property title       Short heading shown to the user.
 * @property description Full explanatory text, possibly with localized dates and numbers.
 * @property priority    Sort key — higher values appear first. Range: 90 (lowest) to 110 (highest).
 */
sealed interface Insight {
    val id: String
    val title: String
    val description: String
    val priority: Int
    val category: InsightCategory
}

/**
 * An insight about the user's average cycle length.
 *
 * @property averageDays The calculated average length of completed cycles.
 */
data class CycleLengthAverage(
    val averageDays: Double
) : Insight {
    override val id: String = "CYCLE_LENGTH_AVERAGE"
    override val title: String = "Average Cycle Length"
    override val description: String = "Based on completed cycles, your average cycle length is ${averageDays.roundToInt()} days."
    override val priority: Int = 100
    override val category: InsightCategory = InsightCategory.SUMMARY
}

/**
 * An insight that predicts the start date of the next menstrual period.
 *
 * @property predictedDate The calculated raw date for the next period's start.
 * @property daysUntilPrediction The number of days from today until the predicted date.
 * @property formattedDateString The localized date string for display (set by ViewModel).
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
    override val category: InsightCategory = InsightCategory.PREDICTION
}

/**
 * An insight that identifies a trend in cycle length over time.
 *
 * @property trendDescription A user-facing string like "shortened", "lengthened", or "remained consistent".
 * @property changeInDays The number of days the cycle has changed by.
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
    override val category: InsightCategory = InsightCategory.TREND
}

/**
 * An advanced insight that identifies a strong correlation between a symptom
 * and a specific phase of the menstrual cycle.
 *
 * @property symptomName The name of the recurring symptom.
 * @property phaseDescription A user-friendly description of the cycle phase (e.g., "during your period").
 * @property recurrenceRate A string representing the pattern's strength (e.g., "4 out of 5").
 * @property formattedPredictedDateString The localized date string for display (set by ViewModel).
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
    override val category: InsightCategory = InsightCategory.PATTERN
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
 *
 * @property moodType A string like "low" or "high".
 * @property phaseDescription A user-friendly description of the phase (e.g., "on the last day of your cycle").
 * @property recurrenceRate A string representing the pattern's strength (e.g., "4 out of 5").
 */
data class MoodPhasePattern(
    val moodType: String,
    val phaseDescription: String,
    val recurrenceRate: String
) : Insight {
    override val id: String = "MOOD_PATTERN_${moodType}_$phaseDescription"
    override val title: String = "Mood Pattern Detected"
    override val category: InsightCategory = InsightCategory.PATTERN
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
 *
 * @property topSymptoms A list of the names of the most common symptoms.
 */
data class TopSymptomsInsight(
    val topSymptoms: List<String>
) : Insight {
    override val id: String = "TOP_SYMPTOMS_OVERALL"
    override val title: String = "Most Frequent Symptoms"
    override val category: InsightCategory = InsightCategory.SUMMARY
    override val description: String = "Your most commonly logged symptoms are: ${topSymptoms.joinToString(", ")}."
    override val priority: Int = 90
}

/**
 * A pattern linking energy levels to a specific cycle phase.
 *
 * @property energyType "low" or "high".
 * @property phaseDescription Phase description (e.g., "3 days before your period").
 * @property recurrenceRate Strength string (e.g., "4 out of 5").
 */
data class EnergyPhasePattern(
    val energyType: String,
    val phaseDescription: String,
    val recurrenceRate: String,
) : Insight {
    override val id: String = "ENERGY_PATTERN_${energyType}_$phaseDescription"
    override val title: String = "Energy Pattern Detected"
    override val category: InsightCategory = InsightCategory.PATTERN
    override val description: String
        get() {
            val descriptor = if (energyType == "low") "low energy" else "high energy"
            return "You've logged $descriptor $phaseDescription in $recurrenceRate of your recent cycles."
        }
    override val priority: Int = 104
}

/**
 * A pattern linking libido levels to a specific cycle phase.
 *
 * @property libidoType "low" or "high".
 * @property phaseDescription Phase description.
 * @property recurrenceRate Strength string.
 */
data class LibidoPhasePattern(
    val libidoType: String,
    val phaseDescription: String,
    val recurrenceRate: String,
) : Insight {
    override val id: String = "LIBIDO_PATTERN_${libidoType}_$phaseDescription"
    override val title: String = "Libido Pattern Detected"
    override val category: InsightCategory = InsightCategory.PATTERN
    override val description: String
        get() {
            val descriptor = if (libidoType == "low") "low libido" else "high libido"
            return "You've logged $descriptor $phaseDescription in $recurrenceRate of your recent cycles."
        }
    override val priority: Int = 102
}

/**
 * A pattern linking water intake to a specific cycle phase.
 *
 * @property intakeLevel "low" or "high".
 * @property phaseDescription Phase description.
 * @property recurrenceRate Strength string.
 */
data class WaterIntakePhasePattern(
    val intakeLevel: String,
    val phaseDescription: String,
    val recurrenceRate: String,
) : Insight {
    override val id: String = "WATER_PATTERN_${intakeLevel}_$phaseDescription"
    override val title: String = "Water Intake Pattern Detected"
    override val category: InsightCategory = InsightCategory.PATTERN
    override val description: String
        get() {
            val descriptor = if (intakeLevel == "low") "lower water intake" else "higher water intake"
            return "You've had $descriptor $phaseDescription in $recurrenceRate of your recent cycles."
        }
    override val priority: Int = 96
}

/**
 * A pattern in menstrual flow intensity across cycle days.
 *
 * @property intensityType Flow intensity level (e.g., "heavy", "light").
 * @property phaseDescription Phase description.
 * @property recurrenceRate Strength string.
 */
data class FlowPattern(
    val intensityType: String,
    val phaseDescription: String,
    val recurrenceRate: String,
) : Insight {
    override val id: String = "FLOW_PATTERN_${intensityType}_$phaseDescription"
    override val title: String = "Flow Pattern Detected"
    override val category: InsightCategory = InsightCategory.PATTERN
    override val description: String
        get() = "You've had $intensityType flow $phaseDescription in $recurrenceRate of your recent cycles."
    override val priority: Int = 97
}

/**
 * A trend in symptom severity across recent cycles.
 *
 * @property trendDescription "increasing", "decreasing", or "stable".
 * @property avgSeverityRecent Average severity in recent cycles.
 * @property avgSeverityOlder Average severity in older cycles.
 */
data class SymptomSeverityTrend(
    val trendDescription: String,
    val avgSeverityRecent: Double,
    val avgSeverityOlder: Double,
) : Insight {
    override val id: String = "SYMPTOM_SEVERITY_TREND"
    override val title: String = "Symptom Severity Trend"
    override val category: InsightCategory = InsightCategory.TREND
    override val description: String
        get() = "Your average symptom severity has been $trendDescription recently " +
            "(${String.format("%.1f", avgSeverityRecent)} vs ${String.format("%.1f", avgSeverityOlder)} in earlier cycles)."
    override val priority: Int = 93
}

/**
 * A composite summary of the current or most recent cycle.
 *
 * @property cycleDay Current day in cycle (1-based).
 * @property cycleLength Predicted or average cycle length.
 * @property phaseName Current cycle phase name.
 * @property daysUntilNextPeriod Days until predicted next period, or null.
 */
data class CycleSummary(
    val cycleDay: Int,
    val cycleLength: Int,
    val phaseName: String,
    val daysUntilNextPeriod: Int?,
) : Insight {
    override val id: String = "CYCLE_SUMMARY"
    override val title: String = "Cycle Summary"
    override val category: InsightCategory = InsightCategory.SUMMARY
    override val description: String
        get() {
            val base = "You're on day $cycleDay of your $cycleLength-day cycle ($phaseName phase)."
            return if (daysUntilNextPeriod != null && daysUntilNextPeriod > 0) {
                "$base Your next period is expected in about $daysUntilNextPeriod days."
            } else {
                base
            }
        }
    override val priority: Int = 115
}

/**
 * A cross-variable correlation detected via Spearman rank analysis.
 *
 * @property variableA   First metric in the correlation.
 * @property variableB   Second metric in the correlation.
 * @property direction   Positive or negative correlation.
 * @property strength    Qualitative strength (weak, moderate, strong).
 * @property coefficient Raw Spearman coefficient.
 * @property sampleSize  Number of paired observations.
 */
data class CrossVariableCorrelation(
    val variableA: MetricType,
    val variableB: MetricType,
    val direction: CorrelationDirection,
    val strength: CorrelationStrength,
    val coefficient: Double,
    val sampleSize: Int,
) : Insight {
    override val id: String = "CORRELATION_${variableA.name}_${variableB.name}"
    override val title: String = "Correlation Detected"
    override val category: InsightCategory = InsightCategory.CORRELATION
    override val description: String
        get() {
            val dirWord = if (direction == CorrelationDirection.POSITIVE) "positively" else "negatively"
            val strengthWord = strength.name.lowercase()
            val nameA = variableA.name.lowercase().replace('_', ' ')
            val nameB = variableB.name.lowercase().replace('_', ' ')
            return "Your $nameA and $nameB are $strengthWord $dirWord correlated " +
                "(based on $sampleSize observations)."
        }
    override val priority: Int = 98
}