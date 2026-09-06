package com.kotomichi.usecase

import com.kotomichi.model.Direction
import com.kotomichi.model.Rating
import com.kotomichi.model.SrsProgress
import com.kotomichi.model.Vocabulary
import com.kotomichi.model.Deck
import com.kotomichi.model.DeckProgress
import com.kotomichi.model.DailyStats
import com.kotomichi.model.UserStatistics
import com.kotomichi.model.HeatmapData
import com.kotomichi.repository.ProgressRepository
import com.kotomichi.repository.VocabRepository
import com.kotomichi.repository.DeckRepository
import com.kotomichi.fsrs.FsrsCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LearnCardUseCase(
    private val vocabRepository: VocabRepository,
    private val progressRepository: ProgressRepository,
    private val deckRepository: DeckRepository,
    private val fsrsCalculator: FsrsCalculator = FsrsCalculator
) {
    suspend fun getNewCardsForDeck(userId: String, deckId: Long, limit: Int = 20): List<Vocabulary> {
        val deckVocabIds = vocabRepository.getVocabIdsInDeck(deckId)
        val learnedVocabIds = progressRepository.getLearnedVocabIds(userId)
        val newVocabIds = deckVocabIds.filterNot { it in learnedVocabIds }.take(limit)
        return vocabRepository.getVocabularyByIds(newVocabIds)
    }
    
    suspend fun getNextNewCard(userId: String, deckId: Long): Vocabulary? {
        val newCards = getNewCardsForDeck(userId, deckId, 1)
        return newCards.firstOrNull()
    }
    
    suspend fun initializeProgressForNewCard(
        userId: String,
        vocabularyId: Long,
        direction: Direction
    ): SrsProgress {
        val progress = fsrsCalculator.createInitialProgress(userId, vocabularyId, direction)
        progressRepository.insertProgress(progress)
        return progress
    }
    
    suspend fun submitLearnAnswer(
        userId: String,
        vocabularyId: Long,
        direction: Direction,
        rating: Rating,
        responseTimeMs: Long
    ): SrsProgress {
        var progress = progressRepository.getProgress(userId, vocabularyId, direction)
            ?: fsrsCalculator.createInitialProgress(userId, vocabularyId, direction)
        
        val stabilityBefore = progress.stability
        val difficultyBefore = progress.difficulty
        
        val result = fsrsCalculator.calculateNextReview(progress, rating)
        
        progress = progress.copy(
            stability = result.newStability,
            difficulty = result.newDifficulty,
            retrievability = fsrsCalculator.calculateRetrievability(result.newStability, 0.0),
            dueAt = result.dueDate,
            lastReviewAt = System.currentTimeMillis(),
            reviewCount = progress.reviewCount + 1,
            lapses = if (rating == Rating.AGAIN) progress.lapses + 1 else progress.lapses,
            updatedAt = System.currentTimeMillis()
        )
        
        progressRepository.upsertProgress(progress)
        
        return progress
    }
    
    suspend fun checkDirectionUnlock(
        userId: String,
        vocabularyId: Long,
        currentDirection: Direction
    ): Boolean {
        val progress = progressRepository.getProgress(userId, vocabularyId, currentDirection)
        return progress?.let { fsrsCalculator.shouldUnlockNextDirection(currentDirection, it.stability) } ?: false
    }
    
    suspend fun getUnlockedDirections(userId: String, vocabularyId: Long): List<Direction> {
        val allProgress = progressRepository.getAllProgress(userId)
            .filter { it.vocabularyId == vocabularyId }
            .associateBy { it.direction }
        
        return Direction.values().filter { direction ->
            if (direction == Direction.KANJI_TO_MEANING) true
            else {
                val prevDirection = Direction.values()[direction.ordinal - 1]
                val prevProgress = allProgress[prevDirection]
                prevProgress?.let { fsrsCalculator.shouldUnlockNextDirection(prevDirection, it.stability) } ?: false
            }
        }
    }
}

class ReviewCardUseCase(
    private val progressRepository: ProgressRepository,
    private val fsrsCalculator: FsrsCalculator = FsrsCalculator
) {
    suspend fun getDueCards(userId: String, limit: Int = 50): List<SrsProgress> {
        return progressRepository.getDueCards(userId, limit)
    }
    
    suspend fun getDueCount(userId: String): Int {
        return progressRepository.getDueCards(userId, Int.MAX_VALUE).size
    }
    
    suspend fun submitReviewAnswer(
        userId: String,
        vocabularyId: Long,
        direction: Direction,
        rating: Rating,
        responseTimeMs: Long
    ): SrsProgress {
        val progress = progressRepository.getProgress(userId, vocabularyId, direction)
            ?: throw IllegalStateException("Progress not found for review")
        
        val stabilityBefore = progress.stability
        val difficultyBefore = progress.difficulty
        
        val result = fsrsCalculator.calculateNextReview(
            progress,
            rating
        )
        
        val newProgress = progress.copy(
            stability = result.newStability,
            difficulty = result.newDifficulty,
            retrievability = fsrsCalculator.calculateRetrievability(result.newStability, 0.0),
            dueAt = result.dueDate,
            lastReviewAt = System.currentTimeMillis(),
            reviewCount = progress.reviewCount + 1,
            lapses = if (rating == Rating.AGAIN) progress.lapses + 1 else progress.lapses,
            updatedAt = System.currentTimeMillis()
        )
        
        progressRepository.upsertProgress(newProgress)
        
        return newProgress
    }
    
    fun observeDueCount(userId: String): Flow<Int> {
        return progressRepository.observeDueCount(userId)
    }
}

class DeckProgressUseCase(
    private val deckRepository: DeckRepository,
    private val progressRepository: ProgressRepository,
    private val vocabRepository: VocabRepository
) {
    suspend fun getDeckProgress(userId: String, deckId: Long): DeckProgress {
        return progressRepository.getDeckProgress(userId, deckId)
    }
    
    suspend fun getAllDeckProgress(userId: String): List<DeckProgress> {
        val decks = deckRepository.getPublishedDecks()
        return decks.map { deck ->
            progressRepository.getDeckProgress(userId, deck.id)
        }
    }
    
    suspend fun checkDeckUnlocked(userId: String, deckId: Long): Boolean {
        val progress = progressRepository.getDeckProgress(userId, deckId)
        return progress.averageRetrievability >= 0.90
    }
    
    fun observeDeckProgress(userId: String, deckId: Long): Flow<DeckProgress> {
        return progressRepository.observeDeckProgress(userId, deckId)
    }
}

class StatisticsUseCase(
    private val progressRepository: ProgressRepository
) {
    suspend fun getDailyStats(userId: String, days: Int = 30): List<DailyStats> {
        return progressRepository.getDailyStats(userId, days)
    }
    
    suspend fun getUserStatistics(userId: String): UserStatistics {
        return progressRepository.getUserStatistics(userId)
    }
    
    suspend fun getHeatmapData(userId: String, days: Int = 365): List<HeatmapData> {
        return progressRepository.getHeatmapData(userId, days)
    }
}

class SyncDataUseCase(
    private val syncRepository: com.kotomichi.repository.SyncRepository
) {
    suspend fun performFullSync(): com.kotomichi.repository.SyncResult {
        return syncRepository.fullSync()
    }
    
    suspend fun pullUpdates(): com.kotomichi.repository.SyncResult {
        return syncRepository.pullMasterData()
    }
    
    suspend fun pushUpdates(): com.kotomichi.repository.SyncResult {
        return syncRepository.pushUserData()
    }
    
    fun observeSyncStatus(): Flow<com.kotomichi.repository.SyncStatus> {
        return syncRepository.observeSyncStatus()
    }
}