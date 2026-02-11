package com.veleda.cyclewise.testutil

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Deterministic constants for test data construction.
 * Using fixed values instead of Clock.System.now() eliminates flakiness
 * caused by non-deterministic timestamps and midnight-rollover issues.
 */
object TestData {
    /** Fixed point in time: 2025-06-15T06:26:40Z */
    val INSTANT: Instant = Instant.fromEpochMilliseconds(1750000000000L)
    val DATE: LocalDate = LocalDate(2025, 6, 15)
    val DATE_YESTERDAY: LocalDate = LocalDate(2025, 6, 14)
    val DATE_TWO_DAYS_AGO: LocalDate = LocalDate(2025, 6, 13)
}
