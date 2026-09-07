/**
 * File: SyncRepositoryImpl.kt
 * Responsibility: Orchestrator utama untuk sinkronisasi. Mengelola state global (isSyncing, syncStatus,
 *                 lastSyncTime, lastSyncDiagnostics), mutex untuk serialisasi, dan routing
 *                 ke komponen pull/push master data dan user data.
 */
package com.kotomichi.repository

import com.kotomichi.db.KotomichiDatabase
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementasi utama SyncRepository.
 * @param database Database untuk query lokal
 * @param httpClient HTTP client untuk request ke server
 * @param baseUrl Base URL API server
 * @param authRepository Repository untuk autentikasi
 */
class SyncRepositoryImpl(
    private val database: KotomichiDatabase,
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val authRepository: AuthRepository
) : SyncRepository {

    // ── State Management ─────────────────────────────────────────────────────
    private val _isSyncing = MutableStateFlow(false)
    override val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    private val _syncStatusFlow: Flow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(0L)
    private val _lastSyncTimeFlow: Flow<Long> = _lastSyncTime.asStateFlow()

    private val _lastSyncDiagnostics = MutableStateFlow("")
    override val lastSyncDiagnostics: StateFlow<String> = _lastSyncDiagnostics.asStateFlow()

    override fun observeSyncStatus(): Flow<SyncStatus> = _syncStatusFlow
    override fun observeLastSyncTime(): Flow<Long> = _lastSyncTimeFlow

    // ── Mutex untuk Serialisasi ──────────────────────────────────────────────
    private val syncMutex = Mutex()

    private suspend fun <T> exclusive(block: suspend () -> T): T {
        _isSyncing.value = true
        try {
            return syncMutex.withLock { block() }
        } finally {
            _isSyncing.value = false
        }
    }

    // ── Komponen Sinkronisasi ────────────────────────────────────────────────
    private val masterDataPull = SyncMasterDataPull(database, httpClient, baseUrl)
    private val userDataPull = SyncUserDataPull(database, httpClient, baseUrl, authRepository)
    private val userDataPush = SyncUserDataPush(database, httpClient, baseUrl, authRepository)
    private val diagnosticsHelper = SyncDiagnosticsHelper(database.appConfigQueries)

    // ── Public API ───────────────────────────────────────────────────────────
    override suspend fun pullMasterData(): SyncResult = exclusive {
        val since = diagnosticsHelper.readMasterWatermark()
        val result = masterDataPull.pullAll(since)

        val serverTimestamp = System.currentTimeMillis()
        _lastSyncTime.value = serverTimestamp
        if (result.success) {
            database.appConfigQueries.upsert(MASTER_WATERMARK_KEY, serverTimestamp.toString(), null, null, serverTimestamp)
            database.appConfigQueries.upsert("last_sync", serverTimestamp.toString(), null, null, serverTimestamp)
        }
        _syncStatus.value = if (result.success) SyncStatus.SUCCESS else SyncStatus.FAILED
        _lastSyncDiagnostics.value = result.message

        result
    }

    override suspend fun pullUserData(): SyncResult = exclusive {
        val result = userDataPull.pullAll()

        val start = System.currentTimeMillis()
        _lastSyncTime.value = start
        database.appConfigQueries.upsert("last_sync", start.toString(), null, null, start)
        _syncStatus.value = if (result.success) SyncStatus.SUCCESS else SyncStatus.FAILED
        _lastSyncDiagnostics.value = result.message

        result
    }

    override suspend fun pushUserData(): SyncResult = exclusive {
        val result = userDataPush.pushAll()

        _syncStatus.value = if (result.success) SyncStatus.SUCCESS else SyncStatus.FAILED
        _lastSyncDiagnostics.value = result.message

        result
    }

    override suspend fun fullSync(): SyncResult = exclusive {
        val masterResult = masterDataPull.pullAll(diagnosticsHelper.readMasterWatermark())
        val userResult = userDataPull.pullAll()
        val result = if (masterResult.success && userResult.success) {
            userDataPush.pushAll()
        } else {
            SyncResult(
                success = false,
                message = buildString {
                    if (!masterResult.success) append(masterResult.message)
                    if (!userResult.success) {
                        if (isNotEmpty()) append("; ")
                        append(userResult.message)
                    }
                }
            )
        }
        _lastSyncDiagnostics.value = result.message
        result
    }

    override suspend fun pullVocabularyUpdates(since: Long): List<com.kotomichi.model.Vocabulary> = withContext(Dispatchers.IO) {
        val response = httpClient.get("$baseUrl/vocabulary?since=$since")
        if (response.status == HttpStatusCode.OK) {
            response.body<List<SupabaseVocabularyRow>>().map { it.toModel() }
        } else {
            emptyList()
        }
    }

    override suspend fun pullDeckUpdates(since: Long): List<com.kotomichi.model.Deck> = withContext(Dispatchers.IO) {
        val response = httpClient.get("$baseUrl/decks?since=$since")
        if (response.status == HttpStatusCode.OK) {
            response.body<List<SupabaseDeckRow>>().map { it.toModel() }
        } else {
            emptyList()
        }
    }

    override suspend fun pullConfigUpdates(since: Long): List<com.kotomichi.model.DirectionThresholds> = withContext(Dispatchers.IO) {
        val response = httpClient.get("$baseUrl/config/thresholds?since=$since")
        if (response.status == HttpStatusCode.OK) {
            response.body<List<SupabaseDirectionThresholdRow>>().map { it.toModel() }
        } else {
            emptyList()
        }
    }

    override suspend fun pushProgress(progressList: List<com.kotomichi.model.SrsProgress>): SyncResult {
        return userDataPush.pushProgress(progressList)
    }

    override suspend fun pushReviewLogs(logs: List<com.kotomichi.model.ReviewLog>): SyncResult {
        return userDataPush.pushReviewLogs(logs)
    }

    override suspend fun pushProfile(profile: com.kotomichi.model.UserProfile): SyncResult {
        return userDataPush.pushProfile(profile)
    }

    override suspend fun masterSyncSince(): Long? = withContext(Dispatchers.IO) {
        diagnosticsHelper.readMasterWatermark()
    }

    override suspend fun pendingReviewLogIds(): List<Long> = withContext(Dispatchers.IO) {
        diagnosticsHelper.readPendingReviewLogIds()
    }
}
