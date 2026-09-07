package com.kotomichi.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotomichi.app.R
import com.kotomichi.di.get
import com.kotomichi.model.Deck
import com.kotomichi.model.DeckProgress
import com.kotomichi.model.UserProfile
import com.kotomichi.repository.DeckRepository
import com.kotomichi.repository.SyncRepository
import com.kotomichi.ui.components.KotomichiLoadingSkeleton
import com.kotomichi.ui.components.KotomichiTopBar
import com.kotomichi.ui.components.KotomichiTopBarVariant
import com.kotomichi.ui.theme.KotomichiDimens
import com.kotomichi.ui.theme.KotomichiSpacing
import com.kotomichi.usecase.AuthUseCase
import com.kotomichi.usecase.DeckProgressUseCase
import com.kotomichi.usecase.GamificationUseCase
import com.kotomichi.usecase.ReviewCardUseCase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToLearn: (Long) -> Unit,
    onNavigateToReview: () -> Unit,
    onLogout: () -> Unit
) {
    val authUseCase: AuthUseCase = get()
    val deckProgressUseCase: DeckProgressUseCase = get()
    val gamificationUseCase: GamificationUseCase = get()
    val reviewUseCase: ReviewCardUseCase = get()
    val syncRepository: SyncRepository = get()
    val deckRepository: DeckRepository = get()

    val user by authUseCase.currentUser.collectAsStateWithLifecycle(null)
    var decks by remember { mutableStateOf<List<Deck>>(emptyList()) }
    var deckProgressMap by remember { mutableStateOf<Map<Long, DeckProgress>>(emptyMap()) }
    val dueCount by reviewUseCase.observeDueCount(user?.id ?: "").collectAsStateWithLifecycle(0)
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val userId = user?.id
    LaunchedEffect(userId) {
        if (userId == null) {
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        loadError = null
        runCatching { syncRepository.pullMasterData() }
        val progressResult = runCatching { syncRepository.pullUserData() }
        progressResult.exceptionOrNull()?.let { loadError = it.message }
        decks = deckRepository.getPublishedDecks()
        deckProgressMap = deckProgressUseCase.getAllDeckProgress(userId).associateBy { it.deckId }
        isLoading = false
    }

    val currentLevel = user?.currentLevel ?: 1
    val totalExp = user?.totalExp ?: 0L
    val nextLevelExp = gamificationUseCase.calculateExpForLevel(currentLevel + 1)
    val currentLevelExp = gamificationUseCase.calculateExpForLevel(currentLevel)
    val expProgress = gamificationUseCase.calculateExpProgress(currentLevel, totalExp)
    val currentStreak = user?.currentStreak ?: 0

    Scaffold(
        topBar = {
            KotomichiTopBar(
                title = stringResource(R.string.app_name),
                variant = KotomichiTopBarVariant.Medium,
                actions = {
                    IconButton(onClick = { /* Open profile */ }) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Profil"
                        )
                    }
                    IconButton(onClick = { /* Open settings */ }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Pengaturan"
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Keluar"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(KotomichiSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.md)
            ) {
                repeat(3) {
                    KotomichiLoadingSkeleton(modifier = Modifier.fillMaxWidth().height(KotomichiDimens.skeletonPlaceholderHeight))
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(KotomichiSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.lg)
            ) {
                if (loadError != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Tidak dapat memuat progress dari server: $loadError",
                            modifier = Modifier.padding(KotomichiSpacing.md),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                ProfileCard(
                    user = user ?: UserProfile(id = ""),
                    currentLevel = if (user != null) currentLevel else 1,
                    totalExp = if (user != null) totalExp else 0L,
                    nextLevelExp = nextLevelExp,
                    currentLevelExp = currentLevelExp,
                    expProgress = expProgress,
                    currentStreak = if (user != null) currentStreak else 0
                )

                ActionButtonsRow(
                    dueCount = dueCount,
                    onLearnClick = {
                        if (decks.isNotEmpty()) {
                            onNavigateToLearn(decks.first().id)
                        }
                    },
                    onReviewClick = onNavigateToReview
                )

                DeckListSection(
                    decks = decks,
                    deckProgressMap = deckProgressMap,
                    onDeckClick = onNavigateToLearn
                )

                ActivityCalendarSection()
            }
        }
    }
}