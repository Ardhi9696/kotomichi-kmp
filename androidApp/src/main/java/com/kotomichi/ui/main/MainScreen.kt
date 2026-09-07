package com.kotomichi.ui.main

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotomichi.di.get
import com.kotomichi.model.UserProfile
import com.kotomichi.ui.components.KotomichiBottomNavigation
import com.kotomichi.ui.components.KotomichiDestination
import com.kotomichi.usecase.AuthUseCase
import com.kotomichi.usecase.ReviewCardUseCase
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    onNavigateToLearnDeck: (Long) -> Unit,
    onNavigateToReview: () -> Unit
) {
    val authUseCase: AuthUseCase = get()
    val reviewUseCase: ReviewCardUseCase = get()
    val scope = rememberCoroutineScope()
    val user by authUseCase.currentUser.collectAsStateWithLifecycle(null)
    val dueCount by reviewUseCase.observeDueCount(user?.id ?: "").collectAsStateWithLifecycle(0)

    var selected by remember { mutableStateOf(KotomichiDestination.Home) }
    val deckCatalog = rememberDeckCatalog(user?.id)

    Scaffold(
        topBar = {
            if (selected == KotomichiDestination.Home) {
                HomeTopBar(
                    userName = user?.name ?: "",
                    onNotificationsClick = {}
                )
            }
        },
        bottomBar = {
            KotomichiBottomNavigation(
                current = selected,
                onNavigate = { selected = it }
            )
        }
    ) { paddingValues ->
        when (selected) {
            KotomichiDestination.Home -> HomeTab(
                paddingValues = paddingValues,
                user = user,
                deckCatalog = deckCatalog,
                dueCount = dueCount,
                onNavigateToLearn = onNavigateToLearnDeck,
                onNavigateToReview = onNavigateToReview
            )
            KotomichiDestination.Belajar -> StudyHubTab(
                paddingValues = paddingValues,
                decks = deckCatalog.decks,
                deckProgressMap = deckCatalog.deckProgressMap,
                isLoading = deckCatalog.isLoading,
                onDeckClick = onNavigateToLearnDeck
            )
            KotomichiDestination.Review -> ReviewTab(
                paddingValues = paddingValues,
                dueCount = dueCount,
                isLoading = deckCatalog.isLoading,
                onStartReview = onNavigateToReview
            )
            KotomichiDestination.Progres -> ProgresTab(
                paddingValues = paddingValues,
                heatmap = emptyList(),
                dailyStats = emptyList(),
                isLoading = deckCatalog.isLoading
            )
            KotomichiDestination.Profil -> ProfilTab(
                paddingValues = paddingValues,
                userName = user?.name ?: "",
                userEmail = user?.email ?: "",
                onLogout = {
                    scope.launch { authUseCase.logout() }
                }
            )
        }
    }
}