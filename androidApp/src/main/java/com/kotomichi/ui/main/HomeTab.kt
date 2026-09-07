package com.kotomichi.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.kotomichi.di.get
import com.kotomichi.model.HeatmapData
import com.kotomichi.model.UserProfile
import com.kotomichi.ui.components.CalendarDayActivity
import com.kotomichi.ui.components.KotomichiCalendar
import com.kotomichi.ui.components.KotomichiCard
import com.kotomichi.ui.components.KotomichiCardVariant
import com.kotomichi.ui.components.KotomichiLoadingSkeleton
import com.kotomichi.ui.theme.KotomichiDimens
import com.kotomichi.ui.theme.KotomichiSpacing
import com.kotomichi.usecase.StatisticsUseCase
import java.time.Instant
import java.time.ZoneId
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTab(
    paddingValues: PaddingValues,
    user: UserProfile?,
    deckCatalog: DeckCatalog,
    dueCount: Int,
    onRefresh: () -> Unit
) {
    val currentStreak = user?.currentStreak ?: 0
    val masteredVocab = deckCatalog.deckProgressMap.values.sumOf { it.masteredVocab }
    val heatmap = rememberHomeHeatmap(userId = user?.id, refreshTrigger = deckCatalog.refreshTick)

    PullToRefreshBox(
        isRefreshing = deckCatalog.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = KotomichiSpacing.lg),
                contentPadding = PaddingValues(vertical = KotomichiSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.lg)
            ) {
                if (deckCatalog.isLoading) {
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.lg)
                        ) {
                            repeat(3) {
                                KotomichiLoadingSkeleton(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(KotomichiDimens.skeletonPlaceholderHeight)
                                )
                            }
                        }
                    }
                } else {
                    deckCatalog.loadError?.let { error ->
                        item {
                            ErrorBanner(error)
                        }
                    }

                    item {
                        Text(
                            text = "Overview",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        HomeStatsRow(
                            dayStreak = currentStreak,
                            reviewsDue = dueCount,
                            masteredVocab = masteredVocab
                        )
                    }

                    item {
                        MateriInfoCard(
                            totalDeck = deckCatalog.decks.size,
                            totalVocabulary = deckCatalog.totalVocabulary
                        )
                    }

                    item {
                        HomeCalendarSection(heatmap = heatmap)
                    }
                }
            }

            if (deckCatalog.isSyncing && !deckCatalog.isLoading && !deckCatalog.isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(top = paddingValues.calculateTopPadding())
                )
            }
        }
    }
}

@Composable
private fun ErrorBanner(error: String) {
    KotomichiCard(
        variant = KotomichiCardVariant.Filled,
        contentPadding = PaddingValues(KotomichiSpacing.md)
    ) {
        Text(
            text = "Tidak dapat memuat progress dari server: $error",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun HomeStatsRow(dayStreak: Int, reviewsDue: Int, masteredVocab: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.sm)
    ) {
        HomeStatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.LocalFireDepartment,
            iconTint = MaterialTheme.colorScheme.primary,
            label = "Streak",
            value = "$dayStreak hari"
        )
        HomeStatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.RateReview,
            iconTint = MaterialTheme.colorScheme.tertiary,
            label = "Review Due",
            value = "$reviewsDue"
        )
        HomeStatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.Psychology,
            iconTint = MaterialTheme.colorScheme.secondary,
            label = "Kuasai",
            value = "$masteredVocab"
        )
    }
}

@Composable
private fun HomeStatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    label: String,
    value: String
) {
    KotomichiCard(
        variant = KotomichiCardVariant.Filled,
        contentPadding = PaddingValues(KotomichiSpacing.md),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.xs)
        ) {
            Box(
                modifier = Modifier
                    .size(KotomichiDimens.statIconSize + KotomichiSpacing.sm * 2)
                    .background(
                        color = iconTint.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(KotomichiDimens.statIconSize)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
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
}

@Composable
private fun MateriInfoCard(totalDeck: Int, totalVocabulary: Int) {
    KotomichiCard(
        variant = KotomichiCardVariant.Filled,
        contentPadding = PaddingValues(KotomichiSpacing.lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.sm)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Materi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            InfoStatRow(
                icon = Icons.Rounded.Translate,
                iconTint = MaterialTheme.colorScheme.tertiary,
                label = "Total Vocabulary",
                value = "$totalVocabulary kata"
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            InfoStatRow(
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                iconTint = MaterialTheme.colorScheme.secondary,
                label = "Total Deck",
                value = "$totalDeck bab"
            )
        }
    }
}

@Composable
private fun InfoStatRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(KotomichiDimens.statIconSize + KotomichiSpacing.sm * 2)
                .background(
                    color = iconTint.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(KotomichiSpacing.sm)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(KotomichiDimens.statIconSize)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(KotomichiSpacing.sm))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HomeCalendarSection(heatmap: List<HeatmapData>) {
    KotomichiCard(variant = KotomichiCardVariant.Filled) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Aktivitas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            if (heatmap.isEmpty()) {
                Text(
                    text = "Belum ada aktivitas belajar. Mulai belajar untuk mengisi kalender.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = KotomichiSpacing.xl2)
                )
            } else {
                val activities = heatmap.map { data ->
                    CalendarDayActivity(
                        date = Instant.ofEpochMilli(data.date).atZone(ZoneId.systemDefault()).toLocalDate(),
                        intensity = data.count.coerceIn(0, 4)
                    )
                }
                KotomichiCalendar(
                    activities = activities,
                    onDayClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = KotomichiSpacing.lg)
                )
            }
        }
    }
}

@Composable
private fun rememberHomeHeatmap(userId: String?, refreshTrigger: Int): List<HeatmapData> {
    val statisticsUseCase: StatisticsUseCase = remember { get() }
    var heatmap by remember { mutableStateOf<List<HeatmapData>>(emptyList()) }

    LaunchedEffect(userId, refreshTrigger) {
        if (userId == null || refreshTrigger <= 0) {
            heatmap = emptyList()
            return@LaunchedEffect
        }
        runCatching { statisticsUseCase.getHeatmapData(userId, 365) }
            .onSuccess { heatmap = it }
            .onFailure {
                Timber.w(it, "heatmap kosong setelah refresh: fetch gagal")
                heatmap = emptyList()
            }
    }

    return heatmap
}