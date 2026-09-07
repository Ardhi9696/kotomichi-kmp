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
import com.kotomichi.util.SyncTtlManager
import kotlinx.coroutines.delay

private const val PERIODIC_SYNC_INTERVAL_MS = 5 * 60 * 1000L

data class DeckCatalog(
    val decks: List<Deck> = emptyList(),
    val deckProgressMap: Map<Long, DeckProgress> = emptyMap(),
    val totalVocabulary: Int = 0,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSyncing: Boolean = false,
    val loadError: String? = null,
    val refreshTick: Int = 0
)

class DeckCatalogState(
    private val syncRepository: SyncRepository,
    private val deckRepository: DeckRepository,
    private val deckProgressUseCase: DeckProgressUseCase,
    private val userId: String?
) {
    var catalog by mutableStateOf(DeckCatalog())
        private set

    private var isSynchronizing = false

    suspend fun sync(showSkeleton: Boolean, force: Boolean = false) {
        if (isSynchronizing) return
        isSynchronizing = true
        catalog = catalog.copy(isSyncing = !showSkeleton, isLoading = showSkeleton, loadError = null)
        try {
            if (!force && !SyncTtlManager.isStale(SyncTtlManager.TTL_ON_RESUME_MS)) {
                reloadLocal(null)
                return
            }
            val masterResult = syncRepository.pullMasterData()
            val userResult = userId?.let { syncRepository.pullUserData() }
            val success = masterResult.success && (userResult?.success ?: true)
            val error = if (success) null else buildString {
                if (!masterResult.success) append(masterResult.message)
                userResult?.takeIf { !it.success }?.let {
                    if (isNotEmpty()) append("; ")
                    append(it.message)
                }
            }.ifBlank { null }
            if (success) SyncTtlManager.markSynced()
            reloadLocal(error)
        } finally {
            catalog = catalog.copy(isSyncing = false)
            isSynchronizing = false
        }
    }

    suspend fun refresh() {
        if (catalog.isLoading) {
            sync(showSkeleton = true, force = true)
            return
        }
        catalog = catalog.copy(isRefreshing = true)
        sync(showSkeleton = false, force = true)
        catalog = catalog.copy(isRefreshing = false)
    }

    fun showEmpty() {
        catalog = DeckCatalog(isLoading = false)
    }

    private suspend fun reloadLocal(error: String?) {
        val decks = deckRepository.getPublishedDecks()
        val progressMap = userId?.let {
            deckProgressUseCase.getAllDeckProgress(it).associateBy { it.deckId }
        } ?: emptyMap()
        catalog = DeckCatalog(
            decks = decks,
            deckProgressMap = progressMap,
            totalVocabulary = deckRepository.getVocabularyCount(),
            isLoading = false,
            loadError = error,
            refreshTick = catalog.refreshTick + 1
        )
    }
}

@Composable
fun rememberDeckCatalog(userId: String?): DeckCatalogState {
    val syncRepository: SyncRepository = get()
    val deckRepository: DeckRepository = get()
    val deckProgressUseCase: DeckProgressUseCase = get()

    val state = remember { DeckCatalogState(syncRepository, deckRepository, deckProgressUseCase, userId) }

    LaunchedEffect(userId) {
        if (userId == null) {
            state.showEmpty()
            return@LaunchedEffect
        }
        val hasLocalData = deckRepository.getPublishedDecks().isNotEmpty()
        state.sync(showSkeleton = !hasLocalData)
        while (true) {
            delay(PERIODIC_SYNC_INTERVAL_MS)
            state.sync(showSkeleton = false)
        }
    }

    return state
}