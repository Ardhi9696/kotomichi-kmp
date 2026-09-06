package com.kotomichi.repository

import com.kotomichi.model.SrsProgress
import com.kotomichi.model.ReviewLog
import com.kotomichi.model.Direction
import com.kotomichi.model.CardState
import com.kotomichi.model.DeckProgress
import com.kotomichi.model.DailyStats
import com.kotomichi.model.UserStatistics
import com.kotomichi.model.HeatmapData
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    suspend fun getProgress(userId: String, vocabularyId: Long, direction: Direction): SrsProgress?
    suspend fun getAllProgress(userId: String): List<SrsProgress>
    suspend fun getDueCards(userId: String, limit: Int): List<SrsProgress>
    suspend fun getNewCards(userId: String, deckId: Long, limit: Int): List<SrsProgress>
    suspend fun getLearnedVocabIds(userId: String): Set<Long>
    suspend fun getProgressByDeck(userId: String, deckId: Long): List<SrsProgress>
    suspend fun insertProgress(progress: SrsProgress)
    suspend fun updateProgress(progress: SrsProgress)
    suspend fun upsertProgress(progress: SrsProgress)
    suspend fun deleteProgress(userId: String, vocabularyId: Long, direction: Direction)
    
    suspend fun insertReviewLog(log: ReviewLog)
    suspend fun getReviewLogs(userId: String, limit: Int, offset: Int): List<ReviewLog>
    suspend fun getReviewLogsByVocab(userId: String, vocabularyId: Long): List<ReviewLog>
    
    suspend fun getDeckProgress(userId: String, deckId: Long): DeckProgress
    suspend fun getDailyStats(userId: String, days: Int): List<DailyStats>
    suspend fun getUserStatistics(userId: String): UserStatistics
    suspend fun getHeatmapData(userId: String, days: Int): List<HeatmapData>
    
    fun observeDueCount(userId: String): Flow<Int>
    fun observeDeckProgress(userId: String, deckId: Long): Flow<DeckProgress>
}