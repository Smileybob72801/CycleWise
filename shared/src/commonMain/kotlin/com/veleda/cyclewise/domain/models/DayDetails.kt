package com.veleda.cyclewise.domain.models

/**
 * A UI-agnostic domain model that encapsulates the key data points
 * for a single day, derived from underlying cycles and logs.
 * This is the "truth" that the repository provides to the presentation layer.
 */
data class DayDetails(
    val isPeriodDay: Boolean = false,
    val hasLoggedSymptoms: Boolean = false,
    val hasLoggedMedications: Boolean = false
)