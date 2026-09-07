/**
 * File: StudyHubTab.kt
 * Responsibility: Orchestrator untuk tab Belajar. Mengatur routing internal
 *                 antara DeckPickerScreen, HubScreen, dan MenuDetailScreen
 *                 berdasarkan state selectedDeck, showDeckPicker, dan activeMenu.
 */
package com.kotomichi.ui.main

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.kotomichi.model.Deck
import com.kotomichi.model.DeckProgress

/**
 * Tab Belajar - orchestrator utama.
 * @param paddingValues Padding dari parent
 * @param decks Daftar deck tersedia
 * @param deckProgressMap Progress per deck
 * @param isLoading Status loading
 * @param selectedDeck Deck yang sedang dipilih
 * @param showDeckPicker Apakah picker deck sedang ditampilkan
 * @param onSelectDeck Callback saat deck dipilih
 * @param onDismissDeckPicker Callback saat picker deck ditutup
 * @param activeMenu Menu yang sedang aktif (null jika tidak ada)
 * @param onActivateMenu Callback untuk mengaktifkan/nonaktifkan menu
 */
@Composable
fun StudyHubTab(
    paddingValues: PaddingValues,
    decks: List<Deck>,
    deckProgressMap: Map<Long, DeckProgress>,
    isLoading: Boolean,
    selectedDeck: Deck?,
    showDeckPicker: Boolean,
    onSelectDeck: (Deck) -> Unit,
    onDismissDeckPicker: () -> Unit,
    activeMenu: LearnMenu?,
    onActivateMenu: (LearnMenu?) -> Unit
) {
    if (isLoading) {
        LoadingTab(paddingValues)
        return
    }

    // ── Screen: Menu detail / placeholder ──
    activeMenu?.let { menu ->
        MenuDetailScreen(
            menu = menu,
            onBack = { onActivateMenu(null) }
        )
        return
    }

    // ── Screen: Pilih deck ──
    if (showDeckPicker || selectedDeck == null) {
        DeckPickerScreen(
            paddingValues = paddingValues,
            decks = decks,
            deckProgressMap = deckProgressMap,
            onClose = onDismissDeckPicker
        )
        return
    }

    // ── Screen: Hub ──
    requireNotNull(selectedDeck) { "selectedDeck must not be null" }
    HubScreen(
        paddingValues = paddingValues,
        deck = selectedDeck,
        onMenuClick = { menu -> onActivateMenu(menu) }
    )
}
