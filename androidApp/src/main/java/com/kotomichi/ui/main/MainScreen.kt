package com.kotomichi.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotomichi.di.get
import com.kotomichi.model.UserProfile
import com.kotomichi.ui.components.KotomichiBottomNavigation
import com.kotomichi.ui.components.KotomichiDestination
import com.kotomichi.ui.components.KotomichiDialog
import com.kotomichi.ui.components.KotomichiGlobalLoadingOverlay
import com.kotomichi.usecase.AuthUseCase
import com.kotomichi.usecase.GamificationUseCase
import com.kotomichi.usecase.ReviewCardUseCase
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    onExitApp: () -> Unit,
    onNavigateToLearnDeck: (Long) -> Unit,
    onNavigateToReview: () -> Unit
) {
    val authUseCase: AuthUseCase = get()
    val reviewUseCase: ReviewCardUseCase = get()
    val gamificationUseCase: GamificationUseCase = remember { get() }
    val scope = rememberCoroutineScope()
    val user by authUseCase.currentUser.collectAsStateWithLifecycle(null)
    val dueCount by reviewUseCase.observeDueCount(user?.id ?: "").collectAsStateWithLifecycle(0)

    var selected by rememberSaveable { mutableStateOf(KotomichiDestination.Home) }
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    var isLoggingOut by remember { mutableStateOf(false) }
    var showDebug by remember { mutableStateOf(false) }
    var debugInfo by remember { mutableStateOf("") }
    val catalogState = rememberDeckCatalog(user?.id)
    val deckCatalog = catalogState.catalog

    LaunchedEffect(showDebug) {
        if (showDebug) {
            debugInfo = buildDebugInfo(user, dueCount, deckCatalog)
        }
    }

    val currentLevel = user?.currentLevel ?: 1
    val totalExp = user?.totalExp ?: 0L
    val nextLevelExp = gamificationUseCase.calculateExpForLevel(currentLevel + 1)
    val currentLevelExp = gamificationUseCase.calculateExpForLevel(currentLevel)
    val expProgress = gamificationUseCase.calculateExpProgress(currentLevel, totalExp)

    BackHandler {
        showExitDialog = true
    }

    Scaffold(
        topBar = {
            if (selected == KotomichiDestination.Home) {
                HomeTopBar(
                    userName = user?.name ?: "",
                    level = currentLevel,
                    totalExp = totalExp,
                    currentLevelExp = currentLevelExp,
                    nextLevelExp = nextLevelExp,
                    expProgress = expProgress,
                    onNotificationsClick = {},
                    onDebugTap = { showDebug = true }
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
                onRefresh = { scope.launch { catalogState.refresh() } }
            )
            KotomichiDestination.Belajar -> StudyHubTab(
                paddingValues = paddingValues,
                decks = deckCatalog.decks,
                deckProgressMap = deckCatalog.deckProgressMap,
                isLoading = deckCatalog.isLoading,
                onDeckClick = onNavigateToLearnDeck
            )
            KotomichiDestination.Profil -> ProfilTab(
                paddingValues = paddingValues,
                userName = user?.name ?: "",
                userEmail = user?.email ?: "",
                onLogout = {
                    isLoggingOut = true
                    scope.launch {
                        try {
                            authUseCase.logout()
                        } finally {
                            isLoggingOut = false
                        }
                    }
                }
            )
        }
    }

    if (isLoggingOut) {
        KotomichiGlobalLoadingOverlay(isVisible = true)
    }

    if (showExitDialog) {
        KotomichiDialog(
            title = "Keluar Aplikasi",
            text = "Yakin ingin keluar dari aplikasi?",
            confirmLabel = "Keluar",
            dismissLabel = "Batal",
            isDestructive = true,
            onConfirm = {
                showExitDialog = false
                onExitApp()
            },
            onDismiss = { showExitDialog = false }
        )
    }

    if (showDebug) {
        DebugInfoDialog(info = debugInfo, onDismiss = { showDebug = false })
    }
}