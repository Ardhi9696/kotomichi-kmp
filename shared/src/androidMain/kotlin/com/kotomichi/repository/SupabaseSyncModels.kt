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

@Serializable
internal data class SupabaseVocabularyTranslation(
    val vocabulary_id: Long,
    val locale: String,
    val meaning: String
)

@Serializable
internal data class SupabaseDeckVocabularyRow(
    val deck_id: Long,
    val vocabulary_id: Long,
    val order_in_deck: Long? = null
)

@Serializable
internal data class SupabaseVocabularyRow(
    val id: Long,
    val kanji: String? = null,
    val hiragana: String? = null,
    val romaji: String? = null,
    val jlpt_level: String? = null,
    val part_of_speech: String? = null,
    val is_active: Boolean? = null,
    val created_by: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val jft_basic: Boolean? = null,
    val godan_verb: Boolean? = null,
    val ichidan_verb: Boolean? = null,
    val fukisoku: Boolean? = null,
    val i_adjective: Boolean? = null,
    val na_adjective: Boolean? = null,
    val jidoushi: Boolean? = null,
    val tadoushi: Boolean? = null,
    val verb_collocation: Boolean? = null,
    val vocabulary_translations: List<SupabaseVocabularyTranslation> = emptyList()
) {
    fun toModel(): com.kotomichi.model.Vocabulary = com.kotomichi.model.Vocabulary(
        id = id,
        kanji = kanji,
        hiragana = hiragana ?: "",
        romaji = romaji,
        jlptLevel = jlpt_level?.let { com.kotomichi.model.JlptLevel.valueOf(it) },
        partOfSpeech = part_of_speech,
        isActive = is_active ?: true,
        createdBy = created_by,
        createdAt = parseSupabaseTimestamp(created_at),
        updatedAt = parseSupabaseTimestamp(updated_at),
        jftBasic = jft_basic ?: false,
        godanVerb = godan_verb ?: false,
        ichidanVerb = ichidan_verb ?: false,
        fukisoku = fukisoku ?: false,
        iAdjective = i_adjective ?: false,
        naAdjective = na_adjective ?: false,
        jidoushi = jidoushi ?: false,
        tadoushi = tadoushi ?: false,
        verbCollocation = verb_collocation ?: false,
        translations = vocabulary_translations.map {
            com.kotomichi.model.VocabularyTranslation(it.vocabulary_id, it.locale, it.meaning)
        }
    )
}

@Serializable
internal data class SupabaseDeckRow(
    val id: Long,
    val title: String,
    val subtitle: String? = null,
    val jlpt_level: String? = null,
    val order_index: Int = 0,
    val is_published: Boolean = false,
    val created_by: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val jft_basic: Boolean = false
) {
    fun toModel(): com.kotomichi.model.Deck = com.kotomichi.model.Deck(
        id = id,
        title = title,
        subtitle = subtitle,
        jlptLevel = jlpt_level?.let { com.kotomichi.model.JlptLevel.valueOf(it) },
        orderIndex = order_index,
        isPublished = is_published,
        createdBy = created_by,
        createdAt = parseSupabaseTimestamp(created_at),
        updatedAt = parseSupabaseTimestamp(updated_at),
        jftBasic = jft_basic
    )
}

internal fun SupabaseUserProfileRow.toModelProfile(): com.kotomichi.model.UserProfile = com.kotomichi.model.UserProfile(
    id = id,
    displayName = display_name ?: "",
    role = when (role?.lowercase()) {
        "super_admin" -> com.kotomichi.model.UserRole.SUPER_ADMIN
        "admin" -> com.kotomichi.model.UserRole.ADMIN
        else -> com.kotomichi.model.UserRole.USER
    },
    preferredLocale = preferred_locale ?: "id",
    level = level ?: 1,
    exp = exp ?: 0,
    lastReviewDate = parseSupabaseDate(last_review_date),
    currentStreak = current_streak ?: 0,
    longestStreak = longest_streak ?: 0,
    createdAt = parseSupabaseTimestamp(created_at),
    updatedAt = parseSupabaseTimestamp(updated_at),
    theme = theme ?: "system",
    lastSeenAt = parseSupabaseTimestamp(last_seen_at)
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