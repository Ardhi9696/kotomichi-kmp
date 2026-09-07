/**
 * File: SyncDiagnostics.kt
 * Responsibility: Helper untuk diagnostic sinkronisasi - watermark, pending review log IDs,
 *                 dan format pesan diagnostic per section.
 */
package com.kotomichi.repository

import kotlinx.serialization.json.Json

internal const val MASTER_WATERMARK_KEY = "master_sync_since"
internal const val PENDING_REVIEW_LOG_KEY = "pending_review_log_ids"

private val pendingJson = Json { ignoreUnknownKeys = true }

internal fun encodeLongList(items: List<Long>): String = "[" + items.joinToString(",") + "]"

internal fun decodeLongList(json: String?): List<Long> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        pendingJson.decodeFromString<List<Long>>(json)
    } catch (e: Exception) {
        emptyList()
    }
}

/**
 * Extension untuk SyncRepositoryImpl yang menyediakan akses ke config queries
 * untuk diagnostic dan persistence state.
 */
internal class SyncDiagnosticsHelper(
    private val configQueries: com.kotomichi.db.AppConfigQueries
) {
    fun readMasterWatermark(): Long? = try {
        configQueries.selectByKey(MASTER_WATERMARK_KEY).executeAsOneOrNull()
            ?.value_json?.toLongOrNull()?.takeIf { it > 0 }
    } catch (e: Exception) {
        null
    }

    fun readPendingReviewLogIds(): List<Long> = try {
        decodeLongList(configQueries.selectByKey(PENDING_REVIEW_LOG_KEY).executeAsOneOrNull()?.value_json)
    } catch (e: Exception) {
        emptyList()
    }

    fun savePendingReviewLogIds(ids: List<Long>) {
        configQueries.upsert(PENDING_REVIEW_LOG_KEY, "[" + ids.joinToString(",") + "]", null, null, System.currentTimeMillis())
    }
}

/** Format diagnostic message per section (progress, review, profil). */
internal fun sectionDiagnostic(name: String, r: SyncResult): String =
    if (r.success) "$name OK (${r.itemsSynced})"
    else "$name GAGAL (sync ${r.itemsSynced}, fail ${r.itemsFailed})${if (r.message.isNotBlank()) " - ${r.message}" else ""}"
