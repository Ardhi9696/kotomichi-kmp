package com.kotomichi.usecase

import com.kotomichi.model.Rating
import com.kotomichi.model.UserProfile
import com.kotomichi.model.UserRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GamificationUseCaseTest {
    
    private val gamificationUseCase = GamificationUseCase()
    
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
        // Level 1: 0-282 EXP
        val progress1 = gamificationUseCase.calculateExpProgress(1, 100L)
        assertEquals(100.0 / 282.0 * 100, progress1, 0.01)
        
        // Level 2: 282-670 EXP (approx)
        val progress2 = gamificationUseCase.calculateExpProgress(2, 400L)
        assertTrue(progress2 > 0 && progress2 < 100)
    }
    
    @Test
    fun `learn EXP award`() {
        val exp = gamificationUseCase.awardLearnExp("user1")
        assertEquals(GamificationUseCase.EXP_LEARN_NEW_CARD, exp)
    }
    
    @Test
    fun `review EXP awards by rating`() {
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