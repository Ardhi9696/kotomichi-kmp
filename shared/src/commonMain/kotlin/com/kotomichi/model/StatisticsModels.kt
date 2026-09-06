package com.kotomichi.model

import kotlinx.serialization.Serializable

@Serializable
data class DailyStats(
    val date: Long,
    val learnCount: Int = 0,
    val reviewCount: Int = 0,
    val correctCount: Int = 0,
    val totalTimeMs: Long = 0,
    val expEarned: Long = 0
) {
    val accuracy: Double
        get() = if (reviewCount > 0) correctCount.toDouble() / reviewCount * 100 else 0.0
    val totalCount: Int
        get() = learnCount + reviewCount
}

@Serializable
data class DeckProgress(
    val deckId: Long,
    val totalVocab: Int,
    val learnedVocab: Int,
    val reviewingVocab: Int,
    val masteredVocab: Int,
    val averageRetrievability: Double,
    val masteryPercent: Double
)

@Serializable
data class UserStatistics(
    val totalVocabLearned: Int,
    val totalReviews: Int,
    val totalCorrect: Int,
    val totalStudyTimeMs: Long,
    val currentStreak: Int,
    val longestStreak: Int,
    val totalExp: Long,
    val currentLevel: Int,
    val averageAccuracy: Double,
    val directionBreakdown: Map<Direction, DirectionStats>
)

@Serializable
data class DirectionStats(
    val totalReviews: Int,
    val correctReviews: Int,
    val averageResponseTimeMs: Long,
    val averageStability: Double,
    val cardsInReview: Int
) {
    val accuracy: Double
        get() = if (totalReviews > 0) correctReviews.toDouble() / totalReviews * 100 else 0.0
}

@Serializable
data class HeatmapData(
    val date: Long,
    val count: Int
)