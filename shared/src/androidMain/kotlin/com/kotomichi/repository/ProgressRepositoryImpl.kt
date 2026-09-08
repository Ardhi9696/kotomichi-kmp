package com.kotomichi.repository

import com.kotomichi.db.KotomichiDatabase
import com.kotomichi.db.SrsProgress
import com.kotomichi.db.ReviewLog
import com.kotomichi.db.UserProfile
import com.kotomichi.model.SrsProgress as ModelSrsProgress
import com.kotomichi.model.ReviewLog as ModelReviewLog
import com.kotomichi.model.UserProfile as ModelUserProfile
import com.kotomichi.model.DailyStats as ModelDailyStats
import com.kotomichi.model.Direction
import com.kotomichi.model.CardState
import com.kotomichi.model.Rating
import com.kotomichi.model.DeckProgress
import com.kotomichi.model.UserStatistics
import com.kotomichi.model.HeatmapData
import com.kotomichi.model.DirectionStats
import app.cash.sqldelight.coroutines.asFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProgressRepositoryImpl(
    private val database: KotomichiDatabase
) : ProgressRepository {

    private val srsQueries = database.srsProgressQueries
    private val reviewQueries = database.reviewLogQueries
    private val userQueries = database.userProfileQueries
    private val configQueries = database.appConfigQueries

    private val _dueCount = MutableStateFlow(0)
    val dueCountFlow: Flow<Int> = _dueCount

    private val _deckProgressFlow = MutableStateFlow<Map<Long, DeckProgress>>(emptyMap())
    val deckProgressFlow: Flow<Map<Long, DeckProgress>> = _deckProgressFlow

    override suspend fun getProgress(userId: String, vocabularyId: Long, direction: Direction): ModelSrsProgress? = withContext(Dispatchers.IO) {
        srsQueries.selectByUserAndVocabAndDirection(userId, vocabularyId, direction.ordinal.toLong()).executeAsOneOrNull()?.toModel()
    }

    override suspend fun getAllProgress(userId: String): List<ModelSrsProgress> = withContext(Dispatchers.IO) {
        srsQueries.selectByUser(userId).executeAsList().map { it.toModel() }
    }

    override suspend fun getDueCards(userId: String, limit: Int): List<ModelSrsProgress> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        srsQueries.selectDueCards(userId, now, limit.toLong()).executeAsList().map { it.toModel() }
    }

    override suspend fun getNewCards(userId: String, deckId: Long, limit: Int): List<ModelSrsProgress> = withContext(Dispatchers.IO) {
        srsQueries.selectNewCardsForDeck(userId, deckId, limit.toLong()).executeAsList().map { it.toModel() }
    }

    override suspend fun getLearnedVocabIds(userId: String): Set<Long> = withContext(Dispatchers.IO) {
        srsQueries.selectLearnedVocabIds(userId).executeAsList().toSet()
    }

    override suspend fun getProgressByDeck(userId: String, deckId: Long): List<ModelSrsProgress> = withContext(Dispatchers.IO) {
        srsQueries.selectByDeck(userId, deckId).executeAsList().map { it.toModel() }
    }

    override suspend fun insertProgress(progress: ModelSrsProgress) = withContext(Dispatchers.IO) {
        srsQueries.insert(progress.toEntity())
    }

    override suspend fun updateProgress(progress: ModelSrsProgress) = withContext(Dispatchers.IO) {
        srsQueries.upsert(
            user_id = progress.userId,
            vocabulary_id = progress.vocabularyId,
            direction = progress.direction.ordinal.toLong(),
            stability = progress.stability,
            difficulty = progress.difficulty,
            retrievability = progress.retrievability,
            due_at = progress.dueAt,
            last_review_at = progress.lastReviewAt,
            review_count = progress.reviewCount.toLong(),
            lapses = progress.lapses.toLong(),
            created_at = progress.createdAt,
            updated_at = System.currentTimeMillis()
        )
    }

    override suspend fun upsertProgress(progress: ModelSrsProgress) = withContext(Dispatchers.IO) {
        srsQueries.upsert(
            user_id = progress.userId,
            vocabulary_id = progress.vocabularyId,
            direction = progress.direction.ordinal.toLong(),
            stability = progress.stability,
            difficulty = progress.difficulty,
            retrievability = progress.retrievability,
            due_at = progress.dueAt,
            last_review_at = progress.lastReviewAt,
            review_count = progress.reviewCount.toLong(),
            lapses = progress.lapses.toLong(),
            created_at = progress.createdAt,
            updated_at = System.currentTimeMillis()
        )
    }

    override suspend fun deleteProgress(userId: String, vocabularyId: Long, direction: Direction) = withContext(Dispatchers.IO) {
        srsQueries.delete(userId, vocabularyId, direction.ordinal.toLong())
    }

    override suspend fun insertReviewLog(log: ModelReviewLog) {
        withContext(Dispatchers.IO) {
            val id = reviewQueries.insertAndReturnId(
                user_id = log.userId,
                vocabulary_id = log.vocabularyId,
                direction = log.direction.ordinal.toLong(),
                is_new = if (log.isNew) 1L else 0L,
                correctness = if (log.correctness) 1L else 0L,
                elapsed_ms = log.elapsedMs,
                rating = log.rating.ordinal.toLong(),
                stability_before = log.stabilityBefore,
                stability_after = log.stabilityAfter,
                difficulty_before = log.difficultyBefore,
                difficulty_after = log.difficultyAfter,
                retrievability_before = log.retrievabilityBefore,
                reviewed_at = log.reviewedAt
            ).executeAsOne()
            appendPendingReviewLogId(id)
        }
    }

    private fun appendPendingReviewLogId(id: Long) {
        val current = try {
            configQueries.selectByKey(PENDING_REVIEW_LOG_KEY).executeAsOneOrNull()?.value_json
        } catch (e: Exception) {
            null
        }
        val ids = decodeLongList(current)
        if (id !in ids) {
            configQueries.upsert(PENDING_REVIEW_LOG_KEY, encodeLongList(ids + id), null, null, System.currentTimeMillis())
        }
    }

    override suspend fun getReviewLogs(userId: String, limit: Int, offset: Int): List<ModelReviewLog> = withContext(Dispatchers.IO) {
        reviewQueries.selectByUser(userId, limit.toLong(), offset.toLong()).executeAsList().map { it.toModel() }
    }

    override suspend fun getReviewLogsByVocab(userId: String, vocabularyId: Long): List<ModelReviewLog> = withContext(Dispatchers.IO) {
        reviewQueries.selectByUserAndVocab(userId, vocabularyId).executeAsList().map { it.toModel() }
    }

    override suspend fun getDeckProgress(userId: String, deckId: Long): DeckProgress = withContext(Dispatchers.IO) {
        val progressList = srsQueries.selectByDeck(userId, deckId).executeAsList().map { it.toModel() }
        calculateDeckProgress(progressList)
    }

    override suspend fun getDailyStats(userId: String, days: Int): List<ModelDailyStats> = withContext(Dispatchers.IO) {
        val endDate = System.currentTimeMillis()
        val startDate = endDate - (days * 24 * 60 * 60 * 1000L)
        reviewQueries.selectByUser(userId, Int.MAX_VALUE.toLong(), 0L)
            .executeAsList()
            .map { it.toModel() }
            .filter { it.reviewedAt in startDate..endDate }
            .groupBy { startOfDay(it.reviewedAt) }
            .map { (day, logs) ->
                ModelDailyStats(
                    date = day,
                    learnCount = logs.count { it.isNew },
                    reviewCount = logs.size,
                    correctCount = logs.count { it.correctness },
                    totalTimeMs = logs.sumOf { it.elapsedMs },
                    expEarned = logs.sumOf { estimateExpEarned(it) } + readDailyStreakBonus(userId, day)
                )
            }
            .sortedBy { it.date }
    }

    override suspend fun getUserStatistics(userId: String): UserStatistics = withContext(Dispatchers.IO) {
        val allProgress = srsQueries.selectByUser(userId).executeAsList().map { it.toModel() }
        val allLogs = reviewQueries.selectByUser(userId, Int.MAX_VALUE.toLong(), 0L).executeAsList().map { it.toModel() }

        val totalReviews = allLogs.size
        val totalCorrect = allLogs.count { it.correctness }
        val totalStudyTime = allLogs.sumOf { it.elapsedMs }
        val directionBreakdown = Direction.values().associateWith { direction ->
            val dirLogs = allLogs.filter { it.direction == direction }
            val dirProgress = allProgress.filter { it.direction == direction }
            DirectionStats(
                totalReviews = dirLogs.size,
                correctReviews = dirLogs.count { it.correctness },
                averageResponseTimeMs = if (dirLogs.isNotEmpty()) dirLogs.sumOf { it.elapsedMs } / dirLogs.size else 0,
                averageStability = if (dirProgress.isNotEmpty()) dirProgress.sumOf { it.stability } / dirProgress.size else 0.0,
                cardsInReview = dirProgress.count { it.state == CardState.REVIEW }
            )
        }

        val profile = userQueries.selectById(userId).executeAsOneOrNull()?.toModel()

        UserStatistics(
            totalVocabLearned = allProgress.count { it.state != CardState.NEW },
            totalReviews = totalReviews,
            totalCorrect = totalCorrect,
            totalStudyTimeMs = totalStudyTime,
            currentStreak = profile?.currentStreak ?: 0,
            longestStreak = profile?.longestStreak ?: 0,
            totalExp = profile?.totalExp ?: 0,
            currentLevel = profile?.currentLevel ?: 1,
            averageAccuracy = if (totalReviews > 0) totalCorrect.toDouble() / totalReviews * 100 else 0.0,
            directionBreakdown = directionBreakdown
        )
    }

    override suspend fun getHeatmapData(userId: String, days: Int): List<HeatmapData> = withContext(Dispatchers.IO) {
        val stats = getDailyStats(userId, days)
        stats.map { HeatmapData(it.date, it.totalCount) }
    }

    override suspend fun getConfigValue(key: String): String? = withContext(Dispatchers.IO) {
        configQueries.selectByKey(key).executeAsOneOrNull()?.value_json
    }

    override suspend fun setConfigValue(key: String, value: String) = withContext(Dispatchers.IO) {
        configQueries.upsert(key, value, null, null, System.currentTimeMillis())
    }

    override suspend fun addDailyStreakBonus(userId: String, dayStart: Long, bonusExp: Long) {
        if (bonusExp <= 0L) return
        withContext(Dispatchers.IO) {
            val key = streakBonusKey(userId, dayStart)
            val current = configQueries.selectByKey(key).executeAsOneOrNull()?.value_json?.toLongOrNull() ?: 0L
            configQueries.upsert(key, (current + bonusExp).toString(), null, null, System.currentTimeMillis())
        }
    }

    private fun streakBonusKey(userId: String, dayStart: Long): String =
        "streak_bonus:$userId:$dayStart"

    private fun readDailyStreakBonus(userId: String, dayStart: Long): Long =
        configQueries.selectByKey(streakBonusKey(userId, dayStart)).executeAsOneOrNull()
            ?.value_json?.toLongOrNull() ?: 0L

    override fun observeDueCount(userId: String): Flow<Int> {
        return srsQueries.observeDueCount(userId, System.currentTimeMillis())
            .asFlow()
            .map { it.executeAsOne().toInt() }
            .distinctUntilChanged()
    }

    override fun observeDeckProgress(userId: String, deckId: Long): Flow<DeckProgress> {
        return srsQueries.observeByDeck(userId, deckId)
            .asFlow()
            .map { it.executeAsList().map { m -> m.toModel() } }
            .map { list -> calculateDeckProgress(list) }
            .distinctUntilChanged()
    }

    /**
     * EXP yang diperoleh dari satu log review, mengikuti rumus permainan aplikasi
     * (GamificationUseCase). Data bersumber dari review_log sinkron server, sehingga
     * jumlah per hari mencerminkan aktivitas belajar yang sesungguhnya.
     */
    private fun estimateExpEarned(log: ModelReviewLog): Long {
        if (log.isNew) return if (log.correctness) com.kotomichi.usecase.GamificationUseCase.EXP_LEARN_NEW_CARD else 0L
        if (!log.correctness) return 0L
        return when (log.rating) {
            Rating.EASY -> com.kotomichi.usecase.GamificationUseCase.EXP_REVIEW_EASY
            Rating.HARD -> com.kotomichi.usecase.GamificationUseCase.EXP_REVIEW_HARD
            else -> com.kotomichi.usecase.GamificationUseCase.EXP_REVIEW_CORRECT
        }
    }

    private fun startOfDay(timestamp: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun calculateDeckProgress(progressList: List<ModelSrsProgress>): DeckProgress {
        val totalVocab = progressList.size
        val learnedVocab = progressList.count { it.state != CardState.NEW }
        val reviewingVocab = progressList.count { it.state == CardState.REVIEW }
        val masteredVocab = progressList.count {
            com.kotomichi.fsrs.FsrsCalculator.calculateRetrievability(it) >= 0.9 && it.state == CardState.REVIEW
        }
        val averageRetrievability = if (progressList.isNotEmpty()) {
            progressList.sumOf { com.kotomichi.fsrs.FsrsCalculator.calculateRetrievability(it) } / progressList.size
        } else 0.0
        val masteryPercent = if (totalVocab > 0) masteredVocab.toDouble() / totalVocab * 100 else 0.0

        return DeckProgress(
            deckId = 0, // Will be set by caller
            totalVocab = totalVocab,
            learnedVocab = learnedVocab,
            reviewingVocab = reviewingVocab,
            masteredVocab = masteredVocab,
            averageRetrievability = averageRetrievability,
            masteryPercent = masteryPercent
        )
    }
}

internal fun SrsProgress.toModel(): ModelSrsProgress = ModelSrsProgress(
    userId = user_id,
    vocabularyId = vocabulary_id,
    direction = Direction.values()[direction.toInt()],
    stability = stability,
    difficulty = difficulty,
    retrievability = retrievability,
    dueAt = due_at,
    lastReviewAt = last_review_at,
    reviewCount = review_count.toInt(),
    lapses = lapses.toInt(),
    createdAt = created_at,
    updatedAt = updated_at
)

private fun ModelSrsProgress.toEntity(): SrsProgress = SrsProgress(
    user_id = userId,
    vocabulary_id = vocabularyId,
    direction = direction.ordinal.toLong(),
    stability = stability,
    difficulty = difficulty,
    retrievability = retrievability,
    due_at = dueAt,
    last_review_at = lastReviewAt,
    review_count = reviewCount.toLong(),
    lapses = lapses.toLong(),
    created_at = createdAt,
    updated_at = System.currentTimeMillis()
)

internal fun ReviewLog.toModel(): ModelReviewLog = ModelReviewLog(
    id = id,
    userId = user_id,
    vocabularyId = vocabulary_id,
    direction = Direction.values()[direction.toInt()],
    isNew = is_new == 1L,
    correctness = correctness == 1L,
    elapsedMs = elapsed_ms,
    rating = Rating.values()[rating.toInt()],
    stabilityBefore = stability_before,
    stabilityAfter = stability_after,
    difficultyBefore = difficulty_before,
    difficultyAfter = difficulty_after,
    retrievabilityBefore = retrievability_before,
    reviewedAt = reviewed_at
)

private fun UserProfile.toModel(): ModelUserProfile = ModelUserProfile(
    id = id,
    displayName = display_name,
    role = com.kotomichi.model.UserRole.entries.firstOrNull { it.name.equals(role, ignoreCase = true) } ?: com.kotomichi.model.UserRole.USER,
    preferredLocale = preferred_locale,
    level = level.toInt(),
    exp = exp.toInt(),
    lastReviewDate = last_review_date,
    currentStreak = current_streak.toInt(),
    longestStreak = longest_streak.toInt(),
    createdAt = created_at,
    updatedAt = updated_at,
    theme = theme,
    lastSeenAt = last_seen_at
)

// Keep the mapper available for callers that read profile locally.
fun ModelUserProfile.toEntity(): UserProfile = UserProfile(
    id = id,
    display_name = displayName,
    role = role.name,
    preferred_locale = preferredLocale,
    level = level.toLong(),
    exp = exp.toLong(),
    last_review_date = lastReviewDate,
    current_streak = currentStreak.toLong(),
    longest_streak = longestStreak.toLong(),
    created_at = createdAt,
    updated_at = updatedAt,
    theme = theme,
    last_seen_at = lastSeenAt
)