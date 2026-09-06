package com.kotomichi.model

import kotlinx.serialization.Serializable

@Serializable
data class SrsProgress(
    val id: Long = 0,
    val userId: String,
    val vocabularyId: Long,
    val direction: Direction,
    val stability: Double,
    val difficulty: Double,
    val elapsedDays: Double,
    val scheduledDays: Double,
    val reps: Int,
    val lapses: Int,
    val state: CardState,
    val lastReview: Long,
    val dueDate: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toRecord(): SrsProgressRecord = SrsProgressRecord(
        userId = userId,
        vocabularyId = vocabularyId,
        direction = direction.ordinal,
        stability = stability,
        difficulty = difficulty,
        elapsedDays = elapsedDays,
        scheduledDays = scheduledDays,
        reps = reps,
        lapses = lapses,
        state = state.ordinal,
        lastReview = lastReview,
        dueDate = dueDate
    )
}

@Serializable
data class SrsProgressRecord(
    val userId: String,
    val vocabularyId: Long,
    val direction: Int,
    val stability: Double,
    val difficulty: Double,
    val elapsedDays: Double,
    val scheduledDays: Double,
    val reps: Int,
    val lapses: Int,
    val state: Int,
    val lastReview: Long,
    val dueDate: Long
)

@Serializable
data class ReviewLog(
    val id: Long = 0,
    val userId: String,
    val vocabularyId: Long,
    val direction: Direction,
    val rating: Rating,
    val responseTimeMs: Long,
    val stabilityBefore: Double,
    val difficultyBefore: Double,
    val stabilityAfter: Double,
    val difficultyAfter: Double,
    val reviewedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
) {
    fun toRecord(): ReviewLogRecord = ReviewLogRecord(
        userId = userId,
        vocabularyId = vocabularyId,
        direction = direction.ordinal,
        rating = rating.ordinal,
        responseTimeMs = responseTimeMs,
        stabilityBefore = stabilityBefore,
        difficultyBefore = difficultyBefore,
        stabilityAfter = stabilityAfter,
        difficultyAfter = difficultyAfter,
        reviewedAt = reviewedAt,
        syncStatus = syncStatus.ordinal
    )
}

@Serializable
data class ReviewLogRecord(
    val userId: String,
    val vocabularyId: Long,
    val direction: Int,
    val rating: Int,
    val responseTimeMs: Long,
    val stabilityBefore: Double,
    val difficultyBefore: Double,
    val stabilityAfter: Double,
    val difficultyAfter: Double,
    val reviewedAt: Long,
    val syncStatus: Int
)

enum class Direction(val label: String, val description: String) {
    KANJI_TO_MEANING(0, "Kanji → Arti", "Mengucapkan arti dari kanji"),
    KANJI_TO_HIRAGANA(1, "Kanji → Hiragana", "Mengucapkan hiragana dari kanji"),
    HIRAGANA_TO_MEANING(2, "Hiragana → Arti", "Mengucapkan arti dari hiragana"),
    MEANING_TO_HIRAGANA(3, "Arti → Hiragana", "Menulis hiragana dari arti"),
    HIRAGANA_TO_KANJI(4, "Hiragana → Kanji", "Menulis kanji dari hiragana"),
    MEANING_TO_KANJI(5, "Arti → Kanji", "Menulis kanji dari arti (paling sulit)");

    companion object {
        fun fromOrdinal(ordinal: Int): Direction = values()[ordinal]
    }
}

enum class CardState(val label: String) {
    NEW(0, "Baru"),
    LEARNING(1, "Belajar"),
    REVIEW(2, "Review"),
    RELEARNING(3, "Belajar Ulang");

    companion object {
        fun fromOrdinal(ordinal: Int): CardState = values()[ordinal]
    }
}

enum class Rating(val label: String, val fsrsValue: Int) {
    AGAIN(0, "Salah", 1),
    HARD(1, "Sulit", 2),
    GOOD(2, "Baik", 3),
    EASY(3, "Mudah", 4);

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