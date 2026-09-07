/**
 * File: MainScreenTopBar.kt
 * Responsibility: Merender top bar berdasarkan tab aktif dan state (menu aktif, deck terpilih).
 *                 Menangani routing ke HomeTopBar, LearnTopBar, atau LearnMenuTopBar.
 */
package com.kotomichi.ui.main

import androidx.compose.runtime.Composable
import com.kotomichi.ui.components.KotomichiDestination

@Composable
fun MainScreenTopBar(
    selected: KotomichiDestination,
    activeMenu: LearnMenu?,
    selectedDeck: com.kotomichi.model.Deck?,
    showDeckPicker: Boolean,
    user: com.kotomichi.model.UserProfile?,
    currentLevel: Int,
    totalExp: Long,
    currentLevelExp: Long,
    nextLevelExp: Long,
    expProgress: Double,
    onDebugTap: () -> Unit,
    onDeckClick: () -> Unit,
    onMenuBack: () -> Unit
) {
    when (selected) {
        KotomichiDestination.Home -> HomeTopBar(
            userName = user?.name ?: "",
            level = currentLevel,
            totalExp = totalExp,
            currentLevelExp = currentLevelExp,
            nextLevelExp = nextLevelExp,
            expProgress = expProgress,
            onNotificationsClick = {},
            onDebugTap = onDebugTap
        )
        KotomichiDestination.Belajar -> {
            when {
                activeMenu != null -> LearnMenuTopBar(menu = activeMenu, onBack = onMenuBack)
                selectedDeck != null && !showDeckPicker -> LearnTopBar(
                    deckTitle = selectedDeck.title,
                    onDeckClick = onDeckClick
                )
            }
        }
        KotomichiDestination.Profil -> {} // No top bar for profile
    }
}
