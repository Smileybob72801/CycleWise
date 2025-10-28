package com.veleda.cyclewise.ui.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.time.YearMonth as JavaYearMonth

/**
 * Utility to format a kotlinx.datetime.LocalDate into a user-friendly, locale-aware String.
 */
fun LocalDate.toLocalizedDateString(): String {
    val javaLocalDate = java.time.LocalDate.of(this.year, month.number, day)
    // FormatStyle.LONG typically produces formats like "October 26, 2025" for US locale, respecting user's locale.
    return javaLocalDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
}

/**
 * Utility to format a java.time.YearMonth into a localized month and year string.
 */
fun JavaYearMonth.toLocalizedMonthYearString(): String {
    // Use a custom pattern for Month and Year only, which still uses the default locale.
    return this.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
}