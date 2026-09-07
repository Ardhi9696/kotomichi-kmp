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
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess

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
            val since = readMasterWatermark()
            // Pull vocabulary
            val vocabResult = pullVocabulary(since)
            totalSynced += vocabResult.itemsSynced
            totalFailed += vocabResult.itemsFailed
            
            // Pull decks
            val deckResult = pullDecks(since)
            totalSynced += deckResult.itemsSynced
            totalFailed += deckResult.itemsFailed

            // Pull deck-vocabulary links (full refresh; table has no updated_at column)
            val linkResult = pullDeckLinks()
            totalSynced += linkResult.itemsSynced
            totalFailed += linkResult.itemsFailed
            
            // Pull config
            val configResult = pullConfig(since)
            totalSynced += configResult.itemsSynced
            totalFailed += configResult.itemsFailed
            
            val serverTimestamp = System.currentTimeMillis()
            _lastSyncTime.value = serverTimestamp
            if (totalFailed == 0) {
                configQueries.upsert(MASTER_WATERMARK_KEY, serverTimestamp.toString(), null, null, serverTimestamp)
            }
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
            val uid = authRepository.currentUserId() ?: throw Exception("Tidak ada sesi aktif")

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

            val sections = buildList {
                add(sectionDiagnostic("progress", progressResult))
                add(sectionDiagnostic("review", reviewResult))
                add(sectionDiagnostic("profil", profileResult))
            }.joinToString("; ")

            _syncStatus.value = if (totalFailed > 0) SyncStatus.FAILED else SyncStatus.SUCCESS
            _lastSyncDiagnostics.value = if (totalFailed == 0) "Data berhasil dikirim [$sections]" else "Sebagian data gagal dikirim [$sections]"

            SyncResult(
                success = totalFailed == 0,
                message = if (totalFailed == 0) "Data berhasil dikirim [$sections]" else "Sebagian data gagal dikirim [$sections]",
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
        val masterResult = pullMasterData()
        val userResult = pullUserData()
        val result = if (masterResult.success && userResult.success) pushUserData() else {
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
        if (progressList.isEmpty()) return@withContext SyncResult(success = true, message = "Progress synced")
        val token = authRepository.getAccessToken()
        if (token.isNullOrBlank()) return@withContext SyncResult(success = false, message = "Tidak ada sesi aktif", itemsFailed = progressList.size)

        var synced = 0
        var failed = 0
        var firstErrorStatus: Int? = null
        progressList.chunked(200).forEach { chunk ->
            val rows = chunk.map { p ->
                SupabaseSrsProgress(
                    user_id = p.userId,
                    vocabulary_id = p.vocabularyId,
                    direction = p.direction.ordinal + 1,
                    stability = p.stability,
                    difficulty = p.difficulty,
                    retrievability = p.retrievability,
                    due_at = formatSupabaseTimestamp(p.dueAt),
                    last_review_at = p.lastReviewAt?.let { formatSupabaseTimestamp(it) },
                    review_count = p.reviewCount,
                    lapses = p.lapses,
                    created_at = formatSupabaseTimestamp(p.createdAt),
                    updated_at = formatSupabaseTimestamp(p.updatedAt)
                )
            }
            val response = httpClient.post("$baseUrl/srs_progress") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header("Prefer", "resolution=merge-duplicates")
                contentType(ContentType.Application.Json)
                setBody(rows)
            }
            if (response.status.isSuccess()) {
                synced += chunk.size
            } else {
                failed += chunk.size
                if (firstErrorStatus == null) firstErrorStatus = response.status.value
            }
        }
        val progressMessage = buildString {
            append(if (failed == 0) "Progress synced" else "Sebagian progress gagal dikirim")
            if (failed > 0 && firstErrorStatus != null) append(" (http $firstErrorStatus)")
        }
        SyncResult(
            success = failed == 0,
            message = progressMessage,
            itemsSynced = synced,
            itemsFailed = failed
        )
    }
    
    override suspend fun pushReviewLogs(logs: List<ReviewLog>): SyncResult = withContext(Dispatchers.IO) {
        if (logs.isEmpty()) return@withContext SyncResult(success = true, message = "Review logs synced")
        val token = authRepository.getAccessToken()
        if (token.isNullOrBlank()) return@withContext SyncResult(success = false, message = "Tidak ada sesi aktif", itemsFailed = logs.size)

        var synced = 0
        var failed = 0
        var firstErrorStatus: Int? = null
        logs.chunked(200).forEach { chunk ->
            val rows = chunk.map { l ->
                SupabaseReviewLog(
                    id = reviewLogRemoteId(l.userId, l.vocabularyId, l.direction.ordinal, l.reviewedAt, l.rating.ordinal),
                    user_id = l.userId,
                    vocabulary_id = l.vocabularyId,
                    direction = l.direction.ordinal + 1,
                    is_new = l.isNew,
                    correctness = l.correctness,
                    elapsed_ms = l.elapsedMs,
                    rating = l.rating.ordinal + 1,
                    stability_before = l.stabilityBefore,
                    stability_after = l.stabilityAfter,
                    difficulty_before = l.difficultyBefore,
                    difficulty_after = l.difficultyAfter,
                    retrievability_before = l.retrievabilityBefore,
                    reviewed_at = formatSupabaseTimestamp(l.reviewedAt)
                )
            }
            val response = httpClient.post("$baseUrl/review_log") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header("Prefer", "resolution=ignore-duplicates")
                contentType(ContentType.Application.Json)
                setBody(rows)
            }
            if (response.status.isSuccess()) {
                synced += chunk.size
            } else {
                failed += chunk.size
                if (firstErrorStatus == null) firstErrorStatus = response.status.value
            }
        }
        val reviewMessage = buildString {
            append(if (failed == 0) "Review logs synced" else "Sebagian review log gagal dikirim")
            if (failed > 0 && firstErrorStatus != null) append(" (http $firstErrorStatus)")
        }
        SyncResult(
            success = failed == 0,
            message = reviewMessage,
            itemsSynced = synced,
            itemsFailed = failed
        )
    }
    
    override suspend fun pushProfile(profile: UserProfile): SyncResult = withContext(Dispatchers.IO) {
        val token = authRepository.getAccessToken()
        if (token.isNullOrBlank()) return@withContext SyncResult(success = false, message = "Tidak ada sesi aktif", itemsFailed = 1)

        val row = SupabaseUserProfileRow(
            id = profile.id,
            display_name = profile.displayName,
            role = profile.role.name.lowercase(),
            preferred_locale = profile.preferredLocale,
            level = profile.level,
            exp = profile.exp,
            last_review_date = profile.lastReviewDate?.let { formatSupabaseDate(it) },
            current_streak = profile.currentStreak,
            longest_streak = profile.longestStreak,
            created_at = formatSupabaseTimestamp(profile.createdAt),
            updated_at = formatSupabaseTimestamp(profile.updatedAt),
            theme = profile.theme,
            last_seen_at = profile.lastSeenAt?.let { formatSupabaseTimestamp(it) }
        )
        val response = httpClient.post("$baseUrl/user_profile") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("Prefer", "resolution=merge-duplicates")
            contentType(ContentType.Application.Json)
            setBody(listOf(row))
        }
        if (response.status.isSuccess()) {
            SyncResult(success = true, message = "Profile synced", itemsSynced = 1)
        } else {
            SyncResult(success = false, message = "Profile gagal dikirim: ${response.status.value}", itemsFailed = 1)
        }
    }

    private suspend fun pushProgress(): SyncResult {
        val uid = authRepository.currentUserId()
            ?: return SyncResult(success = false, message = "Tidak ada sesi aktif", itemsFailed = 1)
        val all = try {
            srsQueries.selectByUser(uid).executeAsList().map { it.toModel() }
        } catch (e: Exception) {
            return SyncResult(success = false, message = "Gagal memuat progress lokal: ${e.message}", itemsFailed = 1)
        }
        return pushProgress(all)
    }

    private suspend fun pushReviewLogs(): SyncResult {
        val pendingIds = readPendingReviewLogIds()
        if (pendingIds.isEmpty()) return SyncResult(success = true, message = "Review logs synced")

        val logs = mutableListOf<ReviewLog>()
        val pruned = pendingIds.toMutableList()
        pendingIds.forEach { id ->
            val row = try {
                reviewQueries.selectById(id).executeAsOneOrNull()
            } catch (e: Exception) {
                null
            }
            if (row != null) {
                logs += row.toModel()
            } else {
                pruned.remove(id)
            }
        }
        if (logs.isEmpty()) {
            savePendingReviewLogIds(emptyList())
            return SyncResult(success = true, message = "Review logs synced")
        }

        val result = pushReviewLogs(logs)
        if (result.success) {
            val sentIds = logs.map { it.id }
            savePendingReviewLogIds(pruned.filter { it !in sentIds })
        }
        return result
    }

    private suspend fun pushProfile(): SyncResult {
        val profile = authRepository.currentUser.firstOrNull()
            ?: return SyncResult(success = false, message = "Tidak ada sesi aktif", itemsFailed = 1)
        return pushProfile(profile)
    }

    private fun sectionDiagnostic(name: String, r: SyncResult): String =
        if (r.success) "$name OK (${r.itemsSynced})"
        else "$name GAGAL (sync ${r.itemsSynced}, fail ${r.itemsFailed})${if (r.message.isNotBlank()) " - ${r.message}" else ""}"

    private fun readMasterWatermark(): Long? = try {
        configQueries.selectByKey(MASTER_WATERMARK_KEY).executeAsOneOrNull()
            ?.value_json?.toLongOrNull()?.takeIf { it > 0 }
    } catch (e: Exception) {
        null
    }

    private fun readPendingReviewLogIds(): List<Long> = try {
        decodeLongList(configQueries.selectByKey(PENDING_REVIEW_LOG_KEY).executeAsOneOrNull()?.value_json)
    } catch (e: Exception) {
        emptyList()
    }

    private fun savePendingReviewLogIds(ids: List<Long>) {
        configQueries.upsert(PENDING_REVIEW_LOG_KEY, encodeLongList(ids), null, null, System.currentTimeMillis())
    }
    
    private suspend fun pullVocabulary(since: Long?): SyncResult = withContext(Dispatchers.IO) {
        val url = if (since != null) {
            "$baseUrl/vocabulary?select=*,vocabulary_translations(*)&updated_at=gt.${formatSupabaseTimestamp(since)}"
        } else {
            "$baseUrl/vocabulary?select=*,vocabulary_translations(*)"
        }
        val response = httpClient.get(url)
        
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
    
    private suspend fun pullDecks(since: Long?): SyncResult = withContext(Dispatchers.IO) {
        val url = if (since != null) {
            "$baseUrl/decks?updated_at=gt.${formatSupabaseTimestamp(since)}"
        } else {
            "$baseUrl/decks"
        }
        val response = httpClient.get(url)
        
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
    
    private suspend fun pullConfig(since: Long?): SyncResult = withContext(Dispatchers.IO) {
        val url = if (since != null) {
            "$baseUrl/direction_thresholds?updated_at=gt.${formatSupabaseTimestamp(since)}"
        } else {
            "$baseUrl/direction_thresholds"
        }
        val response = httpClient.get(url)

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

    override suspend fun masterSyncSince(): Long? = withContext(Dispatchers.IO) { readMasterWatermark() }

    override suspend fun pendingReviewLogIds(): List<Long> = withContext(Dispatchers.IO) { readPendingReviewLogIds() }

    private class ProfilePull(val profile: com.kotomichi.model.UserProfile?, val authorized: Boolean)

    private suspend fun pullRemoteProfile(token: String): ProfilePull? = withContext(Dispatchers.IO) {
        val uid = authRepository.currentUserId() ?: return@withContext null
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
        val uid = authRepository.currentUserId() ?: return@withContext emptyList()
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
        val uid = authRepository.currentUserId() ?: return@withContext emptyList()
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