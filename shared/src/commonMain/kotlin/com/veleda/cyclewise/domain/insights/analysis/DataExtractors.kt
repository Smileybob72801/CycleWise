package com.veleda.cyclewise.domain.insights.analysis

import com.veleda.cyclewise.domain.models.FullDailyLog

/**
 * Reusable extractor functions for [CycleAnalyzer.tallyPatterns].
 *
 * Each extractor takes a [FullDailyLog] and the current cycle length, and returns
 * a list of `(dataId, normalizedDay)` pairs for tallying. The cycle length is used
 * by [CycleAnalyzer.normalizeDay] to convert luteal-phase days to negative offsets.
 */
object DataExtractors {

    /**
     * Extracts symptom occurrences as `(symptomId, normalizedDay)` pairs.
     */
    val symptoms: (FullDailyLog, Int) -> List<Pair<String, Int>> = { log, cycleLength ->
        log.symptomLogs.map { symptomLog ->
            val normalizedDay = CycleAnalyzer.normalizeDay(log.entry.dayInCycle, cycleLength)
            Pair(symptomLog.symptomId, normalizedDay)
        }
    }

    /**
     * Extracts low mood occurrences (score <= 2) as `("low", normalizedDay)` pairs.
     */
    val moodLow: (FullDailyLog, Int) -> List<Pair<String, Int>> = { log, cycleLength ->
        val mood = log.entry.moodScore
        if (mood != null && mood <= 2) {
            val normalizedDay = CycleAnalyzer.normalizeDay(log.entry.dayInCycle, cycleLength)
            listOf(Pair("low", normalizedDay))
        } else {
            emptyList()
        }
    }

    /**
     * Extracts high mood occurrences (score >= 4) as `("high", normalizedDay)` pairs.
     */
    val moodHigh: (FullDailyLog, Int) -> List<Pair<String, Int>> = { log, cycleLength ->
        val mood = log.entry.moodScore
        if (mood != null && mood >= 4) {
            val normalizedDay = CycleAnalyzer.normalizeDay(log.entry.dayInCycle, cycleLength)
            listOf(Pair("high", normalizedDay))
        } else {
            emptyList()
        }
    }

    /**
     * Extracts low energy occurrences (level <= 2) as `("low", normalizedDay)` pairs.
     */
    val energyLow: (FullDailyLog, Int) -> List<Pair<String, Int>> = { log, cycleLength ->
        val energy = log.entry.energyLevel
        if (energy != null && energy <= 2) {
            val normalizedDay = CycleAnalyzer.normalizeDay(log.entry.dayInCycle, cycleLength)
            listOf(Pair("low", normalizedDay))
        } else {
            emptyList()
        }
    }

    /**
     * Extracts high energy occurrences (level >= 4) as `("high", normalizedDay)` pairs.
     */
    val energyHigh: (FullDailyLog, Int) -> List<Pair<String, Int>> = { log, cycleLength ->
        val energy = log.entry.energyLevel
        if (energy != null && energy >= 4) {
            val normalizedDay = CycleAnalyzer.normalizeDay(log.entry.dayInCycle, cycleLength)
            listOf(Pair("high", normalizedDay))
        } else {
            emptyList()
        }
    }

    /**
     * Extracts low libido occurrences (score <= 2) as `("low", normalizedDay)` pairs.
     */
    val libidoLow: (FullDailyLog, Int) -> List<Pair<String, Int>> = { log, cycleLength ->
        val libido = log.entry.libidoScore
        if (libido != null && libido <= 2) {
            val normalizedDay = CycleAnalyzer.normalizeDay(log.entry.dayInCycle, cycleLength)
            listOf(Pair("low", normalizedDay))
        } else {
            emptyList()
        }
    }

    /**
     * Extracts high libido occurrences (score >= 4) as `("high", normalizedDay)` pairs.
     */
    val libidoHigh: (FullDailyLog, Int) -> List<Pair<String, Int>> = { log, cycleLength ->
        val libido = log.entry.libidoScore
        if (libido != null && libido >= 4) {
            val normalizedDay = CycleAnalyzer.normalizeDay(log.entry.dayInCycle, cycleLength)
            listOf(Pair("high", normalizedDay))
        } else {
            emptyList()
        }
    }

    /**
     * Extracts flow intensity occurrences as `(intensityName, normalizedDay)` pairs.
     */
    val flowIntensity: (FullDailyLog, Int) -> List<Pair<String, Int>> = { log, cycleLength ->
        val intensity = log.periodLog?.flowIntensity
        if (intensity != null) {
            val normalizedDay = CycleAnalyzer.normalizeDay(log.entry.dayInCycle, cycleLength)
            listOf(Pair(intensity.name.lowercase(), normalizedDay))
        } else {
            emptyList()
        }
    }
}
