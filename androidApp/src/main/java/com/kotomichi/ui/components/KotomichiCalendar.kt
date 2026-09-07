package com.kotomichi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.kotomichi.ui.theme.KotomichiSpacing
import java.time.DayOfWeek
import java.time.LocalDate

data class CalendarDayActivity(val date: LocalDate, val intensity: Int)

@Composable
fun KotomichiCalendar(
    activities: List<CalendarDayActivity>,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    if (activities.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().padding(KotomichiSpacing.xl),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Belum ada aktivitas",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val maxDate = activities.maxOf { it.date }
    val startDate = maxDate.minusWeeks(17).with(DayOfWeek.SUNDAY)
    val days = generateSequence(startDate) { it.plusDays(1) }
        .takeWhile { !it.isAfter(maxDate) }
        .toList()
    val weeks = days.chunked(7)
    val intensityByDate = activities.associate { it.date to it.intensity.coerceIn(0, 4) }
    val cellShape = MaterialTheme.shapes.extraSmall

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.xs)
    ) {
        weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.xs)
            ) {
                week.forEach { day ->
                    val intensity = intensityByDate[day] ?: 0
                    val alpha = when (intensity) {
                        0 -> 0f
                        1 -> 0.25f
                        2 -> 0.5f
                        3 -> 0.75f
                        else -> 1f
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(cellShape)
                            .background(
                                color = if (intensity == 0) {
                                    MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                                }
                            )
                            .clickable { onDayClick(day) }
                    )
                }
            }
        }
    }
}