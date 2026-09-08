package com.kotomichi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kotomichi.model.DailyStats
import com.kotomichi.ui.theme.KotomichiSpacing
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Nilai ringkasan yang ditampilkan di header kalender. */
data class CalendarSummary(
    val minutesToday: Int = 0,
    val dayStreak: Int = 0,
    val daysThisMonth: Int = 0,
    val totalMinutes: Int = 0,
    val expToday: Int = 0
)

// Konfigurasi warna intensitas; index 0 = tidak aktif.
private val cellColors = listOf(
    Color(0x22000000),
    Color(0xFF4CAF50).copy(alpha = 0.30f),
    Color(0xFF4CAF50).copy(alpha = 0.50f),
    Color(0xFF4CAF50).copy(alpha = 0.72f),
    Color(0xFF2E7D32)
)

@Composable
fun KotomichiCalendar(
    dailyStats: List<DailyStats>,
    summary: CalendarSummary,
    startMonth: YearMonth,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    var cursor by remember { mutableStateOf(startMonth) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    val activityByDate: Map<LocalDate, DailyStats> = remember(dailyStats) {
        dailyStats.associate { it.date.toLocalDate() to it }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        SummaryRow(summary)

        Spacer(Modifier.height(KotomichiSpacing.lg))

        MonthHeader(
            month = cursor,
            canGoBack = cursor > startMonth,
            onPrev = { cursor = cursor.minusMonths(1) },
            onNext = { cursor = cursor.plusMonths(1) }
        )

        Spacer(Modifier.height(KotomichiSpacing.md))

        WeekdayHeader()

        Spacer(Modifier.height(KotomichiSpacing.sm))

        MonthGrid(
            month = cursor,
            today = today,
            activityByDate = activityByDate,
            selectedDate = selectedDate,
            onDayClick = { selectedDate = it }
        )

        selectedDate?.let { date ->
            Spacer(Modifier.height(KotomichiSpacing.lg))
            DayDetail(date = date, stats = activityByDate[date])
        }

        Spacer(Modifier.height(KotomichiSpacing.md))

        LegendRow()
    }
}

@Composable
private fun SummaryRow(summary: CalendarSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.sm)
    ) {
        SummaryTile("${summary.minutesToday}min", "hari ini", Modifier.weight(1f))
        SummaryTile("${summary.dayStreak}", "streak", Modifier.weight(1f))
        SummaryTile("${summary.daysThisMonth}", "hari bulan ini", Modifier.weight(1f))
        SummaryTile("${summary.expToday}", "EXP", Modifier.weight(1f))
        SummaryTile("${summary.totalMinutes}", "total menit", Modifier.weight(1f))
    }
}

@Composable
private fun SummaryTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.shapes.medium
            )
            .padding(vertical = KotomichiSpacing.md, horizontal = KotomichiSpacing.xs)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    canGoBack: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrev,
            enabled = canGoBack,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = "Bulan sebelumnya"
            )
        }
        Text(
            text = month.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        IconButton(
            onClick = onNext,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Bulan berikutnya"
            )
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        DayOfWeek.entries.forEach { dow ->
            Text(
                text = dow.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    activityByDate: Map<LocalDate, DailyStats>,
    selectedDate: LocalDate?,
    onDayClick: (LocalDate) -> Unit
) {
    val firstDayOfWeek = month.atDay(1).dayOfWeek.value // 1..7 (Senin..Minggu)
    val leadingBlanks = (firstDayOfWeek - 1)
    val daysInMonth = month.lengthOfMonth()
    val totalCells = leadingBlanks + daysInMonth
    val weeks = (0 until totalCells).chunked(7)

    Column(verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.xs)) {
        weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.xs)
            ) {
                (0 until 7).forEach { index ->
                    val dayNumber = week.getOrNull(index)?.let { it - leadingBlanks + 1 }
                    if (dayNumber != null && dayNumber in 1..daysInMonth) {
                        val date = month.atDay(dayNumber)
                        val isToday = date == today
                        val isSelected = date == selectedDate
                        DayCell(
                            dayNumber = dayNumber,
                            intensity = activityByDate[date]?.totalCount ?: 0,
                            isToday = isToday,
                            isSelected = isSelected,
                            onClick = { onDayClick(date) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    dayNumber: Int,
    intensity: Int,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = when {
        isSelected -> MaterialTheme.colorScheme.primary
        intensity > 0 -> cellColors[intensity.coerceIn(1, cellColors.lastIndex)]
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        intensity > 0 -> Color.Black
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayNumber.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = contentColor
        )
    }
}

@Composable
private fun DayDetail(date: LocalDate, stats: DailyStats?) {
    val minutes = (stats?.totalTimeMs ?: 0L) / 60_000
    val reviews = stats?.totalCount ?: 0
    val exp = stats?.expEarned ?: 0L

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .padding(KotomichiSpacing.md),
        verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.xs)
    ) {
        Text(
            text = date.format(
                java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())
            ),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.sm)
        ) {
            DetailStat("$minutes min", "dipelajari", Modifier.weight(1f))
            DetailStat("$reviews review", "ditinjau", Modifier.weight(1f))
            DetailStat(if (exp > 0) "+$exp EXP" else "+0 EXP", "EXP", Modifier.weight(1f))
        }
    }
}

@Composable
private fun DetailStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LegendRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = "Kurang",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        (0..4).forEach { level ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(12.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(
                        if (level == 0) MaterialTheme.colorScheme.surfaceVariant
                        else cellColors[level]
                    )
            )
        }
        Text(
            text = "Banyak",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun Long.toLocalDate(): LocalDate =
    java.time.Instant.ofEpochMilli(this).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
