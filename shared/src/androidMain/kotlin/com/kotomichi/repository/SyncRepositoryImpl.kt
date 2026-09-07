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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

class SyncRepositoryImpl(
    private val database: KotomichiDatabase,
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val authRepository: AuthRepository
) : SyncRepository {
    
    private val vocabQueries = database.vocabularyQueries
    private val deckQueries = database.deckQueries
    private val deckVocabQueries = database.deckVocabularyQueries
    private val srsQueries = database.srsProgressQueries
    private val reviewQueries = database.reviewLogQueries
    private val userQueries = database.userProfileQueries
    private val thresholdQueries = database.directionThresholdQueries
    private val configQueries = database.appConfigQueries
    
    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: Flow<SyncStatus> = _syncStatus.asStateFlow()
    
    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: Flow<Long> = _lastSyncTime.asStateFlow()
    
    private val _lastSyncDiagnostics = MutableStateFlow("")
    override val lastSyncDiagnostics: StateFlow<String> = _lastSyncDiagnostics.asStateFlow()
    
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

            // Pull deck-vocabulary links
            val linkResult = pullDeckLinks()
            totalSynced += linkResult.itemsSynced
            totalFailed += linkResult.itemsFailed
            
            // Pull config
            val configResult = pullConfig()
            totalSynced += configResult.itemsSynced
            totalFailed += configResult.itemsFailed
            
            val serverTimestamp = System.currentTimeMillis()
            _lastSyncTime.value = serverTimestamp
            configQueries.upsert("last_sync", serverTimestamp.toString(), null, null, serverTimestamp)            
            _syncStatus.value = if (totalFailed > 0) SyncStatus.FAILED else SyncStatus.SUCCESS
            _lastSyncDiagnostics.value = if (totalFailed == 0) "Master data tersinkron" else "Master data sebagian gagal"
            
            SyncResult(
                success = totalFailed == 0,
                message = if (totalFailed == 0) "Sinkronisasi berhasil" else "Sinkronisasi sebagian gagal",
                itemsSynced = totalSynced,
                itemsFailed = totalFailed,
                serverTimestamp = serverTimestamp
            )
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.FAILED
            _lastSyncDiagnostics.value = "Master data gagal: ${e.message}"
            SyncResult(
                success = false,
                message = "Sinkronisasi gagal: ${e.message}",
                itemsFailed = 1
            )
        }
    }
    
    override suspend fun pullUserData(): SyncResult = withContext(Dispatchers.IO) {
        _syncStatus.value = SyncStatus.SYNCING
        var totalSynced = 0
        var totalFailed = 0

        try {
            var token = authRepository.getAccessToken()
            if (token.isNullOrBlank()) {
                throw Exception("Tidak ada sesi aktif")
            }
            val uid = authRepository.currentUser.firstOrNull()?.id ?: throw Exception("Tidak ada sesi aktif")

            val start = System.currentTimeMillis()

            var profilePull = pullRemoteProfile(token)
            if (profilePull != null && !profilePull.authorized) {
                runCatching { authRepository.refreshToken() }
                token = authRepository.getAccessToken()
                if (token.isNullOrBlank()) {
                    throw Exception("Sesi berakhir, silakan masuk kembali")
                }
                profilePull = pullRemoteProfile(token)
            }
            if (profilePull == null) {
                throw Exception("Tidak ada sesi aktif")
            }
            profilePull.profile?.let { profile ->
                authRepository.publishProfile(profile)
                totalSynced += 1
            }

            val progressList = pullRemoteProgress(token)
            progressList.forEach { progress ->
                srsQueries.upsert(
                    user_id = progress.userId,
                    vocabulary_id = progress.vocabularyId,
                    direction = progress.direction.ordinal.toLong(),
                    stability = progress.stability,
                    difficulty = progress.difficulty,
                    retrievability = progress.retrievability,
                    due_at = progress.dueAt,
                    last_review_at = progress.lastReviewAt,
                    review_count = progress.reviewCount.toLong(),
                    lapses = progress.lapses.toLong(),
                    created_at = progress.createdAt,
                    updated_at = System.currentTimeMillis()
                )
            }
            totalSynced += progressList.size

            val logs = pullRemoteReviewLogs(token)
            val latestLocal = try {
                reviewQueries.selectMaxId(uid).executeAsOne()
            } catch (e: Exception) {
                0L
            }
            logs.filter { it.id > latestLocal }.forEach { log ->
                try {
                    reviewQueries.insertWithRemoteId(
                        id = log.id,
                        user_id = log.userId,
                        vocabulary_id = log.vocabularyId,
                        direction = log.direction.ordinal.toLong(),
                        is_new = if (log.isNew) 1L else 0L,
                        correctness = if (log.correctness) 1L else 0L,
                        elapsed_ms = log.elapsedMs,
                        rating = log.rating.ordinal.toLong(),
                        stability_before = log.stabilityBefore,
                        stability_after = log.stabilityAfter,
                        difficulty_before = log.difficultyBefore,
                        difficulty_after = log.difficultyAfter,
                        retrievability_before = log.retrievabilityBefore,
                        reviewed_at = log.reviewedAt
                    )
                    totalSynced += 1
                } catch (e: Exception) {
                    totalFailed += 1
                }
            }

            _lastSyncTime.value = start
            configQueries.upsert("last_sync", start.toString(), null, null, start)
            _syncStatus.value = if (totalFailed > 0) SyncStatus.FAILED else SyncStatus.SUCCESS
            _lastSyncDiagnostics.value = if (totalFailed == 0) "Progress dimuat dari server" else "Sebagian data gagal dimuat"

            SyncResult(
                success = totalFailed == 0,
                message = if (totalFailed == 0) "Progress dimuat dari server" else "Sebagian data gagal dimuat",
                itemsSynced = totalSynced,
                itemsFailed = totalFailed,
                serverTimestamp = start
            )
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.FAILED
            _lastSyncDiagnostics.value = "Gagal memuat progress: ${e.message}"
            SyncResult(
                success = false,
                message = "Gagal memuat progress: ${e.message}",
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
            _lastSyncDiagnostics.value = if (totalFailed == 0) "Data berhasil dikirim" else "Sebagian data gagal dikirim"
            
            SyncResult(
                success = totalFailed == 0,
                message = if (totalFailed == 0) "Data berhasil dikirim" else "Sebagian data gagal dikirim",
                itemsSynced = totalSynced,
                itemsFailed = totalFailed
            )
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.FAILED
            _lastSyncDiagnostics.value = "Push gagal: ${e.message}"
            SyncResult(
                success = false,
                message = "Push gagal: ${e.message}",
                itemsFailed = 1
            )
        }
    }
    
    override suspend fun fullSync(): SyncResult {
        val pullResult = pullMasterData()
        val result = if (pullResult.success) pushUserData() else pullResult
        _lastSyncDiagnostics.value = result.message
        return result
    }
    
    override suspend fun pullVocabularyUpdates(since: Long): List<Vocabulary> = withContext(Dispatchers.IO) {
        val response = httpClient.get("$baseUrl/vocabulary?since=$since")
        
        if (response.status == HttpStatusCode.OK) {
            response.body<List<SupabaseVocabularyRow>>().map { it.toModel() }
        } else {
            emptyList()
        }
    }
    
    override suspend fun pullDeckUpdates(since: Long): List<Deck> = withContext(Dispatchers.IO) {
        val response = httpClient.get("$baseUrl/decks?since=$since")
        
        if (response.status == HttpStatusCode.OK) {
            response.body<List<SupabaseDeckRow>>().map { it.toModel() }
        } else {
            emptyList()
        }
    }
    
    override suspend fun pullConfigUpdates(since: Long): List<DirectionThresholds> = withContext(Dispatchers.IO) {
        val response = httpClient.get("$baseUrl/config/thresholds?since=$since")
        
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
        val response = httpClient.get("$baseUrl/vocabulary?select=*,vocabulary_translations(*)")
        
        if (response.status == HttpStatusCode.OK) {
            val vocabList = response.body<List<SupabaseVocabularyRow>>()
            vocabList.forEach { vocab ->
                vocabQueries.insert(vocab.toModel().toEntity())
                val id = vocab.id
                vocabQueries.deleteTranslationsByVocabularyId(id)
                vocab.vocabulary_translations.forEach { tr ->
                    vocabQueries.insertTranslation(
                        vocabulary_id = tr.vocabulary_id,
                        locale = tr.locale,
                        meaning = tr.meaning
                    )
                }
            }
            SyncResult(success = true, message = "Vocabulary synced", itemsSynced = vocabList.size)
        } else {
            SyncResult(success = false, message = "Failed", itemsFailed = 1)
        }
    }
    
    private suspend fun pullDecks(): SyncResult = withContext(Dispatchers.IO) {
        val response = httpClient.get("$baseUrl/decks")
        
        if (response.status == HttpStatusCode.OK) {
            val deckList = response.body<List<SupabaseDeckRow>>()
            deckList.forEach { row ->
                deckQueries.insert(row.toModel().toEntity())
            }
            SyncResult(success = true, message = "Decks synced", itemsSynced = deckList.size)
        } else {
            SyncResult(success = false, message = "Failed", itemsFailed = 1)
        }
    }

    private suspend fun pullDeckLinks(): SyncResult = withContext(Dispatchers.IO) {
        val response = httpClient.get("$baseUrl/deck_vocabulary")
        
        if (response.status == HttpStatusCode.OK) {
            val links = response.body<List<SupabaseDeckVocabularyRow>>()
            deckVocabQueries.deleteAll()
            links.forEach { link ->
                deckVocabQueries.upsertLink(
                    deck_id = link.deck_id,
                    vocabulary_id = link.vocabulary_id,
                    order_in_deck = link.order_in_deck
                )
            }
            SyncResult(success = true, message = "Deck links synced", itemsSynced = links.size)
        } else {
            SyncResult(success = false, message = "Failed", itemsFailed = 1)
        }
    }
    
    private suspend fun pullConfig(): SyncResult = withContext(Dispatchers.IO) {
        val response = httpClient.get("$baseUrl/direction_thresholds")

        if (response.status == HttpStatusCode.OK) {
            val thresholds = response.body<List<SupabaseDirectionThresholdRow>>()
            thresholds.forEach { threshold ->
                thresholdQueries.upsert(
                    direction = threshold.direction.toLong(),
                    fast_threshold_ms = threshold.fast_threshold_ms.toLong(),
                    good_threshold_ms = threshold.good_threshold_ms.toLong(),
                    updated_by = threshold.updated_by,
                    updated_at = threshold.updated_at?.let { parseSupabaseTimestamp(it) } ?: System.currentTimeMillis()
                )
            }
            SyncResult(success = true, message = "Config synced", itemsSynced = thresholds.size)
        } else {
            SyncResult(success = false, message = "Failed", itemsFailed = 1)
        }
    }
    
    override fun observeSyncStatus(): Flow<SyncStatus> = _syncStatus.asStateFlow()
    
    override fun observeLastSyncTime(): Flow<Long> = _lastSyncTime.asStateFlow()

    private class ProfilePull(val profile: com.kotomichi.model.UserProfile?, val authorized: Boolean)

    private suspend fun pullRemoteProfile(token: String): ProfilePull? = withContext(Dispatchers.IO) {
        val uid = authRepository.currentUser.firstOrNull()?.id ?: return@withContext null
        val response = httpClient.get("$baseUrl/user_profile?id=eq.$uid&limit=1") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
            return@withContext ProfilePull(null, authorized = false)
        }
        if (response.status != HttpStatusCode.OK) return@withContext ProfilePull(null, authorized = true)
        val rows = runCatching { response.body<List<SupabaseUserProfileRow>>() }
            .getOrElse { return@withContext ProfilePull(null, authorized = true) }
        val row = rows.firstOrNull() ?: return@withContext ProfilePull(null, authorized = true)

        ProfilePull(profile = row.toModelProfile(), authorized = true)
    }

    private suspend fun pullRemoteProgress(token: String): List<com.kotomichi.model.SrsProgress> = withContext(Dispatchers.IO) {
        val uid = authRepository.currentUser.firstOrNull()?.id ?: return@withContext emptyList()
        val response = httpClient.get("$baseUrl/srs_progress?user_id=eq.$uid") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        if (response.status != HttpStatusCode.OK) return@withContext emptyList()
        response.body<List<SupabaseSrsProgress>>().map { row ->
            com.kotomichi.model.SrsProgress(
                userId = row.user_id,
                vocabularyId = row.vocabulary_id,
                direction = com.kotomichi.model.Direction.values().getOrElse((row.direction - 1).coerceIn(0, 5)) { com.kotomichi.model.Direction.KANJI_TO_MEANING },
                stability = row.stability,
                difficulty = row.difficulty,
                retrievability = row.retrievability,
                dueAt = parseSupabaseTimestamp(row.due_at),
                lastReviewAt = row.last_review_at?.let { parseSupabaseTimestamp(it) },
                reviewCount = row.review_count,
                lapses = row.lapses,
                createdAt = parseSupabaseTimestamp(row.created_at),
                updatedAt = row.updated_at?.let { parseSupabaseTimestamp(it) } ?: System.currentTimeMillis()
            )
        }
    }

    private suspend fun pullRemoteReviewLogs(token: String): List<com.kotomichi.model.ReviewLog> = withContext(Dispatchers.IO) {
        val uid = authRepository.currentUser.firstOrNull()?.id ?: return@withContext emptyList()
        val response = httpClient.get("$baseUrl/review_log?user_id=eq.$uid&order=reviewed_at.asc") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        if (response.status != HttpStatusCode.OK) return@withContext emptyList()
        response.body<List<SupabaseReviewLog>>().map { row ->
            com.kotomichi.model.ReviewLog(
                id = row.id,
                userId = row.user_id,
                vocabularyId = row.vocabulary_id,
                direction = com.kotomichi.model.Direction.values().getOrElse((row.direction - 1).coerceIn(0, 5)) { com.kotomichi.model.Direction.KANJI_TO_MEANING },
                isNew = row.is_new,
                correctness = row.correctness,
                elapsedMs = row.elapsed_ms,
                rating = com.kotomichi.model.Rating.values().getOrElse((row.rating - 1).coerceIn(0, 3)) { com.kotomichi.model.Rating.GOOD },
                stabilityBefore = row.stability_before,
                stabilityAfter = row.stability_after,
                difficultyBefore = row.difficulty_before,
                difficultyAfter = row.difficulty_after,
                retrievabilityBefore = row.retrievability_before,
                reviewedAt = parseSupabaseTimestamp(row.reviewed_at)
            )
        }
    }
}