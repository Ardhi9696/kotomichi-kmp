package com.kotomichi.repository

import com.kotomichi.model.Vocabulary
import com.kotomichi.model.Deck
import com.kotomichi.model.SrsProgress
import com.kotomichi.model.ReviewLog
import com.kotomichi.model.UserProfile
import com.kotomichi.model.DirectionThresholds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SyncRepository {
    suspend fun pullMasterData(): SyncResult
    suspend fun pullUserData(): SyncResult
    suspend fun pushUserData(): SyncResult
    suspend fun fullSync(): SyncResult
    suspend fun pullVocabularyUpdates(since: Long): List<Vocabulary>
    suspend fun pullDeckUpdates(since: Long): List<Deck>
    suspend fun pullConfigUpdates(since: Long): List<DirectionThresholds>
    suspend fun pushProgress(progressList: List<SrsProgress>): SyncResult
    suspend fun pushReviewLogs(logs: List<ReviewLog>): SyncResult
    suspend fun pushProfile(profile: UserProfile): SyncResult
    
    fun observeSyncStatus(): Flow<SyncStatus>
    fun observeLastSyncTime(): Flow<Long>
    val lastSyncDiagnostics: StateFlow<String>
}

data class SyncResult(
    val success: Boolean,
    val message: String,
    val itemsSynced: Int = 0,
    val itemsFailed: Int = 0,
    val serverTimestamp: Long = System.currentTimeMillis()
)

enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    FAILED,
    CONFLICT
}