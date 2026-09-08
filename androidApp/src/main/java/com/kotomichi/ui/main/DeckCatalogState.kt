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
import com.kotomichi.repository.SyncResult
import com.kotomichi.usecase.DeckProgressUseCase
import com.kotomichi.util.SyncTtlManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber

private const val PERIODIC_SYNC_INTERVAL_MS = 5 * 60 * 1000L

/** Cooldown pull-to-refresh agar refetch beruntun (< 5 detik) tidak membuang API. */
private const val REFRESH_COOLDOWN_MS = 5_000L

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
            // Seluruh sinkronisasi (jaringan + baca/tulis DB lokal) dijalankan di
            // background agar UI tidak membeku. State Compose boleh di-update dari
            // thread mana pun (snapshot system).
            withContext(Dispatchers.IO) {
                // Pipeline lain (WorkManager/TTL) sedang sync → jangan tarik ulang,
                // cukup muat data lokal agar UI tetap responsif.
                if (!force && syncRepository.isSyncing.value) {
                    reloadLocal(null)
                    return@withContext
                }
                if (!force && !SyncTtlManager.isStale(SyncTtlManager.TTL_ON_RESUME_MS)) {
                    reloadLocal(null)
                    return@withContext
                }
                val masterResult = runCatching { syncRepository.pullMasterData() }
                    .getOrElse { SyncResult(success = false, message = "Sync master error: ${it.message}") }
                val userResult = userId?.let {
                    // Kirim dulu progress/review/profil lokal yang belum terkirim, baru tarik
                    // dari server — supaya hasil Belajar/Review yang baru selesai tidak tertimpa
                    // data server yang lebih lama.
                    runCatching { syncRepository.pushUserData() }
                        .getOrElse { SyncResult(success = false, message = "Push user error: ${it.message}") }
                    runCatching { syncRepository.pullUserData() }
                        .getOrElse { SyncResult(success = false, message = "Sync user error: ${it.message}") }
                }
                val success = masterResult.success && (userResult?.success ?: true)
                val error = if (success) null else buildString {
                    if (!masterResult.success) append(masterResult.message)
                    userResult?.takeIf { !it.success }?.let {
                        if (isNotEmpty()) append("; ")
                        append(it.message)
                    }
                }.ifBlank { null }
                if (success) SyncTtlManager.markSynced()
                runCatching { reloadLocal(error) }
                    .onFailure { t ->
                        Timber.e(t, "sync: reloadLocal gagal")
                        catalog = catalog.copy(
                            decks = emptyList(),
                            deckProgressMap = emptyMap(),
                            totalVocabulary = 0,
                            isLoading = false,
                            loadError = "Gagal memuat data lokal: ${t.message}"
                        )
                    }
            }
        } finally {
            catalog = catalog.copy(isSyncing = false)
            isSynchronizing = false
        }
    }

    private var lastRefreshAt = 0L

    suspend fun refresh() {
        val now = System.currentTimeMillis()
        if (now - lastRefreshAt < REFRESH_COOLDOWN_MS) return
        lastRefreshAt = now
        if (catalog.isLoading) {
            sync(showSkeleton = true, force = true)
            return
        }
        catalog = catalog.copy(isRefreshing = true)
        try {
            withContext(Dispatchers.IO) { sync(showSkeleton = false, force = true) }
        } catch (t: Throwable) {
            Timber.e(t, "refresh gagal")
            catalog = catalog.copy(loadError = "Refresh gagal: ${t.message}")
        } finally {
            catalog = catalog.copy(isRefreshing = false)
        }
    }

    fun showEmpty() {
        catalog = DeckCatalog(isLoading = false)
    }

    /** Muat ulang data dari DB lokal tanpa menyentuh jaringan. */
    suspend fun reloadLocalOnly() {
        runCatching { reloadLocal(null) }
            .onFailure { t ->
                Timber.e(t, "reloadLocalOnly gagal")
                catalog = catalog.copy(
                    decks = emptyList(),
                    deckProgressMap = emptyMap(),
                    totalVocabulary = 0,
                    isLoading = false,
                    loadError = "Gagal memuat data lokal: ${t.message}"
                )
            }
    }

    private suspend fun reloadLocal(error: String?) {
        val decks = try {
            deckRepository.getPublishedDecks()
        } catch (t: Throwable) {
            Timber.e(t, "reloadLocal: getPublishedDecks gagal")
            throw t
        }
        val progressMap = try {
            userId?.let {
                deckProgressUseCase.getAllDeckProgress(it).associateBy { it.deckId }
            } ?: emptyMap()
        } catch (t: Throwable) {
            Timber.e(t, "reloadLocal: getAllDeckProgress gagal")
            throw t
        }
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

    val state = remember(userId) { DeckCatalogState(syncRepository, deckRepository, deckProgressUseCase, userId) }

    LaunchedEffect(userId) {
        if (userId == null) {
            state.showEmpty()
            return@LaunchedEffect
        }
        val hasLocalData = deckRepository.getPublishedDecks().isNotEmpty()
        state.sync(showSkeleton = !hasLocalData)
        while (true) {
            delay(PERIODIC_SYNC_INTERVAL_MS)
            // Tarikan data periodik ditangani terpusat (TTL/WorkManager via SyncCoordinator).
            // Di sini cukup muat ulang dari DB lokal agar UI tetap segar tanpa dobel sync.
            state.reloadLocalOnly()
        }
    }

    return state
}