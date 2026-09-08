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
import com.kotomichi.model.DailyStats
import com.kotomichi.model.UserProfile
import com.kotomichi.ui.components.CalendarSummary
import com.kotomichi.ui.components.KotomichiCalendar
import com.kotomichi.ui.components.KotomichiCard
import com.kotomichi.ui.components.KotomichiCardVariant
import com.kotomichi.ui.components.KotomichiLoadingSkeleton
import com.kotomichi.ui.theme.KotomichiDimens
import com.kotomichi.ui.theme.KotomichiSpacing
import com.kotomichi.usecase.StatisticsUseCase
import java.time.LocalDate
import java.time.YearMonth
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
    val calendarData = rememberHomeCalendar(
        userId = user?.id,
        refreshTrigger = deckCatalog.refreshTick,
        userStreak = currentStreak
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        PullToRefreshBox(
            isRefreshing = deckCatalog.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
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
                        HomeCalendarSection(
                            dailyStats = calendarData.dailyStats,
                            summary = calendarData.summary,
                            startMonth = calendarData.startMonth
                        )
                    }
                }
            }

            if (deckCatalog.isSyncing && !deckCatalog.isLoading && !deckCatalog.isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                )
            }
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
private fun HomeCalendarSection(
    dailyStats: List<DailyStats>,
    summary: CalendarSummary,
    startMonth: YearMonth
) {
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
            if (dailyStats.isEmpty()) {
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
                KotomichiCalendar(
                    dailyStats = dailyStats,
                    summary = summary,
                    startMonth = startMonth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = KotomichiSpacing.lg)
                )
            }
        }
    }
}

private data class HomeCalendarData(
    val dailyStats: List<DailyStats>,
    val summary: CalendarSummary,
    val startMonth: YearMonth
)

@Composable
private fun rememberHomeCalendar(
    userId: String?,
    refreshTrigger: Int,
    userStreak: Int
): HomeCalendarData {
    val statisticsUseCase: StatisticsUseCase = remember { get() }
    var dailyStats by remember { mutableStateOf<List<DailyStats>>(emptyList()) }

    LaunchedEffect(userId, refreshTrigger) {
        if (userId == null || refreshTrigger <= 0) {
            dailyStats = emptyList()
            return@LaunchedEffect
        }
        runCatching { statisticsUseCase.getDailyStats(userId, 365) }
            .onSuccess { dailyStats = it }
            .onFailure {
                Timber.w(it, "kalender kosong setelah refresh: fetch gagal")
                dailyStats = emptyList()
            }
    }

    val summary = remember(dailyStats, userStreak) {
        val zone = java.time.ZoneId.systemDefault()
        val today = LocalDate.now()
        val todayEndMs = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val thisMonthStart = YearMonth.now().atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val todayMinutes = dailyStats
            .filter { it.date in today.atStartOfDay(zone).toInstant().toEpochMilli() until todayEndMs }
            .sumOf { it.totalTimeMs } / 60_000
        val expToday = dailyStats
            .filter { it.date in today.atStartOfDay(zone).toInstant().toEpochMilli() until todayEndMs }
            .sumOf { it.expEarned }
        val daysThisMonth = dailyStats.count { it.date in thisMonthStart until todayEndMs && it.totalCount > 0 }
        val totalMinutes = dailyStats.sumOf { it.totalTimeMs } / 60_000
        CalendarSummary(
            minutesToday = todayMinutes.toInt(),
            dayStreak = userStreak,
            daysThisMonth = daysThisMonth,
            totalMinutes = totalMinutes.toInt(),
            expToday = expToday.toInt()
        )
    }

    return HomeCalendarData(
        dailyStats = dailyStats,
        summary = summary,
        startMonth = YearMonth.now()
    )
}