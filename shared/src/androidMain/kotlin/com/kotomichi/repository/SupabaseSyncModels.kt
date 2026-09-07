package com.kotomichi.repository

import kotlinx.serialization.Serializable

@Serializable
internal data class SupabaseSrsProgress(
    val user_id: String,
    val vocabulary_id: Long,
    val direction: Int,
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val retrievability: Double? = null,
    val due_at: String,
    val last_review_at: String? = null,
    val review_count: Int = 0,
    val lapses: Int = 0,
    val created_at: String,
    val updated_at: String? = null
)

@Serializable
internal data class SupabaseReviewLog(
    val id: Long,
    val user_id: String,
    val vocabulary_id: Long,
    val direction: Int,
    val is_new: Boolean = false,
    val correctness: Boolean,
    val elapsed_ms: Long,
    val rating: Int,
    val stability_before: Double? = null,
    val stability_after: Double? = null,
    val difficulty_before: Double? = null,
    val difficulty_after: Double? = null,
    val retrievability_before: Double? = null,
    val reviewed_at: String
)

@Serializable
internal data class SupabaseUserProfileRow(
    val id: String,
    val display_name: String? = null,
    val role: String? = null,
    val preferred_locale: String? = null,
    val level: Int? = null,
    val exp: Int? = null,
    val last_review_date: String? = null,
    val current_streak: Int? = null,
    val longest_streak: Int? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val theme: String? = null,
    val last_seen_at: String? = null
)

@Serializable
internal data class SupabaseDirectionThresholdRow(
    val direction: Int,
    val fast_threshold_ms: Int,
    val good_threshold_ms: Int,
    val updated_by: String? = null,
    val updated_at: String? = null
)

internal fun parseSupabaseTimestamp(iso: String?): Long {
    if (iso.isNullOrBlank()) return System.currentTimeMillis()
    return try {
        val cleaned = iso.substringBefore('.')
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            .parse(cleaned.substringBefore('Z'))
            ?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}

internal fun parseSupabaseDate(date: String?): Long? {
    if (date.isNullOrBlank()) return null
    return try {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .parse(date)
            ?.time
    } catch (e: Exception) {
        null
    }
}