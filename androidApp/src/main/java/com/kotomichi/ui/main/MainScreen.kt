/**
 * File: MainScreen.kt
 * Responsibility: Orchestrator utama UI aplikasi. Mengkoordinasikan state, top bar, konten tab,
 *                 bottom navigation, dan dialog. Menggunakan komponen terpisah untuk setiap tanggung jawab.
 */
package com.kotomichi.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.kotomichi.ui.components.KotomichiBottomNavigation
import com.kotomichi.ui.components.KotomichiGlobalLoadingOverlay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    onExitApp: () -> Unit,
    onNavigateToLearnDeck: (Long) -> Unit,
    onNavigateToReview: () -> Unit
) {
    val context = LocalContext.current
    val state = rememberMainScreenState(context, onExitApp)

    // Back handler: jika ada menu aktif, tutup menu; jika tidak, tampilkan dialog keluar
    BackHandler {
        if (state.activeMenu != null) {
            state.setActiveMenu(null)
        } else {
            state.setShowExitDialog(true)
        }
    }

    Scaffold(
        topBar = {
            MainScreenTopBar(
                selected = state.selected,
                activeMenu = state.activeMenu,
                selectedDeck = state.selectedDeck,
                showDeckPicker = state.showDeckPicker,
                user = state.user,
                currentLevel = state.currentLevel,
                totalExp = state.totalExp,
                currentLevelExp = state.currentLevelExp,
                nextLevelExp = state.nextLevelExp,
                expProgress = state.expProgress,
                onDebugTap = { state.setShowDebug(true) },
                onDeckClick = { state.setShowDeckPicker(true) },
                onMenuBack = { state.setActiveMenu(null) }
            )
        },
        bottomBar = {
            if (state.activeMenu == null) {
                KotomichiBottomNavigation(
                    current = state.selected,
                    onNavigate = state.setSelected
                )
            }
        }
    ) { paddingValues ->
        MainScreenContent(
            paddingValues = paddingValues,
            selected = state.selected,
            user = state.user,
            deckCatalog = state.deckCatalog,
            dueCount = state.dueCount,
            activeMenu = state.activeMenu,
            showDeckPicker = state.showDeckPicker,
            selectedDeck = state.selectedDeck,
            onRefresh = { state.scope.launch { state.catalogState.refresh() } },
            onSelectDeck = { deck ->
                state.setSelectedDeckId(deck.id)
                com.kotomichi.ui.theme.DeckPreference.write(context, deck.id)
                state.setShowDeckPicker(false)
            },
            onDismissDeckPicker = { state.setShowDeckPicker(false) },
            onActivateMenu = state.setActiveMenu,
            onLogout = {
                state.setIsLoggingOut(true)
                state.scope.launch {
                    try {
                        com.kotomichi.di.get<com.kotomichi.usecase.AuthUseCase>().logout()
                    } finally {
                        state.setIsLoggingOut(false)
                    }
                }
            },
            onUpdateName = { newName ->
                val currentUser = state.user ?: throw IllegalStateException("Tidak ada sesi aktif")
                com.kotomichi.di.get<com.kotomichi.usecase.AuthUseCase>()
                    .updateProfile(currentUser.copy(displayName = newName))
            }
        )
    }

    MainScreenDialogs(
        isLoggingOut = state.isLoggingOut,
        showExitDialog = state.showExitDialog,
        showDebug = state.showDebug,
        debugInfo = state.debugInfo,
        onExitApp = state.onExitApp,
        onExitDialogDismiss = { state.setShowExitDialog(false) },
        onDebugDismiss = { state.setShowDebug(false) }
    )
}
