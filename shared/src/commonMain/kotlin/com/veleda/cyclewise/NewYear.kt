package com.veleda.cyclewise

import kotlinx.datetime.*
import kotlin.time.ExperimentalTime
import kotlinx.datetime.Month
import kotlin.time.Clock

@OptIn(ExperimentalTime::class)
fun daysUntilJuliesBirthday(): Int {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    val targetYear = if (today < LocalDate(today.year, Month.MARCH, 19))
    {
        today.year
    }
    else
    {
        today.year + 1
    }

    val julieBirthDay = LocalDate(targetYear, Month.MARCH, 19)

    return today.daysUntil(julieBirthDay)
}

fun daysPhrase() : String =
    "There are only ${daysUntilJuliesBirthday()} days left until Julie's birthday!"