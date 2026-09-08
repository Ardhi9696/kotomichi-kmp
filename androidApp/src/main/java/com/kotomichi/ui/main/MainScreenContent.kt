/**
 * File: MainScreenContent.kt
 * Responsibility: Merender konten tab berdasarkan navigasi aktif (Home, Belajar, Profil).
 *                 Menangani routing ke HomeTab, StudyHubTab, atau ProfilTab.
 */
package com.kotomichi.ui.main

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.kotomichi.ui.components.KotomichiDestination
import com.kotomichi.usecase.BelajarMode

@Composable
fun MainScreenContent(
    paddingValues: PaddingValues,
    selected: KotomichiDestination,
    user: com.kotomichi.model.UserProfile?,
    deckCatalog: DeckCatalog,
    dueCount: Int,
    activeMenu: LearnMenu?,
    belajarMode: BelajarMode?,
    showDeckPicker: Boolean,
    selectedDeck: com.kotomichi.model.Deck?,
    setBelajarMode: (BelajarMode?) -> Unit,
    onRefresh: () -> Unit,
    onSelectDeck: (com.kotomichi.model.Deck) -> Unit,
    onDismissDeckPicker: () -> Unit,
    onActivateMenu: (LearnMenu?) -> Unit,
    onRequestMenuExit: () -> Unit,
    onLogout: () -> Unit,
    onUpdateName: suspend (String) -> Unit
) {
    when (selected) {
        KotomichiDestination.Home -> HomeTab(
            paddingValues = paddingValues,
            user = user,
            deckCatalog = deckCatalog,
            dueCount = dueCount,
            onRefresh = onRefresh
        )
        KotomichiDestination.Belajar -> StudyHubTab(
            paddingValues = paddingValues,
            userId = user?.id ?: "",
            decks = deckCatalog.decks,
            deckProgressMap = deckCatalog.deckProgressMap,
            isLoading = deckCatalog.isLoading,
            isRefreshing = deckCatalog.isRefreshing,
            onRefresh = onRefresh,
            selectedDeck = selectedDeck,
            showDeckPicker = showDeckPicker,
            onSelectDeck = onSelectDeck,
            onDismissDeckPicker = onDismissDeckPicker,
            activeMenu = activeMenu,
            onActivateMenu = onActivateMenu,
            belajarMode = belajarMode,
            setBelajarMode = setBelajarMode,
            dueCount = dueCount,
            onRequestMenuExit = onRequestMenuExit
        )
        KotomichiDestination.Profil -> ProfilTab(
            paddingValues = paddingValues,
            userName = user?.name ?: "",
            userEmail = user?.email ?: "",
            isRefreshing = deckCatalog.isRefreshing,
            onRefresh = onRefresh,
            onLogout = onLogout,
            onUpdateName = onUpdateName
        )
    }
}
