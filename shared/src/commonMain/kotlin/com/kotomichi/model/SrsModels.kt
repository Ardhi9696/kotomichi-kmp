package com.kotomichi.model

import kotlinx.serialization.Serializable

@Serializable
data class SrsProgress(
    val userId: String,
    val vocabularyId: Long,
    val direction: Direction,
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val retrievability: Double? = null,
    val dueAt: Long,
    val lastReviewAt: Long? = null,
    val reviewCount: Int = 0,
    val lapses: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val state: CardState
        get() = when {
            reviewCount == 0 -> CardState.NEW
            lapses > 0 -> CardState.RELEARNING
            else -> CardState.REVIEW
        }

    val dueDate: Long get() = dueAt
    val lastReview: Long get() = lastReviewAt ?: createdAt
    val reps: Int get() = reviewCount

    fun toRecord(): SrsProgressRecord = SrsProgressRecord(
        userId = userId,
        vocabularyId = vocabularyId,
        direction = direction.ordinal,
        stability = stability,
        difficulty = difficulty,
        retrievability = retrievability,
        dueAt = dueAt,
        lastReviewAt = lastReviewAt,
        reviewCount = reviewCount,
        lapses = lapses
    )
}

@Serializable
data class SrsProgressRecord(
    val userId: String,
    val vocabularyId: Long,
    val direction: Int,
    val stability: Double,
    val difficulty: Double,
    val retrievability: Double? = null,
    val dueAt: Long,
    val lastReviewAt: Long? = null,
    val reviewCount: Int,
    val lapses: Int
)

@Serializable
data class ReviewLog(
    val id: Long = 0,
    val userId: String,
    val vocabularyId: Long,
    val direction: Direction,
    val isNew: Boolean = false,
    val correctness: Boolean,
    val elapsedMs: Long,
    val rating: Rating,
    val stabilityBefore: Double? = null,
    val stabilityAfter: Double? = null,
    val difficultyBefore: Double? = null,
    val difficultyAfter: Double? = null,
    val retrievabilityBefore: Double? = null,
    val reviewedAt: Long = System.currentTimeMillis()
) {
    val responseTimeMs: Long get() = elapsedMs

    fun toRecord(): ReviewLogRecord = ReviewLogRecord(
        userId = userId,
        vocabularyId = vocabularyId,
        direction = direction.ordinal,
        isNew = if (isNew) 1 else 0,
        correctness = if (correctness) 1 else 0,
        elapsedMs = elapsedMs,
        rating = rating.ordinal,
        stabilityBefore = stabilityBefore,
        stabilityAfter = stabilityAfter,
        difficultyBefore = difficultyBefore,
        difficultyAfter = difficultyAfter,
        retrievabilityBefore = retrievabilityBefore,
        reviewedAt = reviewedAt
    )
}

@Serializable
data class ReviewLogRecord(
    val userId: String,
    val vocabularyId: Long,
    val direction: Int,
    val isNew: Int,
    val correctness: Int,
    val elapsedMs: Long,
    val rating: Int,
    val stabilityBefore: Double? = null,
    val stabilityAfter: Double? = null,
    val difficultyBefore: Double? = null,
    val difficultyAfter: Double? = null,
    val retrievabilityBefore: Double? = null,
    val reviewedAt: Long
)

enum class Direction(val label: String, val description: String) {
    KANJI_TO_MEANING("漢字 → 意味", "Mengucapkan imi dari kanji"),
    KANJI_TO_HIRAGANA("漢字 → ひらがな", "Mengucapkan hiragana dari kanji"),
    HIRAGANA_TO_MEANING("ひらがな → 意味", "Mengucapkan imi dari hiragana"),
    MEANING_TO_HIRAGANA("意味 → ひらがな", "Menulis hiragana dari imi"),
    HIRAGANA_TO_KANJI("ひらがな → 漢字", "Menulis kanji dari hiragana"),
    MEANING_TO_KANJI("意味 → 漢字", "Menulis kanji dari imi (paling sulit)");

    companion object {
        fun fromOrdinal(ordinal: Int): Direction = values()[ordinal]
    }
}

enum class CardState(val label: String) {
    NEW("Baru"),
    LEARNING("Belajar"),
    REVIEW("Review"),
    RELEARNING("Belajar Ulang");

    companion object {
        fun fromOrdinal(ordinal: Int): CardState = values()[ordinal]
    }
}

enum class Rating(val label: String, val fsrsValue: Int) {
    AGAIN("Salah", 1),
    HARD("Sulit", 2),
    GOOD("Baik", 3),
    EASY("Mudah", 4);

    companion object {
        fun fromOrdinal(ordinal: Int): Rating = values()[ordinal]
        fun fromFsrsValue(value: Int): Rating = values().first { it.fsrsValue == value }
    }
}

enum class SyncStatus {
    PENDING(0),
    SYNCED(1),
    FAILED(2);

    val value: Int
    constructor(value: Int) { this.value = value }
    companion object {
        fun fromValue(value: Int): SyncStatus = values().first { it.value == value }
    }
}