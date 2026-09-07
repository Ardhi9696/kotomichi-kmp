package com.kotomichi.util

import com.kotomichi.di.get
import com.kotomichi.repository.SyncResult
import com.kotomichi.usecase.AuthUseCase
import com.kotomichi.usecase.SyncDataUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Gerbang sinkronisasi terpusat. Semua pipeline (WorkManager, TTL lifecycle, UI)
 * meminta sync lewat coordinator ini sehingga:
 *  - tidak ada dua sync yang berjalan bersamaan (dedup, bukan sekadar serialisasi),
 *  - staleness & cooldown dijaga di satu tempat,
 *  - semua jalur yang sukses menandai last-synced secara konsisten.
 */
object SyncCoordinator {

    /** Cooldown antar-pemicu agar lifecycle onStart→onResume dll. tidak menembak dua kali. */
    const val COOLDOWN_MS = 15_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastMessage = MutableStateFlow<String?>(null)
    val lastMessage: StateFlow<String?> = _lastMessage.asStateFlow()

    private var lastAttemptAt = 0L

    private fun authUseCase(): AuthUseCase = get()
    private fun syncUseCase(): SyncDataUseCase = get()

    /**
     * Entri untuk pemicu ringan (lifecycle/UI). Lewati bila:
     *  - data masih fresh (ttlMs belum terlampaui) dan tidak force,
     *  - masih dalam cooldown,
     *  - ada sinkronisasi lain sedang berjalan (biarkan yang berjalan menyelesaikan).
     */
    fun request(reason: String, ttlMs: Long, full: Boolean, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - SyncTtlManager.lastSyncedAt() < ttlMs) {
            _lastMessage.value = "Sync [$reason] dilewati: data masih fresh"
            Timber.d("Sync [$reason] dilewati: data masih fresh")
            return
        }
        if (!force && now - lastAttemptAt < COOLDOWN_MS) return
        if (_isSyncing.value || syncUseCase().isSyncing.value) {
            _lastMessage.value = "Sync [$reason] dilewati: sync lain sedang berjalan"
            Timber.d("Sync [$reason] dilewati: sync lain sedang berjalan")
            return
        }
        lastAttemptAt = now
        _isSyncing.value = true
        scope.launch { runSync(reason, full) }
    }

    /**
     * Entri untuk WorkManager. Mengembalikan hasil agar worker bisa memutuskan
     * retry. Bila ada sync lain yang sedang berjalan, dilewati (dianggap sukses)
     * karena pekerjaan akan dituntaskan oleh sync tersebut.
     */
    suspend fun workerSync(): SyncResult {
        if (_isSyncing.value || syncUseCase().isSyncing.value) {
            Timber.d("Worker sync dilewati: sync lain sedang berjalan")
            return SyncResult(success = true, message = "Worker dilewati: sync lain sedang berjalan")
        }
        _isSyncing.value = true
        return try {
            if (!authUseCase().isAuthenticated.first()) {
                Timber.d("Worker sync dilewati: belum login")
                SyncResult(success = true, message = "Worker dilewati: belum login")
            } else {
                val result = syncUseCase().performFullSync()
                if (result.success) SyncTtlManager.markSynced()
                result
            }
        } catch (e: Exception) {
            Timber.e(e, "Worker sync error")
            SyncResult(success = false, message = "Worker sync error: ${e.message}")
        } finally {
            _isSyncing.value = false
        }
    }

    fun isStale(ttlMs: Long): Boolean {
        val now = System.currentTimeMillis()
        return now - SyncTtlManager.lastSyncedAt() >= ttlMs
    }

    private suspend fun runSync(reason: String, full: Boolean) {
        try {
            if (!authUseCase().isAuthenticated.first()) {
                _lastMessage.value = "Sync [$reason] dilewati: belum login"
                Timber.d("Sync [$reason] dilewati: belum login")
                return
            }
            val result = if (full) syncUseCase().performFullSync() else syncUseCase().pushUpdates()
            if (result.success) {
                SyncTtlManager.markSynced()
                _lastMessage.value = "Sync [$reason] sukses: ${result.message}"
                Timber.d("Sync [$reason] sukses: ${result.message}")
            } else {
                _lastMessage.value = "Sync [$reason] gagal: ${result.message}"
                Timber.w("Sync [$reason] gagal: ${result.message}")
            }
        } catch (e: Exception) {
            _lastMessage.value = "Sync [$reason] error: ${e.message}"
            Timber.e(e, "Sync [$reason] error")
        } finally {
            _isSyncing.value = false
        }
    }
}