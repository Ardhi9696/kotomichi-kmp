package com.kotomichi.fsrs

import com.kotomichi.model.CardState
import com.kotomichi.model.Direction
import com.kotomichi.model.Rating
import com.kotomichi.model.SrsProgress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FsrsCalculatorTest {
    
    @Test
    fun `initial state for new card with Again rating`() {
        val progress = FsrsCalculator.createInitialProgress("user1", 1L, Direction.KANJI_TO_MEANING)
        val result = FsrsCalculator.calculateNextReview(progress, Rating.AGAIN)
        
        assertEquals(CardState.LEARNING, result.newState)
        assertTrue(result.newStability > 0)
        assertTrue(result.newDifficulty > 0)
        assertTrue(result.scheduledDays > 0)
    }
    
    @Test
    fun `initial state for new card with Good rating`() {
        val progress = FsrsCalculator.createInitialProgress("user1", 1L, Direction.KANJI_TO_MEANING)
        val result = FsrsCalculator.calculateNextReview(progress, Rating.GOOD)
        
        assertEquals(CardState.REVIEW, result.newState)
        assertTrue(result.newStability > 0)
        assertTrue(result.scheduledDays > 0)
    }
    
    @Test
    fun `initial state for new card with Easy rating`() {
        val progress = FsrsCalculator.createInitialProgress("user1", 1L, Direction.KANJI_TO_MEANING)
        val result = FsrsCalculator.calculateNextReview(progress, Rating.EASY)
        
        assertEquals(CardState.REVIEW, result.newState)
        assertTrue(result.newStability > 0)
        assertTrue(result.scheduledDays > 0)
    }
    
    @Test
    fun `learning state transitions`() {
        val progress = SrsProgress(
            userId = "user1",
            vocabularyId = 1L,
            direction = Direction.KANJI_TO_MEANING,
            stability = 0.5,
            difficulty = 5.0,
            elapsedDays = 0.0,
            scheduledDays = 0.0,
            reps = 1,
            lapses = 0,
            state = CardState.LEARNING,
            lastReview = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis()
        )
        
        // Again in learning -> stays in learning
        val resultAgain = FsrsCalculator.calculateNextReview(progress, Rating.AGAIN)
        assertEquals(CardState.LEARNING, resultAgain.newState)
        
        // Good in learning -> moves to review
        val resultGood = FsrsCalculator.calculateNextReview(progress, Rating.GOOD)
        assertEquals(CardState.REVIEW, resultGood.newState)
    }
    
    @Test
    fun `review state with Again rating goes to relearning`() {
        val progress = SrsProgress(
            userId = "user1",
            vocabularyId = 1L,
            direction = Direction.KANJI_TO_MEANING,
            stability = 10.0,
            difficulty = 5.0,
            elapsedDays = 5.0,
            scheduledDays = 9.0,
            reps = 5,
            lapses = 0,
            state = CardState.REVIEW,
            lastReview = System.currentTimeMillis() - (5 * 24 * 60 * 60 * 1000L),
            dueDate = System.currentTimeMillis()
        )
        
        val result = FsrsCalculator.calculateNextReview(progress, Rating.AGAIN)
        assertEquals(CardState.RELEARNING, result.newState)
        assertTrue(result.newLapses == 1)
    }
    
    @Test
    fun `retrievability calculation`() {
        // High stability, low elapsed -> high retrievability
        val retrievability1 = FsrsCalculator.calculateRetrievability(30.0, 1.0)
        assertTrue(retrievability1 > 0.9)
        
        // Low stability, high elapsed -> low retrievability
        val retrievability2 = FsrsCalculator.calculateRetrievability(1.0, 5.0)
        assertTrue(retrievability2 < 0.5)
    }
    
    @Test
    fun `direction unlock thresholds`() {
        assertEquals(0.0, FsrsCalculator.getDirectionUnlockThreshold(Direction.KANJI_TO_MEANING))
        assertEquals(7.0, FsrsCalculator.getDirectionUnlockThreshold(Direction.KANJI_TO_HIRAGANA))
        assertEquals(7.0, FsrsCalculator.getDirectionUnlockThreshold(Direction.HIRAGANA_TO_MEANING))
        assertEquals(7.0, FsrsCalculator.getDirectionUnlockThreshold(Direction.MEANING_TO_HIRAGANA))
        assertEquals(14.0, FsrsCalculator.getDirectionUnlockThreshold(Direction.HIRAGANA_TO_KANJI))
        assertEquals(21.0, FsrsCalculator.getDirectionUnlockThreshold(Direction.MEANING_TO_KANJI))
    }
    
    @Test
    fun `should unlock next direction`() {
        assertTrue(FsrsCalculator.shouldUnlockNextDirection(Direction.KANJI_TO_MEANING, 8.0))
        assertTrue(FsrsCalculator.shouldUnlockNextDirection(Direction.KANJI_TO_HIRAGANA, 8.0))
        assertTrue(FsrsCalculator.shouldUnlockNextDirection(Direction.HIRAGANA_TO_KANJI, 15.0))
        assertTrue(FsrsCalculator.shouldUnlockNextDirection(Direction.MEANING_TO_KANJI, 22.0))
        
        assertTrue(!FsrsCalculator.shouldUnlockNextDirection(Direction.KANJI_TO_MEANING, 5.0))
        assertTrue(!FsrsCalculator.shouldUnlockNextDirection(Direction.HIRAGANA_TO_KANJI, 10.0))
    }
    
    @Test
    fun `due date calculation`() {
        val progress = SrsProgress(
            userId = "user1",
            vocabularyId = 1L,
            direction = Direction.KANJI_TO_MEANING,
            stability = 10.0,
            difficulty = 5.0,
            elapsedDays = 0.0,
            scheduledDays = 9.0,
            reps = 5,
            lapses = 0,
            state = CardState.REVIEW,
            lastReview = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + (9 * 24 * 60 * 60 * 1000L)
        )
        
        assertTrue(FsrsCalculator.isDue(progress, progress.dueDate))
        assertTrue(!FsrsCalculator.isDue(progress, progress.dueDate - 1000))
    }
}