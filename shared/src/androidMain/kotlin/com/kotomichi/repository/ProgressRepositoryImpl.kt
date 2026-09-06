package com.kotomichi.repository

import com.kotomichi.db.KotomichiDatabase
import com.kotomichi.db.SrsProgress
import com.kotomichi.db.ReviewLog
import com.kotomichi.db.UserProfile
import com.kotomichi.db.AuthTokens
import com.kotomichi.db.DirectionThresholds
import com.kotomichi.db.SystemConfig
import com.kotomichi.db.DailyStats
import com.kotomichi.db.SyncMetadata
import com.kotomichi.model.SrsProgress as ModelSrsProgress
import com.kotomichi.model.ReviewLog as ModelReviewLog
import com.kotomichi.model.UserProfile as ModelUserProfile
import com.kotomichi.model.AuthTokens as ModelAuthTokens
import com.kotomichi.model.DirectionThresholds as ModelDirectionThresholds
import com.kotomichi.model.SystemConfig as ModelSystemConfig
import com.kotomichi.model.DailyStats as ModelDailyStats
import com.kotomichi.model.Direction
import com.kotomichi.model.CardState
import com.kotomichi.model.Rating
import com.kotomichi.model.SyncStatus
import com.kotomichi.model.DeckProgress
import com.kotomichi.model.UserStatistics
import com.kotomichi.model.HeatmapData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

class ProgressRepositoryImpl(
    private val database: KotomichiDatabase
) : ProgressRepository {
    
    private val srsQueries = database.srsProgressQueries
    private val reviewQueries = database.reviewLogQueries
    private val userQueries = database.userProfileQueries
    private val thresholdQueries = database.directionThresholdsQueries
    private val configQueries = database.systemConfigQueries
    private val dailyStatsQueries = database.dailyStatsQueries
    private val syncQueries = database.syncMetadataQueries
    
    private val _dueCount = MutableStateFlow(0)
    val dueCountFlow: Flow<Int> = _dueCount
    
    private val _deckProgressFlow = MutableStateFlow<Map<Long, DeckProgress>>(emptyMap())
    val deckProgressFlow: Flow<Map<Long, DeckProgress>> = _deckProgressFlow
    
    override suspend fun getProgress(userId: String, vocabularyId: Long, direction: Direction): ModelSrsProgress? = withContext(Dispatchers.IO) {
        srsQueries.selectByUserAndVocabAndDirection(userId, vocabularyId, direction.ordinal)?.toModel()
    }
    
    override suspend fun getAllProgress(userId: String): List<ModelSrsProgress> = withContext(Dispatchers.IO) {
        srsQueries.selectByUser(userId).map { it.toModel() }
    }
    
    override suspend fun getDueCards(userId: String, limit: Int): List<ModelSrsProgress> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        srsQueries.selectDueCards(userId, now, limit).map { it.toModel() }
    }
    
    override suspend fun getNewCards(userId: String, deckId: Long, limit: Int): List<ModelSrsProgress> = withContext(Dispatchers.IO) {
        srsQueries.selectNewCards(userId, deckId, limit).map { it.toModel() }
    }
    
    override suspend fun getLearnedVocabIds(userId: String): Set<Long> = withContext(Dispatchers.IO) {
        srsQueries.selectLearnedVocabIds(userId).toSet()
    }
    
    override suspend fun getProgressByDeck(userId: String, deckId: Long): List<ModelSrsProgress> = withContext(Dispatchers.IO) {
        srsQueries.selectByDeck(userId, deckId).map { it.toModel() }
    }
    
    override suspend fun insertProgress(progress: ModelSrsProgress) = withContext(Dispatchers.IO) {
        srsQueries.insert(progress.toEntity())
    }
    
    override suspend fun updateProgress(progress: ModelSrsProgress) = withContext(Dispatchers.IO) {
        srsQueries.update(progress.toEntity())
    }
    
    override suspend fun upsertProgress(progress: ModelSrsProgress) = withContext(Dispatchers.IO) {
        srsQueries.upsert(progress.toEntity())
    }
    
    override suspend fun deleteProgress(userId: String, vocabularyId: Long, direction: Direction) = withContext(Dispatchers.IO) {
        srsQueries.delete(userId, vocabularyId, direction.ordinal)
    }
    
    override suspend fun insertReviewLog(log: ModelReviewLog) = withContext(Dispatchers.IO) {
        reviewQueries.insertAndReturnId(
            userId = log.userId,
            vocabularyId = log.vocabularyId,
            direction = log.direction.ordinal,
            rating = log.rating.ordinal,
            responseTimeMs = log.responseTimeMs,
            stabilityBefore = log.stabilityBefore,
            difficultyBefore = log.difficultyBefore,
            stabilityAfter = log.stabilityAfter,
            difficultyAfter = log.difficultyAfter,
            reviewedAt = log.reviewedAt,
            syncStatus = log.syncStatus.value
        )
    }
    
    override suspend fun getReviewLogs(userId: String, limit: Int, offset: Int): List<ModelReviewLog> = withContext(Dispatchers.IO) {
        reviewQueries.selectByUser(userId, limit, offset).map { it.toModel() }
    }
    
    override suspend fun getReviewLogsByVocab(userId: String, vocabularyId: Long): List<ModelReviewLog> = withContext(Dispatchers.IO) {
        reviewQueries.selectByUserAndVocab(userId, vocabularyId).map { it.toModel() }
    }
    
    override suspend fun getUnsyncedReviewLogs(userId: String): List<ModelReviewLog> = withContext(Dispatchers.IO) {
        reviewQueries.selectUnsynced(userId).map { it.toModel() }
    }
    
    override suspend fun markReviewLogsSynced(logIds: List<Long>) = withContext(Dispatchers.IO) {
        reviewQueries.updateSyncStatus(logIds, SyncStatus.SYNCED.value)
    }
    
    override suspend fun getDeckProgress(userId: String, deckId: Long): DeckProgress = withContext(Dispatchers.IO) {
        val progressList = srsQueries.selectByDeck(userId, deckId).map { it.toModel() }
        calculateDeckProgress(progressList)
    }
    
    override suspend fun getDailyStats(userId: String, days: Int): List<ModelDailyStats> = withContext(Dispatchers.IO) {
        val endDate = System.currentTimeMillis()
        val startDate = endDate - (days * 24 * 60 * 60 * 1000L)
        dailyStatsQueries.selectByUser(userId, startDate, endDate).map { it.toModel() }
    }
    
    override suspend fun getUserStatistics(userId: String): UserStatistics = withContext(Dispatchers.IO) {
        val allProgress = srsQueries.selectByUser(userId).map { it.toModel() }
        val allLogs = reviewQueries.selectByUser(userId, Int.MAX_VALUE, 0).map { it.toModel() }
        
        val totalReviews = allLogs.size
        val totalCorrect = allLogs.count { it.rating != Rating.AGAIN }
        val totalStudyTime = allLogs.sumOf { it.responseTimeMs }
        val directionBreakdown = Direction.values().associateWith { direction ->
            val dirLogs = allLogs.filter { it.direction == direction }
            val dirProgress = allProgress.filter { it.direction == direction }
            DirectionStats(
                totalReviews = dirLogs.size,
                correctReviews = dirLogs.count { it.rating != Rating.AGAIN },
                averageResponseTimeMs = if (dirLogs.isNotEmpty()) dirLogs.averageOf { it.responseTimeMs.toDouble() }.toLong() else 0,
                averageStability = if (dirProgress.isNotEmpty()) dirProgress.averageOf { it.stability } else 0.0,
                cardsInReview = dirProgress.count { it.state == CardState.REVIEW }
            )
        }
        
        val profile = userQueries.selectById(userId)?.toModel()
        
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
    
    override fun observeDueCount(userId: String): Flow<Int> {
        return srsQueries.observeDueCount(userId, System.currentTimeMillis())
            .map { it.toInt() }
            .distinctUntilChanged()
    }
    
    override fun observeDeckProgress(userId: String, deckId: Long): Flow<DeckProgress> {
        return srsQueries.observeByDeck(userId, deckId)
            .map { list -> calculateDeckProgress(list.map { it.toModel() }) }
            .distinctUntilChanged()
    }
    
    private fun calculateDeckProgress(progressList: List<ModelSrsProgress>): DeckProgress {
        val totalVocab = progressList.size
        val learnedVocab = progressList.count { it.state != CardState.NEW }
        val reviewingVocab = progressList.count { it.state == CardState.REVIEW }
        val masteredVocab = progressList.count { 
            com.kotomichi.fsrs.FsrsCalculator.calculateRetrievability(it) >= 0.9 && it.state == CardState.REVIEW 
        }
        val averageRetrievability = if (progressList.isNotEmpty()) {
            progressList.averageOf { com.kotomichi.fsrs.FsrsCalculator.calculateRetrievability(it) }
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

private fun SrsProgress.toModel(): ModelSrsProgress = ModelSrsProgress(
    id = id,
    userId = user_id,
    vocabularyId = vocabulary_id,
    direction = Direction.values()[direction],
    stability = stability,
    difficulty = difficulty,
    elapsedDays = elapsed_days,
    scheduledDays = scheduled_days,
    reps = reps,
    lapses = lapses,
    state = CardState.values()[state],
    lastReview = last_review,
    dueDate = due_date,
    createdAt = created_at,
    updatedAt = updated_at
)

private fun ModelSrsProgress.toEntity(): SrsProgress = SrsProgress(
    id = id,
    user_id = userId,
    vocabulary_id = vocabularyId,
    direction = direction.ordinal,
    stability = stability,
    difficulty = difficulty,
    elapsed_days = elapsedDays,
    scheduled_days = scheduledDays,
    reps = reps,
    lapses = lapses,
    state = state.ordinal,
    last_review = lastReview,
    due_date = dueDate,
    created_at = createdAt,
    updated_at = System.currentTimeMillis()
)

private fun ReviewLog.toModel(): ModelReviewLog = ModelReviewLog(
    id = id,
    userId = user_id,
    vocabularyId = vocabulary_id,
    direction = Direction.values()[direction],
    rating = Rating.values()[rating],
    responseTimeMs = response_time_ms,
    stabilityBefore = stability_before,
    difficultyBefore = difficulty_before,
    stabilityAfter = stability_after,
    difficultyAfter = difficulty_after,
    reviewedAt = reviewed_at,
    syncStatus = SyncStatus.fromValue(sync_status)
)

private fun ModelReviewLog.toEntity(): ReviewLog = ReviewLog(
    id = id,
    user_id = userId,
    vocabulary_id = vocabularyId,
    direction = direction.ordinal,
    rating = rating.ordinal,
    response_time_ms = responseTimeMs,
    stability_before = stabilityBefore,
    difficulty_before = difficultyBefore,
    stability_after = stabilityAfter,
    difficulty_after = difficultyAfter,
    reviewed_at = reviewedAt,
    sync_status = syncStatus.value
)

private fun UserProfile.toModel(): ModelUserProfile = ModelUserProfile(
    id = id,
    email = email,
    name = name,
    role = com.kotomichi.model.UserRole.valueOf(role),
    totalExp = total_exp,
    currentLevel = current_level,
    currentStreak = current_streak,
    longestStreak = longest_streak,
    lastActiveDate = last_active_date,
    createdAt = created_at,
    updatedAt = updated_at
)

private fun ModelUserProfile.toEntity(): UserProfile = UserProfile(
    id = id,
    email = email,
    name = name,
    role = role.name,
    total_exp = totalExp,
    current_level = currentLevel,
    current_streak = currentStreak,
    longest_streak = longestStreak,
    last_active_date = lastActiveDate,
    created_at = createdAt,
    updated_at = System.currentTimeMillis()
)

private fun DailyStats.toModel(): ModelDailyStats = ModelDailyStats(
    date = date,
    learnCount = learn_count,
    reviewCount = review_count,
    correctCount = correct_count,
    totalTimeMs = total_time_ms,
    expEarned = exp_earned
)