package com.kotomichi.bkt

import kotlinx.serialization.Serializable

@Serializable
data class BktParameters(
    val pLearn: Double = 0.3,
    val pGuess: Double = 0.2,
    val pSlip: Double = 0.1,
    val pTransit: Double = 0.15,
    val pKnown: Double = 0.5
)

@Serializable
data class BktState(
    val skillId: String,
    val userId: String,
    val pKnown: Double,
    val attemptCount: Int = 0,
    val correctCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
data class BktResult(
    val newPKnown: Double,
    val predictedCorrect: Double,
    val isMastered: Boolean
)

object BktCalculator {
    private const val MASTERY_THRESHOLD = 0.95
    
    fun updateSkill(
        state: BktState,
        isCorrect: Boolean,
        parameters: BktParameters = BktParameters()
    ): BktResult {
        val pKnownPrior = state.pKnown
        val pCorrectIfKnown = 1 - parameters.pSlip
        val pCorrectIfUnknown = parameters.pGuess
        
        val pCorrect = pKnownPrior * pCorrectIfKnown + (1 - pKnownPrior) * pCorrectIfUnknown
        
        val pKnownPosterior = if (isCorrect) {
            (pKnownPrior * pCorrectIfKnown) / pCorrect
        } else {
            (pKnownPrior * parameters.pSlip) / (1 - pCorrect)
        }
        
        val pKnownAfterTransit = pKnownPosterior + (1 - pKnownPosterior) * parameters.pTransit
        
        val newPKnown = pKnownAfterTransit.coerceIn(0.0, 1.0)
        val predictedCorrect = newPKnown * (1 - parameters.pSlip) + (1 - newPKnown) * parameters.pGuess
        val isMastered = newPKnown >= MASTERY_THRESHOLD
        
        return BktResult(
            newPKnown = newPKnown,
            predictedCorrect = predictedCorrect,
            isMastered = isMastered
        )
    }
    
    fun createInitialState(skillId: String, userId: String, parameters: BktParameters = BktParameters()): BktState {
        return BktState(
            skillId = skillId,
            userId = userId,
            pKnown = parameters.pKnown
        )
    }
}