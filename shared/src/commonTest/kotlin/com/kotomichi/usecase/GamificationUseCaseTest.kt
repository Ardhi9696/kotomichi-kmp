package com.kotomichi.usecase

import com.kotomichi.model.Rating
import com.kotomichi.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GamificationUseCaseTest {

    private class FakeProgressRepository : ProgressRepository {
        override suspend fun getConfigValue(key: String) = null
        override suspend fun setConfigValue(key: String, value: String) {}
        override suspend fun addDailyStreakBonus(userId: String, dayStart: Long, bonusExp: Long) {}

        override suspend fun getProgress(userId: String, vocabularyId: Long, direction: com.kotomichi.model.Direction) = null
        override suspend fun getAllProgress(userId: String) = emptyList<com.kotomichi.model.SrsProgress>()
        override suspend fun getDueCards(userId: String, limit: Int) = emptyList<com.kotomichi.model.SrsProgress>()
        override suspend fun getNewCards(userId: String, deckId: Long, limit: Int) = emptyList<com.kotomichi.model.SrsProgress>()
        override suspend fun getLearnedVocabIds(userId: String) = emptySet<Long>()
        override suspend fun getProgressByDeck(userId: String, deckId: Long) = emptyList<com.kotomichi.model.SrsProgress>()
        override suspend fun insertProgress(progress: com.kotomichi.model.SrsProgress) {}
        override suspend fun updateProgress(progress: com.kotomichi.model.SrsProgress) {}
        override suspend fun upsertProgress(progress: com.kotomichi.model.SrsProgress) {}
        override suspend fun deleteProgress(userId: String, vocabularyId: Long, direction: com.kotomichi.model.Direction) {}
        override suspend fun insertReviewLog(log: com.kotomichi.model.ReviewLog) {}
        override suspend fun getReviewLogs(userId: String, limit: Int, offset: Int) = emptyList<com.kotomichi.model.ReviewLog>()
        override suspend fun getReviewLogsByVocab(userId: String, vocabularyId: Long) = emptyList<com.kotomichi.model.ReviewLog>()
        override suspend fun getDeckProgress(userId: String, deckId: Long) = com.kotomichi.model.DeckProgress(
            deckId = deckId,
            totalVocab = 0,
            learnedVocab = 0,
            reviewingVocab = 0,
            masteredVocab = 0,
            averageRetrievability = 0.0,
            masteryPercent = 0.0
        )
        override suspend fun getDailyStats(userId: String, days: Int) = emptyList<com.kotomichi.model.DailyStats>()
        override suspend fun getUserStatistics(userId: String) = com.kotomichi.model.UserStatistics(
            totalVocabLearned = 0,
            totalReviews = 0,
            totalCorrect = 0,
            totalStudyTimeMs = 0L,
            currentStreak = 0,
            longestStreak = 0,
            totalExp = 0L,
            currentLevel = 1,
            averageAccuracy = 0.0,
            directionBreakdown = emptyMap()
        )
        override suspend fun getHeatmapData(userId: String, days: Int) = emptyList<com.kotomichi.model.HeatmapData>()
        override fun observeDueCount(userId: String): Flow<Int> = kotlinx.coroutines.flow.emptyFlow()
        override fun observeDeckProgress(userId: String, deckId: Long): Flow<com.kotomichi.model.DeckProgress> = kotlinx.coroutines.flow.emptyFlow()
    }

    private val gamificationUseCase = GamificationUseCase(FakeProgressRepository())
    
    @Test
    fun `EXP calculation for levels`() {
        // Level 1: 100 * 1^1.5 = 100
        assertEquals(100L, gamificationUseCase.calculateExpForLevel(1))
        
        // Level 2: 100 * 2^1.5 ≈ 282
        assertEquals(282L, gamificationUseCase.calculateExpForLevel(2))
        
        // Level 10: 100 * 10^1.5 ≈ 3162
        assertEquals(3162L, gamificationUseCase.calculateExpForLevel(10))
    }
    
    @Test
    fun `level from EXP calculation`() {
        // 0 EXP -> Level 1
        assertEquals(1, gamificationUseCase.calculateLevelFromExp(0))
        
        // 100 EXP -> Level 1 (need 282 for level 2)
        assertEquals(1, gamificationUseCase.calculateLevelFromExp(100))
        
        // 282 EXP -> Level 2
        assertEquals(2, gamificationUseCase.calculateLevelFromExp(282))
        
        // 10000 EXP -> higher level
        val level = gamificationUseCase.calculateLevelFromExp(10000)
        assertTrue(level > 5)
    }
    
    @Test
    fun `EXP progress percent`() {
        // Levels are cumulative thresholds: level 1 spans [exp(1), exp(2)) = [100, 282)
        val level1Exp = gamificationUseCase.calculateExpForLevel(1)
        val level2Exp = gamificationUseCase.calculateExpForLevel(2)

        // 100 EXP is exactly the level-1 start threshold -> 0% progress
        assertEquals(0.0, gamificationUseCase.calculateExpProgress(1, 100L), 0.01)

        // Mid level-1 (191 EXP): (191 - 100) / (282 - 100) * 100 = 50%
        val midLevel1Exp = (level1Exp + level2Exp) / 2
        val expectedMid = (midLevel1Exp - level1Exp).toDouble() / (level2Exp - level1Exp) * 100
        assertEquals(expectedMid, gamificationUseCase.calculateExpProgress(1, midLevel1Exp), 0.01)

        // Level 2: exp range [282, exp(3)) = [282, 519), 400 sits inside -> 0..100
        val progress2 = gamificationUseCase.calculateExpProgress(2, 400L)
        assertTrue(progress2 > 0 && progress2 < 100)
    }
    
@Test
    fun `learn EXP award`() = runTest {
        val exp = gamificationUseCase.awardLearnExp("user1")
        assertEquals(GamificationUseCase.EXP_LEARN_NEW_CARD, exp)
    }

    @Test
    fun `review EXP awards by rating`() = runTest {
        val expAgain = gamificationUseCase.awardReviewExp("user1", false, Rating.AGAIN)
        assertEquals(0L, expAgain)
        
        val expHard = gamificationUseCase.awardReviewExp("user1", true, Rating.HARD)
        assertEquals(GamificationUseCase.EXP_REVIEW_HARD, expHard)
        
        val expGood = gamificationUseCase.awardReviewExp("user1", true, Rating.GOOD)
        assertEquals(GamificationUseCase.EXP_REVIEW_CORRECT, expGood)
        
        val expEasy = gamificationUseCase.awardReviewExp("user1", true, Rating.EASY)
        assertEquals(GamificationUseCase.EXP_REVIEW_EASY, expEasy)
    }
    
    @Test
    fun `streak calculation`() {
        val now = System.currentTimeMillis()
        val yesterday = now - (24 * 60 * 60 * 1000L)
        val twoDaysAgo = now - (2 * 24 * 60 * 60 * 1000L)
        
        // Same day -> no change
        // (This would need a proper test with mocked time)
    }
}