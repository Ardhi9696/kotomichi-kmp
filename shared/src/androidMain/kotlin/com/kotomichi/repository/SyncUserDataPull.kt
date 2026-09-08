/**
 * File: SyncUserDataPull.kt
 * Responsibility: Menarik data user-specific dari server (profile, SRS progress, review logs).
 *                 Memerlukan autentikasi user.
 */
package com.kotomichi.repository

import com.kotomichi.db.KotomichiDatabase
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manager untuk pull user data dari server.
 * @param database Database queries untuk menyimpan data
 * @param httpClient HTTP client untuk request ke server
 * @param baseUrl Base URL API server
 * @param authRepository Repository untuk akses token dan user ID
 */
internal class SyncUserDataPull(
    private val database: KotomichiDatabase,
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val authRepository: AuthRepository
) {
    private val srsQueries = database.srsProgressQueries
    private val reviewQueries = database.reviewLogQueries
    private val userQueries = database.userProfileQueries

    /**
     * Pull semua user data dari server (profile, progress, review logs).
     * @return Hasil sinkronisasi
     */
    suspend fun pullAll(): SyncResult = withContext(Dispatchers.IO) {
        var totalSynced = 0
        var totalFailed = 0

        try {
            var token = authRepository.getAccessToken()
            if (token.isNullOrBlank()) {
                throw Exception("Tidak ada sesi aktif")
            }
            val uid = authRepository.currentUserId() ?: throw Exception("Tidak ada sesi aktif")

            val start = System.currentTimeMillis()

            // Pull profile
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

            // Pull progress
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

            // Pull review logs. Dedupe by content identity (user, vocab, direction,
            // reviewed_at ada di detik, rating) BUKAN dengan membandingkan id.
            // Local memakai id auto-increment positif, sedangkan remote memakai id
            // hash negatif yang deterministik -> membandingkan `id > latestLocal`
            // selalu gagal menarik log antar-perangkat (log dari device lain tidak
            // pernah masuk). Identitas konten menyamakan log yang sama di kedua sisi.
            val logs = pullRemoteReviewLogs(token)
            logs.forEach { log ->
                try {
                    val exists = reviewQueries.selectExistsByIdentity(
                        userId = log.userId,
                        vocabularyId = log.vocabularyId,
                        direction = log.direction.ordinal.toLong(),
                        reviewedAtSeconds = log.reviewedAt / 1000,
                        rating = log.rating.ordinal.toLong()
                    ).executeAsOne()
                    if (exists > 0L) return@forEach
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

            SyncResult(
                success = totalFailed == 0,
                message = if (totalFailed == 0) "Progress dimuat dari server" else "Sebagian data gagal dimuat",
                itemsSynced = totalSynced,
                itemsFailed = totalFailed,
                serverTimestamp = start
            )
        } catch (e: Exception) {
            SyncResult(
                success = false,
                message = "Gagal memuat progress: ${e.message}",
                itemsFailed = 1
            )
        }
    }

    private data class ProfilePull(val profile: com.kotomichi.model.UserProfile?, val authorized: Boolean)

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
