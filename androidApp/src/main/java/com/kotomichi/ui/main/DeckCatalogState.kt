package com.kotomichi.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kotomichi.di.get
import com.kotomichi.model.Deck
import com.kotomichi.model.DeckProgress
import com.kotomichi.repository.DeckRepository
import com.kotomichi.repository.SyncRepository
import com.kotomichi.usecase.DeckProgressUseCase

data class DeckCatalog(
    val decks: List<Deck> = emptyList(),
    val deckProgressMap: Map<Long, DeckProgress> = emptyMap(),
    val isLoading: Boolean = true,
    val loadError: String? = null
)

@Composable
fun rememberDeckCatalog(userId: String?): DeckCatalog {
    val syncRepository: SyncRepository = get()
    val deckRepository: DeckRepository = get()
    val deckProgressUseCase: DeckProgressUseCase = get()

    var catalog by remember { mutableStateOf(DeckCatalog()) }

    LaunchedEffect(userId) {
        if (userId == null) {
            catalog = DeckCatalog(isLoading = false)
            return@LaunchedEffect
        }
        catalog = catalog.copy(isLoading = true, loadError = null)
        runCatching { syncRepository.pullMasterData() }
        val progressResult = runCatching { syncRepository.pullUserData() }
        val decks = deckRepository.getPublishedDecks()
        val deckProgressMap = deckProgressUseCase.getAllDeckProgress(userId).associateBy { it.deckId }
        catalog = DeckCatalog(
            decks = decks,
            deckProgressMap = deckProgressMap,
            isLoading = false,
            loadError = progressResult.exceptionOrNull()?.message
        )
    }

    return catalog
}