package com.kotomichi.bkt

import com.kotomichi.model.Rating
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BktCalculatorTest {
    
    @Test
    fun `initial state creation`() {
        val state = BktCalculator.createInitialState("skill1", "user1")
        
        assertEquals("skill1", state.skillId)
        assertEquals("user1", state.userId)
        assertEquals(0.5, state.pKnown)
        assertEquals(0, state.attemptCount)
        assertEquals(0, state.correctCount)
    }
    
    @Test
    fun `correct answer increases pKnown`() {
        val state = BktCalculator.createInitialState("skill1", "user1")
        val result = BktCalculator.updateSkill(state, true)
        
        assertTrue(result.newPKnown > state.pKnown)
        assertTrue(result.predictedCorrect > 0.5)
    }
    
    @Test
    fun `incorrect answer decreases pKnown`() {
        val state = BktCalculator.createInitialState("skill1", "user1")
        val result = BktCalculator.updateSkill(state, false)
        
        assertTrue(result.newPKnown < state.pKnown)
        assertTrue(result.predictedCorrect < 0.5)
    }
    
    @Test
    fun `mastery threshold`() {
        // Create state with high pKnown
        val state = BktCalculator.createInitialState("skill1", "user1").copy(pKnown = 0.96)
        val result = BktCalculator.updateSkill(state, true)
        
        assertTrue(result.isMastered)
        assertTrue(result.newPKnown >= 0.95)
    }
    
    @Test
    fun `not mastered below threshold`() {
        // With default parameters a single correct answer from pKnown = 0.8 already
        // lifts newPKnown above the 0.95 mastery threshold (posterior 0.947 + transit).
        // 0.7 is the highest start that stays below threshold after one correct answer.
        val state = BktCalculator.createInitialState("skill1", "user1").copy(pKnown = 0.7)
        val result = BktCalculator.updateSkill(state, true)
        
        assertTrue(!result.isMastered)
        assertTrue(result.newPKnown < 0.95)
    }
    
    @Test
    fun `multiple correct answers approach mastery`() {
        var state = BktCalculator.createInitialState("skill1", "user1")
        
        repeat(10) {
            val result = BktCalculator.updateSkill(state, true)
            state = state.copy(pKnown = result.newPKnown, attemptCount = state.attemptCount + 1, correctCount = state.correctCount + 1)
        }
        
        assertTrue(state.pKnown > 0.9)
    }
    
    @Test
    fun `parameters affect learning rate`() {
        val fastLearnParams = BktParameters(pLearn = 0.5, pTransit = 0.3)
        val slowLearnParams = BktParameters(pLearn = 0.1, pTransit = 0.05)
        
        val state = BktCalculator.createInitialState("skill1", "user1")
        
        val fastResult = BktCalculator.updateSkill(state, true, fastLearnParams)
        val slowResult = BktCalculator.updateSkill(state, true, slowLearnParams)
        
        assertTrue(fastResult.newPKnown > slowResult.newPKnown)
    }
}