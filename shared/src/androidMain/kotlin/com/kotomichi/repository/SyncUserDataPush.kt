/**
 * File: SyncUserDataPush.kt
 * Responsibility: Mendorong data user-specific ke server (SRS progress, review logs, profile).
 *                 Memerlukan autentikasi user. Mendukung retry per-item untuk isolasi error.
 */
package com.kotomichi.repository

import com.kotomichi.db.KotomichiDatabase
import com.kotomichi.model.SrsProgress
import com.kotomichi.model.ReviewLog
import com.kotomichi.model.UserProfile
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

/**
 * Manager untuk push user data ke server.
 * @param database Database queries untuk membaca data lokal
 * @param httpClient HTTP client untuk request ke server
 * @param baseUrl Base URL API server
 * @param authRepository Repository untuk akses token dan user ID
 */
internal class SyncUserDataPush(
    private val database: KotomichiDatabase,
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val authRepository: AuthRepository
) {
    private val srsQueries = database.srsProgressQueries
    private val reviewQueries = database.reviewLogQueries
    private val configQueries = database.appConfigQueries

    /**
     * Push semua user data ke server (progress, review logs, profile).
     * @return Hasil sinkronisasi
     */
    suspend fun pushAll(): SyncResult = withContext(Dispatchers.IO) {
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

            SyncResult(
                success = totalFailed == 0,
                message = if (totalFailed == 0) "Data berhasil dikirim [$sections]" else "Sebagian data gagal dikirim [$sections]",
                itemsSynced = totalSynced,
                itemsFailed = totalFailed
            )
        } catch (e: Exception) {
            SyncResult(
                success = false,
                message = "Push gagal: ${e.message}",
                itemsFailed = 1
            )
        }
    }

    /**
     * Push SRS progress ke server. Filter hanya progress yang sudah direview (reviewCount > 0).
     * Mendukung retry per-item untuk isolasi error.
     */
    suspend fun pushProgress(progressList: List<SrsProgress>): SyncResult = withContext(Dispatchers.IO) {
        // Kartu yang belum pernah direview belum punya state FSRS (stability=0, difficulty=0,
        // retrievability=null) dan berisiko ditolak constraint server. Tidak perlu dikirim.
        val eligible = progressList.filter { it.reviewCount > 0 }
        if (eligible.isEmpty()) {
            return@withContext SyncResult(success = true, message = "Progress synced (tidak ada kartu direview untuk dikirim)")
        }
        val token = authRepository.getAccessToken()
        if (token.isNullOrBlank()) return@withContext SyncResult(success = false, message = "Tidak ada sesi aktif", itemsFailed = eligible.size)

        fun rowFor(p: SrsProgress) = SupabaseSrsProgress(
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

        var synced = 0
        var failed = 0
        var firstErrorStatus: Int? = null
        var firstErrorDetail: String? = null

        eligible.chunked(200).forEach { chunk ->
            val rows = chunk.map { rowFor(it) }
            val response = httpClient.post("$baseUrl/srs_progress") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header("Prefer", "resolution=merge-duplicates")
                contentType(ContentType.Application.Json)
                setBody(rows)
            }
            if (response.status.isSuccess()) {
                synced += chunk.size
            } else {
                val errorDetail = runCatching { response.bodyAsText() }.getOrElse { "" }.take(200)
                if (firstErrorStatus == null) {
                    firstErrorStatus = response.status.value
                    firstErrorDetail = errorDetail
                }
                // Isolasi: satu baris buruk jangan menggagalkan baris lain. Coba per baris.
                var chunkSynced = 0
                var chunkFailed = 0
                chunk.forEach { p ->
                    val single = httpClient.post("$baseUrl/srs_progress") {
                        header(HttpHeaders.Authorization, "Bearer $token")
                        header("Prefer", "resolution=merge-duplicates")
                        contentType(ContentType.Application.Json)
                        setBody(listOf(rowFor(p)))
                    }
                    if (single.status.isSuccess()) chunkSynced++ else chunkFailed++
                }
                synced += chunkSynced
                failed += chunkFailed
            }
        }
        val progressMessage = buildString {
            append(if (failed == 0) "Progress synced" else "Sebagian progress gagal dikirim")
            if (failed > 0 && firstErrorStatus != null) append(" (http $firstErrorStatus")
            if (failed > 0 && !firstErrorDetail.isNullOrBlank()) append(": $firstErrorDetail")
            if (failed > 0 && firstErrorStatus != null) append(")")
        }
        SyncResult(
            success = failed == 0,
            message = progressMessage,
            itemsSynced = synced,
            itemsFailed = failed
        )
    }

    /**
     * Push review logs ke server. Mendukung retry per-item untuk isolasi error.
     */
    suspend fun pushReviewLogs(logs: List<ReviewLog>): SyncResult = withContext(Dispatchers.IO) {
        if (logs.isEmpty()) return@withContext SyncResult(success = true, message = "Review logs synced")
        val token = authRepository.getAccessToken()
        if (token.isNullOrBlank()) return@withContext SyncResult(success = false, message = "Tidak ada sesi aktif", itemsFailed = logs.size)

        var synced = 0
        var failed = 0
        var firstErrorStatus: Int? = null
        var firstErrorDetail: String? = null
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
                val errorDetail = runCatching { response.bodyAsText() }.getOrElse { "" }.take(200)
                if (firstErrorStatus == null) {
                    firstErrorStatus = response.status.value
                    firstErrorDetail = errorDetail
                }
                var chunkSynced = 0
                var chunkFailed = 0
                chunk.forEach { l ->
                    val singleRows = listOf(
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
                    )
                    val single = httpClient.post("$baseUrl/review_log") {
                        header(HttpHeaders.Authorization, "Bearer $token")
                        header("Prefer", "resolution=ignore-duplicates")
                        contentType(ContentType.Application.Json)
                        setBody(singleRows)
                    }
                    if (single.status.isSuccess()) chunkSynced++ else chunkFailed++
                }
                synced += chunkSynced
                failed += chunkFailed
            }
        }
        val reviewMessage = buildString {
            append(if (failed == 0) "Review logs synced" else "Sebagian review log gagal dikirim")
            if (failed > 0 && firstErrorStatus != null) append(" (http $firstErrorStatus")
            if (failed > 0 && !firstErrorDetail.isNullOrBlank()) append(": $firstErrorDetail")
            if (failed > 0 && firstErrorStatus != null) append(")")
        }
        SyncResult(
            success = failed == 0,
            message = reviewMessage,
            itemsSynced = synced,
            itemsFailed = failed
        )
    }

    /**
     * Push profile user ke server.
     */
    suspend fun pushProfile(profile: UserProfile): SyncResult = withContext(Dispatchers.IO) {
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
            val errorDetail = runCatching { response.bodyAsText() }.getOrElse { "" }.take(200)
            SyncResult(
                success = false,
                message = "Profile gagal dikirim: ${response.status.value}${if (errorDetail.isNotBlank()) ": $errorDetail" else ""}",
                itemsFailed = 1
            )
        }
    }

    // ── Private helpers untuk push dari local DB ──

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
        val profile = authRepository.currentUser.first()
            ?: return SyncResult(success = false, message = "Tidak ada sesi aktif", itemsFailed = 1)
        return pushProfile(profile)
    }

    private fun readPendingReviewLogIds(): List<Long> = try {
        decodeLongList(configQueries.selectByKey(PENDING_REVIEW_LOG_KEY).executeAsOneOrNull()?.value_json)
    } catch (e: Exception) {
        emptyList()
    }

    private fun savePendingReviewLogIds(ids: List<Long>) {
        configQueries.upsert(PENDING_REVIEW_LOG_KEY, "[" + ids.joinToString(",") + "]", null, null, System.currentTimeMillis())
    }
}
