package com.veleda.cyclewise.domain.insights

import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * A sealed interface representing a single piece of generated insight
 * derived from the user's health data.
 */
sealed interface Insight {
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
    override val title: String = "Average Cycle Length"
    override val description: String = "Based on your completed cycles, your average length is ${averageDays.roundToInt()} days."
    override val priority: Int = 100
}

/**
 * An insight identifying one of the most frequently logged symptoms.
 * @param symptomName The name of the recurring symptom.
 */
data class SymptomRecurrence(
    val symptomName: String
) : Insight {
    override val title: String = "Frequent Symptom"
    override val description: String = "$symptomName is one of your most frequently logged symptoms."
    override val priority: Int = 90
}