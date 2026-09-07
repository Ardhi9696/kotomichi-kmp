/**
 * File: SyncHelpers.kt
 * Responsibility: Fungsi helper untuk format timestamp Supabase dan utility umum
 *                 yang digunakan oleh semua modul sinkronisasi.
 */
package com.kotomichi.repository

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Format timestamp epoch_ms ke format ISO-8601 yang diterima Supabase. */
internal fun formatSupabaseTimestamp(epochMs: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date(epochMs))
}

/** Format epoch_ms ke format date (yyyy-MM-dd) untuk Supabase. */
internal fun formatSupabaseDate(epochMs: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date(epochMs))
}

/** Parse timestamp ISO-8601 dari Supabase ke epoch_ms. */
internal fun parseSupabaseTimestamp(iso: String?): Long {
    if (iso.isNullOrBlank()) return System.currentTimeMillis()
    return try {
        val cleaned = iso.substringBefore('.')
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            .parse(cleaned.substringBefore('Z'))
            ?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}

/** Parse date ISO-8601 dari Supabase ke epoch_ms. */
internal fun parseSupabaseDate(date: String?): Long? {
    if (date.isNullOrBlank()) return null
    return try {
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .parse(date)
            ?.time
    } catch (e: Exception) {
        null
    }
}

/** Generate deterministic negative ID untuk review log (mencegah kolisi dengan server). */
internal fun reviewLogRemoteId(userId: String, vocabularyId: Long, direction: Int, reviewedAt: Long, rating: Int): Long {
    val canonical = "$userId|$vocabularyId|$direction|$reviewedAt|$rating"
    var hash = -3750763034362895579L
    canonical.toByteArray(java.nio.charset.StandardCharsets.UTF_8).forEach { b ->
        hash = (hash xor (b.toLong() and 0xff)) * 0x100000001b3L
    }
    val magnitude = if (hash == Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(hash)
    return if (magnitude == 0L) -1L else -magnitude
}
