package com.kotomichi.repository

import com.kotomichi.db.KotomichiDatabase
import com.kotomichi.db.ReviewLog
import com.kotomichi.model.Rating
import com.kotomichi.usecase.GamificationUseCase
import kotlin.math.pow

/**
 * Menghitung dan menyelaraskan level/EXP profil dari aktivitas yang TERCATAT
 * (review_log + bonus streak harian), bukan hanya dari jalur pemberian EXP
 * per-jawaban yang bisa gagal diam-diam (silent error). Dengan ini level/exp
 * selalu terdeteksi selama user pernah mengerjakan Belajar/Review, di perangkat
 * mana pun, begitu log tertarik/ditulis.
 */
internal suspend fun recomputeProfileExpFromLogs(
    database: KotomichiDatabase,
    authRepository: AuthRepository
) {
    val uid = authRepository.currentUserId() ?: return
    val profile = authRepository.getCurrentUser() ?: return
    val totalExp = computeTotalExp(database, uid)
    val level = levelFromExp(totalExp)
    if (profile.level != level || profile.totalExp != totalExp) {
        authRepository.publishProfile(profile.copy(level = level, exp = totalExp.toInt()))
    }
}

/** Total EXP = jumlah per-log (aturan GamificationUseCase) + bonus streak harian. */
internal fun computeTotalExp(database: KotomichiDatabase, userId: String): Long {
    val logs = database.reviewLogQueries
        .selectByUser(userId, Int.MAX_VALUE.toLong(), 0L)
        .executeAsList()
    var total = 0L
    for ((day, dayLogs) in logs.groupBy { startOfDay(it.reviewed_at) }) {
        total += dayLogs.sumOf { estimateExpEarned(it) }
        total += streakBonusOn(database, userId, day)
    }
    return total
}

internal fun levelFromExp(totalExp: Long): Int {
    var level = 1
    while (totalExp >= expForLevel(level + 1)) level++
    return level
}

private fun expForLevel(level: Int): Long =
    (GamificationUseCase.BASE_EXP * level.toDouble().pow(GamificationUseCase.EXP_EXPONENT)).toLong()

private fun estimateExpEarned(log: ReviewLog): Long {
    val correct = log.correctness == 1L
    if (log.is_new == 1L) {
        return if (correct) GamificationUseCase.EXP_LEARN_NEW_CARD else 0L
    }
    if (!correct) return 0L
    return when (Rating.entries.getOrNull(log.rating.toInt())) {
        Rating.EASY -> GamificationUseCase.EXP_REVIEW_EASY
        Rating.HARD -> GamificationUseCase.EXP_REVIEW_HARD
        else -> GamificationUseCase.EXP_REVIEW_CORRECT
    }
}

private fun streakBonusOn(database: KotomichiDatabase, userId: String, dayStart: Long): Long =
    runCatching {
        database.appConfigQueries
            .selectByKey("streak_bonus:$userId:$dayStart")
            .executeAsOneOrNull()
            ?.value_json
            ?.toLongOrNull() ?: 0L
    }.getOrDefault(0L)

private fun startOfDay(timestamp: Long): Long {
    val calendar = java.util.Calendar.getInstance()
    calendar.timeInMillis = timestamp
    calendar.clear(java.util.Calendar.HOUR_OF_DAY)
    calendar.clear(java.util.Calendar.MINUTE)
    calendar.clear(java.util.Calendar.SECOND)
    calendar.clear(java.util.Calendar.MILLISECOND)
    return calendar.timeInMillis
}