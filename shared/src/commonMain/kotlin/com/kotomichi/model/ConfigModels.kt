package com.kotomichi.model

import kotlinx.serialization.Serializable

@Serializable
data class DirectionThresholds(
    val direction: Direction,
    val easyThresholdSec: Int = 8,
    val goodThresholdSec: Int = 15,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun ratingFromResponseTime(responseTimeMs: Long, isCorrect: Boolean): Rating {
        if (!isCorrect) return Rating.AGAIN
        val seconds = responseTimeMs / 1000
        return when {
            seconds < easyThresholdSec -> Rating.EASY
            seconds < goodThresholdSec -> Rating.GOOD
            else -> Rating.HARD
        }
    }
}

@Serializable
data class SystemConfig(
    val key: String,
    val value: String,
    val description: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

object ConfigKeys {
    const val DIRECTION_THRESHOLDS_PREFIX = "direction_thresholds_"
    const val EXP_BASE = "exp_base"
    const val EXP_EXPONENT = "exp_exponent"
    const val MASTERY_THRESHOLD = "mastery_threshold"
    const val STREAK_BONUS_DAYS = "streak_bonus_days"
    const val STREAK_BONUS_EXP = "streak_bonus_exp"
    const val SYNC_INTERVAL_HOURS = "sync_interval_hours"
    const val AUDIO_CACHE_MAX_SIZE_MB = "audio_cache_max_size_mb"
}

object DefaultConfig {
    const val EXP_BASE = 100
    const val EXP_EXPONENT = 1.5
    const val MASTERY_THRESHOLD = 0.90
    const val STREAK_BONUS_DAYS = "7,30,90,180,365"
    const val STREAK_BONUS_EXP = "50,200,500,1000,2000"
    const val SYNC_INTERVAL_HOURS = 6
    const val AUDIO_CACHE_MAX_SIZE_MB = 100
}