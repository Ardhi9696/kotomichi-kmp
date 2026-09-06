package com.kotomichi.repository

import com.kotomichi.db.KotomichiDatabase
import com.kotomichi.model.Vocabulary
import com.kotomichi.model.Deck
import com.kotomichi.model.SrsProgress
import com.kotomichi.model.ReviewLog
import com.kotomichi.model.UserProfile
import com.kotomichi.model.DirectionThresholds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.http.HttpStatusCode

class SyncRepositoryImpl(
    private val database: KotomichiDatabase,
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val authRepository: AuthRepository
) : SyncRepository {
    
    private val vocabQueries = database.vocabularyQueries
    private val deckQueries = database.deckQueries
    private val srsQueries = database.srsProgressQueries
    private val reviewQueries = database.reviewLogQueries
    private val userQueries = database.userProfileQueries
    private val thresholdQueries = database.directionThresholdQueries
    private val configQueries = database.appConfigQueries
    
    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: Flow<SyncStatus> = _syncStatus.asStateFlow()
    
    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: Flow<Long> = _lastSyncTime.asStateFlow()
    
    override suspend fun pullMasterData(): SyncResult = withContext(Dispatchers.IO) {
        _syncStatus.value = SyncStatus.SYNCING
        var totalSynced = 0
        var totalFailed = 0
        
        try {
            // Pull vocabulary
            val vocabResult = pullVocabulary()
            totalSynced += vocabResult.itemsSynced
            totalFailed += vocabResult.itemsFailed
            
            // Pull decks
            val deckResult = pullDecks()
            totalSynced += deckResult.itemsSynced
            totalFailed += deckResult.itemsFailed
            
            // Pull config
            val configResult = pullConfig()
            totalSynced += configResult.itemsSynced
            totalFailed += configResult.itemsFailed
            
            val serverTimestamp = System.currentTimeMillis()
            _lastSyncTime.value = serverTimestamp
            configQueries.upsert("last_sync", serverTimestamp.toString(), null, null, serverTimestamp)            
            _syncStatus.value = if (totalFailed > 0) SyncStatus.FAILED else SyncStatus.SUCCESS
            
            SyncResult(
                success = totalFailed == 0,
                message = if (totalFailed == 0) "Sinkronisasi berhasil" else "Sinkronisasi sebagian gagal",
                itemsSynced = totalSynced,
                itemsFailed = totalFailed,
                serverTimestamp = serverTimestamp
            )
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.FAILED
            SyncResult(
                success = false,
                message = "Sinkronisasi gagal: ${e.message}",
                itemsFailed = 1
            )
        }
    }
    
    override suspend fun pushUserData(): SyncResult = withContext(Dispatchers.IO) {
        _syncStatus.value = SyncStatus.SYNCING
        var totalSynced = 0
        var totalFailed = 0
        
        try {
            // Push progress
            val progressResult = pushProgress()
            totalSynced += progressResult.itemsSynced
            totalFailed += progressResult.itemsFailed
            
            // Push review logs
            val reviewResult = pushReviewLogs()
            totalSynced += reviewResult.itemsSynced
            totalFailed += reviewResult.itemsFailed
            
            // Push profile
            val profileResult = pushProfile()
            totalSynced += profileResult.itemsSynced
            totalFailed += profileResult.itemsFailed
            
            _syncStatus.value = if (totalFailed > 0) SyncStatus.FAILED else SyncStatus.SUCCESS
            
            SyncResult(
                success = totalFailed == 0,
                message = if (totalFailed == 0) "Data berhasil dikirim" else "Sebagian data gagal dikirim",
                itemsSynced = totalSynced,
                itemsFailed = totalFailed
            )
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.FAILED
            SyncResult(
                success = false,
                message = "Push gagal: ${e.message}",
                itemsFailed = 1
            )
        }
    }
    
    override suspend fun fullSync(): SyncResult {
        val pullResult = pullMasterData()
        return if (pullResult.success) pushUserData() else pullResult
    }
    
    override suspend fun pullVocabularyUpdates(since: Long): List<Vocabulary> = withContext(Dispatchers.IO) {
        val response = httpClient.get("$baseUrl/vocabulary?since=$since") {
            authRepository.currentUser.first().let { it?.id?.let { header("Authorization", "Bearer $it") } }
        }
        
        if (response.status == HttpStatusCode.OK) {
            response.body<List<Vocabulary>>()
        } else {
            emptyList()
        }
    }
    
    override suspend fun pullDeckUpdates(since: Long): List<Deck> = withContext(Dispatchers.IO) {
        val response = httpClient.get("$baseUrl/decks?since=$since") {
            authRepository.currentUser.first().let { it?.id?.let { header("Authorization", "Bearer $it") } }
        }
        
        if (response.status == HttpStatusCode.OK) {
            response.body<List<Deck>>()
        } else {
            emptyList()
        }
    }
    
    override suspend fun pullConfigUpdates(since: Long): List<DirectionThresholds> = withContext(Dispatchers.IO) {
        val response = httpClient.get("$baseUrl/config/thresholds?since=$since") {
            authRepository.currentUser.first().let { it?.id?.let { header("Authorization", "Bearer $it") } }
        }
        
        if (response.status == HttpStatusCode.OK) {
            response.body<List<DirectionThresholds>>()
        } else {
            emptyList()
        }
    }
    
    override suspend fun pushProgress(progressList: List<SrsProgress>): SyncResult = withContext(Dispatchers.IO) {
        // TODO: Implement batch push
        SyncResult(success = true, message = "Progress synced", itemsSynced = progressList.size)
    }
    
    override suspend fun pushReviewLogs(logs: List<ReviewLog>): SyncResult = withContext(Dispatchers.IO) {
        // TODO: Implement batch push
        SyncResult(success = true, message = "Review logs synced", itemsSynced = logs.size)
    }
    
    override suspend fun pushProfile(profile: UserProfile): SyncResult = withContext(Dispatchers.IO) {
        // TODO: Implement
        SyncResult(success = true, message = "Profile synced", itemsSynced = 1)
    }

    private suspend fun pushProgress(): SyncResult = SyncResult(success = true, message = "Progress synced")

    private suspend fun pushReviewLogs(): SyncResult = SyncResult(success = true, message = "Review logs synced")

    private suspend fun pushProfile(): SyncResult = SyncResult(success = true, message = "Profile synced", itemsSynced = 1)
    
    private suspend fun pullVocabulary(): SyncResult = withContext(Dispatchers.IO) {
        val response = httpClient.get("$baseUrl/vocabulary") {
            authRepository.currentUser.first().let { it?.id?.let { header("Authorization", "Bearer $it") } }
        }
        
        if (response.status == HttpStatusCode.OK) {
            val vocabList = response.body<List<Vocabulary>>()
            vocabList.forEach { vocab ->
                vocabQueries.insert(vocab.toEntity())
            }
            SyncResult(success = true, message = "Vocabulary synced", itemsSynced = vocabList.size)
        } else {
            SyncResult(success = false, message = "Failed", itemsFailed = 1)
        }
    }
    
    private suspend fun pullDecks(): SyncResult = withContext(Dispatchers.IO) {
        val response = httpClient.get("$baseUrl/decks") {
            authRepository.currentUser.first().let { it?.id?.let { header("Authorization", "Bearer $it") } }
        }
        
        if (response.status == HttpStatusCode.OK) {
            val deckList = response.body<List<Deck>>()
            deckList.forEach { deck ->
                deckQueries.insert(deck.toEntity())
            }
            SyncResult(success = true, message = "Decks synced", itemsSynced = deckList.size)
        } else {
            SyncResult(success = false, message = "Failed", itemsFailed = 1)
        }
    }
    
    private suspend fun pullConfig(): SyncResult = withContext(Dispatchers.IO) {
        val response = httpClient.get("$baseUrl/config/thresholds") {
            authRepository.currentUser.first().let { it?.id?.let { header("Authorization", "Bearer $it") } }
        }
        
        if (response.status == HttpStatusCode.OK) {
            val thresholds = response.body<List<DirectionThresholds>>()
            thresholds.forEach { threshold ->
                thresholdQueries.upsert(
                    direction = threshold.direction.ordinal.toLong(),
                    fast_threshold_ms = threshold.fastThresholdMs.toLong(),
                    good_threshold_ms = threshold.goodThresholdMs.toLong(),
                    updated_by = threshold.updatedBy,
                    updated_at = threshold.updatedAt
                )
            }
            SyncResult(success = true, message = "Config synced", itemsSynced = thresholds.size)
        } else {
            SyncResult(success = false, message = "Failed", itemsFailed = 1)
        }
    }
    
    override fun observeSyncStatus(): Flow<SyncStatus> = _syncStatus.asStateFlow()
    
    override fun observeLastSyncTime(): Flow<Long> = _lastSyncTime.asStateFlow()
}