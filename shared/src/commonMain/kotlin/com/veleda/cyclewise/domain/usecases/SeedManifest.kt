package com.veleda.cyclewise.domain.usecases

/**
 * Tracks the exact record IDs created by [TutorialSeederUseCase] so they can be
 * deleted precisely after the tutorial ends.
 *
 * Serialised to JSON and stored in `AppSettings` (~2 KB). The caller (Android layer)
 * handles JSON conversion since `org.json` is Android-only; this class lives in the
 * shared KMP module as a plain data class.
 *
 * @property periodUuids     UUIDs of the seeded [Period] records.
 * @property dailyEntryIds   IDs of the seeded [DailyEntry] records.
 * @property waterIntakeDates ISO-8601 date strings of the seeded [WaterIntake] records.
 */
data class SeedManifest(
    val periodUuids: List<String>,
    val dailyEntryIds: List<String>,
    val waterIntakeDates: List<String>,
)
