package com.kotomichi.usecase

import com.kotomichi.model.UserProfile
import com.kotomichi.model.DailyStats
import com.kotomichi.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GamificationUseCase(
    private val progressRepository: ProgressRepository
) {
    companion object {
        const val BASE_EXP = 100L
        const val EXP_EXPONENT = 1.5
        const val EXP_LEARN_NEW_CARD = 10L
        const val EXP_REVIEW_CORRECT = 5L
        const val EXP_REVIEW_HARD = 3L
        const val EXP_REVIEW_EASY = 2L
        const val EXP_DAILY_STREAK_BASE = 20L
        val STREAK_BONUS_THRESHOLDS = listOf(7, 30, 90, 180, 365)
        val STREAK_BONUS_EXP = listOf(50L, 200L, 500L, 1000L, 2000L)
    }
    
    fun calculateExpForLevel(level: Int): Long {
        return (BASE_EXP * Math.pow(level.toDouble(), EXP_EXPONENT)).toLong()
    }
    
    fun calculateLevelFromExp(totalExp: Long): Int {
        var level = 1
        while (totalExp >= calculateExpForLevel(level + 1)) {
            level++
        }
        return level
    }
    
    fun calculateExpProgress(currentLevel: Int, totalExp: Long): Double {
        val currentLevelExp = calculateExpForLevel(currentLevel)
        val nextLevelExp = calculateExpForLevel(currentLevel + 1)
        val progress = totalExp - currentLevelExp
        val needed = nextLevelExp - currentLevelExp
        return (progress.toDouble() / needed * 100).coerceIn(0.0, 100.0)
    }
    
    suspend fun awardLearnExp(userId: String): Long {
        return EXP_LEARN_NEW_CARD
    }
    
    suspend fun awardReviewExp(userId: String, isCorrect: Boolean, rating: com.kotomichi.model.Rating): Long {
        return when {
            !isCorrect -> 0
            rating == com.kotomichi.model.Rating.EASY -> EXP_REVIEW_EASY
            rating == com.kotomichi.model.Rating.HARD -> EXP_REVIEW_HARD
            else -> EXP_REVIEW_CORRECT
        }
    }
    
    suspend fun updateStreak(userId: String, lastActiveDate: Long): Pair<Int, Long> {
        val today = getStartOfDay(System.currentTimeMillis())
        val lastActive = getStartOfDay(lastActiveDate)
        val daysDiff = (today - lastActive) / (24 * 60 * 60 * 1000)
        
        val currentStreak = progressRepository.getUserStatistics(userId).currentStreak
        val newStreak = when {
            daysDiff == 0L -> currentStreak
            daysDiff == 1L -> currentStreak + 1
            else -> 1
        }
        
        var bonusExp = 0L
        if (daysDiff == 1L) {
            bonusExp = EXP_DAILY_STREAK_BASE
            STREAK_BONUS_THRESHOLDS.forEachIndexed { index, threshold ->
                if (newStreak % threshold == 0) {
                    bonusExp += STREAK_BONUS_EXP[index]
                }
            }
            progressRepository.addDailyStreakBonus(userId, today, bonusExp)
        }
        
        return newStreak to bonusExp
    }
    
    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    suspend fun getTotalExpToday(userId: String): Long {
        val stats = progressRepository.getDailyStats(userId, 1)
        return stats.firstOrNull()?.expEarned ?: 0
    }
}