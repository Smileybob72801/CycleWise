package com.veleda.cyclewise.ui.tracker

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import java.time.DayOfWeek as JavaDayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Row of abbreviated day-of-week labels used as the calendar grid header.
 *
 * @param daysOfWeek Ordered list of [JavaDayOfWeek] starting from the locale's first day.
 */
@Composable
internal fun DaysOfWeekTitle(daysOfWeek: List<JavaDayOfWeek>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        for (dayOfWeek in daysOfWeek) {
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
