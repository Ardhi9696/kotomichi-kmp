package com.kotomichi.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kotomichi.model.DailyStats
import com.kotomichi.model.HeatmapData
import com.kotomichi.ui.components.CalendarDayActivity
import com.kotomichi.ui.components.KotomichiCalendar
import com.kotomichi.ui.components.KotomichiCard
import com.kotomichi.ui.components.KotomichiCardVariant
import com.kotomichi.ui.theme.KotomichiSpacing
import java.time.Instant
import java.time.ZoneId

@Composable
fun ProgresTab(
    paddingValues: PaddingValues,
    heatmap: List<HeatmapData>,
    dailyStats: List<DailyStats>,
    isLoading: Boolean
) {
    if (isLoading) {
        LoadingTab(paddingValues)
        return
    }

    val activities = heatmap.mapNotNull { data ->
        val date = Instant.ofEpochMilli(data.date).atZone(ZoneId.systemDefault()).toLocalDate()
        CalendarDayActivity(date = date, intensity = data.count.coerceIn(0, 4))
    }
    val totalLearned = dailyStats.sumOf { it.learnCount }
    val totalReviewed = dailyStats.sumOf { it.reviewCount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = KotomichiSpacing.lg),
        contentPadding = PaddingValues(vertical = KotomichiSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.lg)
    ) {
        item {
            Text(
                text = "Kalender & Statistik",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            KotomichiCard(variant = KotomichiCardVariant.Filled) {
                Column(verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.lg)) {
                    Text(
                        text = "Kalender Aktivitas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (activities.isEmpty()) {
                        Text(
                            text = "Belum ada aktivitas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        KotomichiCalendar(
                            activities = activities,
                            onDayClick = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                        )
                    }
                }
            }
        }

        item {
            KotomichiCard(variant = KotomichiCardVariant.Filled) {
                Column(verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.md)) {
                    Text(
                        text = "Ringkasan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    StatRow(label = "Total dipelajari", value = "$totalLearned")
                    StatRow(label = "Total direview", value = "$totalReviewed")
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.xs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}