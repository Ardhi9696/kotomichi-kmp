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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.kotomichi.ui.dashboard.ActionButtonsRow
import com.kotomichi.ui.dashboard.DeckListSection
import com.kotomichi.ui.dashboard.ProfileCard
import com.kotomichi.ui.theme.KotomichiDimens
import com.kotomichi.ui.theme.KotomichiSpacing
import com.kotomichi.usecase.GamificationUseCase
import com.kotomichi.usecase.StatisticsUseCase
import java.time.Instant
import java.time.ZoneId

@Composable
fun HomeTab(
    paddingValues: PaddingValues,
    user: UserProfile?,
    deckCatalog: DeckCatalog,
    dueCount: Int,
    onNavigateToLearn: (Long) -> Unit,
    onNavigateToReview: () -> Unit
) {
    val gamificationUseCase: GamificationUseCase = remember {
        get()
    }
    val currentLevel = user?.currentLevel ?: 1
    val totalExp = user?.totalExp ?: 0L
    val nextLevelExp = gamificationUseCase.calculateExpForLevel(currentLevel + 1)
    val currentLevelExp = gamificationUseCase.calculateExpForLevel(currentLevel)
    val expProgress = gamificationUseCase.calculateExpProgress(currentLevel, totalExp)
    val currentStreak = user?.currentStreak ?: 0
    val heatmap = rememberHomeHeatmap(userId = user?.id, refreshTrigger = !deckCatalog.isLoading)

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
                ProfileCard(
                    user = user ?: UserProfile(id = ""),
                    currentLevel = if (user != null) currentLevel else 1,
                    totalExp = if (user != null) totalExp else 0L,
                    nextLevelExp = nextLevelExp,
                    currentLevelExp = currentLevelExp,
                    expProgress = expProgress,
                    currentStreak = if (user != null) currentStreak else 0
                )
            }

            item {
                HomeStatsRow(
                    level = currentLevel,
                    dayStreak = currentStreak,
                    reviewsDue = dueCount
                )
            }

            item {
                ActionButtonsRow(
                    dueCount = dueCount,
                    onLearnClick = {
                        deckCatalog.decks.firstOrNull()?.let { deck ->
                            onNavigateToLearn(deck.id)
                        }
                    },
                    onReviewClick = onNavigateToReview
                )
            }

            item {
                Text(
                    text = "Lanjutkan Belajar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                DeckListSection(
                    decks = deckCatalog.decks,
                    deckProgressMap = deckCatalog.deckProgressMap,
                    onDeckClick = onNavigateToLearn
                )
            }

            item {
                HomeCalendarSection(heatmap = heatmap)
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
private fun HomeStatsRow(level: Int, dayStreak: Int, reviewsDue: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.sm)
    ) {
        HomeStatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.Psychology,
            iconTint = MaterialTheme.colorScheme.secondary,
            label = "Level",
            value = "$level"
        )
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
                    text = "Kalender Aktivitas",
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
private fun rememberHomeHeatmap(userId: String?, refreshTrigger: Boolean): List<HeatmapData> {
    val statisticsUseCase: StatisticsUseCase = remember { get() }
    var heatmap by remember { mutableStateOf<List<HeatmapData>>(emptyList()) }

    LaunchedEffect(userId, refreshTrigger) {
        if (userId == null || !refreshTrigger) {
            heatmap = emptyList()
            return@LaunchedEffect
        }
        runCatching { statisticsUseCase.getHeatmapData(userId, 365) }
            .onSuccess { heatmap = it }
            .onFailure { heatmap = emptyList() }
    }

    return heatmap
}