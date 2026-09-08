/**
 * File: MainScreenState.kt
 * Responsibility: Mengelola state UI untuk MainScreen (navigasi, dialog visibility, deck preference, menu aktif).
 *                 Menyediakan helper untuk back handler dan reset state saat pindah tab.
 */
package com.kotomichi.ui.main

import android.content.Context
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
import com.kotomichi.ui.components.KotomichiDestination
import com.kotomichi.ui.theme.DeckPreference
import com.kotomichi.usecase.AuthUseCase
import com.kotomichi.usecase.BelajarMode
import com.kotomichi.usecase.GamificationUseCase
import com.kotomichi.usecase.ReviewCardUseCase

/**
 * State holder untuk MainScreen.
 * @param context Android context untuk preferences
 * @param onExitApp Callback saat user keluar aplikasi
 */
@Composable
fun rememberMainScreenState(
    context: Context,
    onExitApp: () -> Unit
): MainScreenState {
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

    var selectedDeckId by rememberSaveable { mutableStateOf(DeckPreference.read(context)) }
    var showDeckPicker by rememberSaveable { mutableStateOf(false) }
    val selectedDeck = deckCatalog.decks.firstOrNull { it.id == selectedDeckId }

    var activeMenu by rememberSaveable { mutableStateOf<LearnMenu?>(null) }
    var belajarMode by rememberSaveable { mutableStateOf<BelajarMode?>(null) }
    var showMenuExitConfirm by rememberSaveable { mutableStateOf(false) }

    /**
     * Minta konfirmasi keluar dari menu aktif (Cek Kemampuan, Belajar, Review).
     * Progress sesi sudah tersimpan otomatis, jadi hanya menampilkan dialog.
     */
    fun requestMenuExit() {
        if (activeMenu != null) showMenuExitConfirm = true
    }

    /** Konfirmasi keluar: tutup dialog lalu reset menu aktif + mode belajar. */
    fun confirmMenuExit() {
        showMenuExitConfirm = false
        activeMenu = null
        belajarMode = null
    }

    // Reset aktif menu ketika pindah tab lain
    LaunchedEffect(selected) {
        if (selected != KotomichiDestination.Belajar) {
            activeMenu = null
        }
    }

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

    return MainScreenState(
        selected = selected,
        setSelected = { selected = it },
        showExitDialog = showExitDialog,
        setShowExitDialog = { showExitDialog = it },
        isLoggingOut = isLoggingOut,
        setIsLoggingOut = { isLoggingOut = it },
        showDebug = showDebug,
        setShowDebug = { showDebug = it },
        debugInfo = debugInfo,
        setDebugInfo = { debugInfo = it },
        user = user,
        dueCount = dueCount,
        deckCatalog = deckCatalog,
        selectedDeck = selectedDeck,
        selectedDeckId = selectedDeckId,
        setSelectedDeckId = { selectedDeckId = it },
        showDeckPicker = showDeckPicker,
        setShowDeckPicker = { showDeckPicker = it },
        activeMenu = activeMenu,
        setActiveMenu = { newMenu ->
            activeMenu = newMenu
            if (newMenu == null) belajarMode = null
        },
        belajarMode = belajarMode,
        setBelajarMode = { belajarMode = it },
        showMenuExitConfirm = showMenuExitConfirm,
        requestMenuExit = ::requestMenuExit,
        confirmMenuExit = ::confirmMenuExit,
        dismissMenuExitConfirm = { showMenuExitConfirm = false },
        currentLevel = currentLevel,
        totalExp = totalExp,
        currentLevelExp = currentLevelExp,
        nextLevelExp = nextLevelExp,
        expProgress = expProgress,
        scope = scope,
        catalogState = catalogState,
        onExitApp = onExitApp
    )
}

/**
 * Data class yang menampung semua state MainScreen.
 */
data class MainScreenState(
    val selected: KotomichiDestination,
    val setSelected: (KotomichiDestination) -> Unit,
    val showExitDialog: Boolean,
    val setShowExitDialog: (Boolean) -> Unit,
    val isLoggingOut: Boolean,
    val setIsLoggingOut: (Boolean) -> Unit,
    val showDebug: Boolean,
    val setShowDebug: (Boolean) -> Unit,
    val debugInfo: String,
    val setDebugInfo: (String) -> Unit,
    val user: com.kotomichi.model.UserProfile?,
    val dueCount: Int,
    val deckCatalog: DeckCatalog,
    val selectedDeck: com.kotomichi.model.Deck?,
    val selectedDeckId: Long?,
    val setSelectedDeckId: (Long?) -> Unit,
    val showDeckPicker: Boolean,
    val setShowDeckPicker: (Boolean) -> Unit,
    val activeMenu: LearnMenu?,
    val setActiveMenu: (LearnMenu?) -> Unit,
    val belajarMode: BelajarMode?,
    val setBelajarMode: (BelajarMode?) -> Unit,
    val showMenuExitConfirm: Boolean,
    val requestMenuExit: () -> Unit,
    val confirmMenuExit: () -> Unit,
    val dismissMenuExitConfirm: () -> Unit,
    val currentLevel: Int,
    val totalExp: Long,
    val currentLevelExp: Long,
    val nextLevelExp: Long,
    val expProgress: Double,
    val scope: kotlinx.coroutines.CoroutineScope,
    val catalogState: DeckCatalogState,
    val onExitApp: () -> Unit
)
