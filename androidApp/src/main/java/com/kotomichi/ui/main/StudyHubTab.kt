/**
 * File: StudyHubTab.kt
 * Responsibility: Orchestrator untuk tab Belajar. Mengatur routing internal
 *                 antara DeckPickerScreen, HubScreen, MenuDetailScreen, dan
 *                 BelajarQuizScreen berdasarkan state selectedDeck, showDeckPicker,
 *                 dan activeMenu.
 */
package com.kotomichi.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kotomichi.model.Deck
import com.kotomichi.model.DeckProgress
import com.kotomichi.ui.belajar.BelajarQuizScreen
import com.kotomichi.ui.belajar.ReviewQuizScreen
import com.kotomichi.ui.components.KotomichiPullToRefresh
import com.kotomichi.usecase.BelajarMode

/**
 * Tab Belajar - orchestrator utama.
 * @param paddingValues Padding dari parent
 * @param userId ID user aktif
 * @param decks Daftar deck tersedia
 * @param deckProgressMap Progress per deck
 * @param isLoading Status loading
 * @param isRefreshing Status tarik-untuk-muat-ulang (pull to refresh)
 * @param onRefresh Callback saat pull to refresh dipicu
 * @param selectedDeck Deck yang sedang dipilih
 * @param showDeckPicker Apakah picker deck sedang ditampilkan
 * @param onSelectDeck Callback saat deck dipilih
 * @param onDismissDeckPicker Callback saat picker deck ditutup
 * @param activeMenu Menu yang sedang aktif (null jika tidak ada)
 * @param onActivateMenu Callback untuk mengaktifkan/nonaktifkan menu
 * @param belajarMode Mode belajar yang aktif untuk quick action Belajar
 * @param setBelajarMode Callback untuk mengubah mode belajar
 * @param dueCount Jumlah kartu jatuh tempo (semua deck)
 * @param onRequestMenuExit Callback untuk meminta konfirmasi keluar dari menu
 */
@Composable
fun StudyHubTab(
    paddingValues: PaddingValues,
    userId: String,
    decks: List<Deck>,
    deckProgressMap: Map<Long, DeckProgress>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    selectedDeck: Deck?,
    showDeckPicker: Boolean,
    onSelectDeck: (Deck) -> Unit,
    onDismissDeckPicker: () -> Unit,
    activeMenu: LearnMenu?,
    onActivateMenu: (LearnMenu?) -> Unit,
    belajarMode: BelajarMode?,
    setBelajarMode: (BelajarMode?) -> Unit,
    dueCount: Int,
    onRequestMenuExit: () -> Unit
) {
    if (isLoading) {
        LoadingTab(paddingValues)
        return
    }

    // ── Screen: Menu detail / placeholder ──
    activeMenu?.let { menu ->
        if (menu == LearnMenu.CekKemampuan && selectedDeck != null) {
            CekKemampuanScreen(
                paddingValues = paddingValues,
                deck = selectedDeck,
                onBack = onRequestMenuExit
            )
            return
        }
        if (menu == LearnMenu.Belajar && selectedDeck != null) {
            BelajarQuizScreen(
                paddingValues = paddingValues,
                userId = userId,
                deck = selectedDeck,
                mode = belajarMode ?: BelajarMode.NORMAL,
                onExit = onRequestMenuExit
            )
            return
        }
        if (menu == LearnMenu.Review) {
            ReviewQuizScreen(
                paddingValues = paddingValues,
                userId = userId,
                onExit = onRequestMenuExit
            )
            return
        }
        StudyHubPullToRefresh(paddingValues = paddingValues, isRefreshing = isRefreshing, onRefresh = onRefresh) {
            MenuDetailScreen(
                menu = menu,
                onBack = { onActivateMenu(null) }
            )
        }
        return
    }

    // ── Screen: Pilih deck ──
    if (showDeckPicker || selectedDeck == null) {
        StudyHubPullToRefresh(paddingValues = paddingValues, isRefreshing = isRefreshing, onRefresh = onRefresh) {
            DeckPickerScreen(
                paddingValues = PaddingValues(),
                decks = decks,
                deckProgressMap = deckProgressMap,
                onClose = onDismissDeckPicker
            )
        }
        return
    }

    // ── Screen: Hub ──
    requireNotNull(selectedDeck) { "selectedDeck must not be null" }
    StudyHubPullToRefresh(paddingValues = paddingValues, isRefreshing = isRefreshing, onRefresh = onRefresh) {
        HubScreen(
            paddingValues = PaddingValues(),
            deck = selectedDeck,
            dueCount = dueCount,
            onMenuClick = { menu -> onActivateMenu(menu) },
            onBelajarClick = { mode ->
                setBelajarMode(mode)
                onActivateMenu(LearnMenu.Belajar)
            },
            onReviewClick = { onActivateMenu(LearnMenu.Review) }
        )
    }
}

/**
 * Bungkus layar list Belajar dengan pull to refresh yang berada DI BAWAH top bar
 * (bukan dari ujung screen). Padding diterapkan di container luar sehingga indikator
 * refresh berada di bawah top bar; layar di dalam menerima padding kosong agar tidak
 * dobel.
 */
@Composable
private fun StudyHubPullToRefresh(
    paddingValues: PaddingValues,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        KotomichiPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            content = { content() }
        )
    }
}
