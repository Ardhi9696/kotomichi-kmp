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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.kotomichi.di.get
import com.kotomichi.model.UserProfile
import com.kotomichi.ui.components.KotomichiLoadingSkeleton
import com.kotomichi.ui.dashboard.ActionButtonsRow
import com.kotomichi.ui.dashboard.ActivityCalendarSection
import com.kotomichi.ui.dashboard.DeckListSection
import com.kotomichi.ui.dashboard.ProfileCard
import com.kotomichi.ui.theme.KotomichiDimens
import com.kotomichi.ui.theme.KotomichiSpacing
import com.kotomichi.usecase.GamificationUseCase

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
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Tidak dapat memuat progress dari server: $error",
                            modifier = Modifier.padding(KotomichiSpacing.md),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
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
                ActivityCalendarSection()
            }
        }
    }
}