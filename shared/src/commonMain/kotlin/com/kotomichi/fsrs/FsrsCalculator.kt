package com.kotomichi.fsrs

import com.kotomichi.model.Direction
import com.kotomichi.model.Rating
import com.kotomichi.model.SrsProgress
import com.kotomichi.model.CardState
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow

/**
 * Spaced repetition scheduling parameters.
 *
 * On Android the core scheduling (calculateNextReview) delegates to the official
 * java-fsrs library. These parameters are only applied by the pure-Kotlin
 * fallback algorithm used on iOS.
 */
data class FsrsParameters(
    val w: DoubleArray = FsrsCalculatorLegacy.DEFAULT_PARAMETERS
)

data class ReviewResult(
    val newStability: Double,
    val newDifficulty: Double,
    val newState: CardState,
    val scheduledDays: Double,
    val dueDate: Long
)

/**
 * Calculates the next scheduling state of an SRS card.
 *
 * - Android actual wraps the official FSRS scheduler (io.github.open-spaced-repetition:fsrs).
 * - iOS actual uses the pure-Kotlin fallback that preserves historical behavior.
 */
expect object FsrsCalculator {
    fun calculateNextReview(
        progress: SrsProgress,
        rating: Rating,
        parameters: FsrsParameters = FsrsParameters(),
        now: Long = System.currentTimeMillis()
    ): ReviewResult

    fun calculateRetrievability(stability: Double, elapsedDays: Double): Double

    fun calculateRetrievability(progress: SrsProgress, now: Long = System.currentTimeMillis()): Double

    fun isDue(progress: SrsProgress, now: Long = System.currentTimeMillis()): Boolean

    fun createInitialProgress(
        userId: String,
        vocabularyId: Long,
        direction: Direction,
        now: Long = System.currentTimeMillis()
    ): SrsProgress

    fun getDirectionUnlockThreshold(direction: Direction): Double

    fun shouldUnlockNextDirection(currentDirection: Direction, stability: Double): Boolean
}

/**
 * Pure-Kotlin reference implementation.
 *
 * Used directly by the iOS actual and for the retrievability / unlock helpers
 * shared by both platforms.
 */
internal object FsrsCalculatorLegacy {

    internal val DEFAULT_PARAMETERS = doubleArrayOf(
        0.40255, 1.18385, 3.173, 15.69105, 7.1949, 0.5345, 1.4604, 0.0046,
        1.54575, 0.1192, 1.01925, 1.9395, 0.11, 0.29605, 2.2698, 0.2315,
        2.9898, 0.34945, 0.50285, 0.6621
    )

    private const val MIN_DIFFICULTY = 1.0
    private const val MAX_DIFFICULTY = 10.0
    private const val MIN_STABILITY = 0.01

    internal fun calculateNextReview(
        progress: SrsProgress,
        rating: Rating,
        parameters: FsrsParameters,
        now: Long = System.currentTimeMillis()
    ): ReviewResult {
        val (stability, difficulty, state) = when (progress.state) {
            CardState.NEW -> initState(rating, parameters)
            CardState.LEARNING -> learningState(progress, rating, parameters)
            CardState.REVIEW -> reviewState(progress, rating, parameters)
            CardState.RELEARNING -> relearningState(progress, rating, parameters)
        }

        val scheduledDays = nextInterval(stability)
        val dueDate = now + (scheduledDays * 24 * 60 * 60 * 1000).toLong()

        return ReviewResult(
            newStability = stability,
            newDifficulty = difficulty,
            newState = state,
            scheduledDays = scheduledDays,
            dueDate = dueDate
        )
    }

    private fun initState(rating: Rating, parameters: FsrsParameters): Triple<Double, Double, CardState> {
        return when (rating) {
            Rating.AGAIN -> Triple(parameters.w[0], parameters.w[1], CardState.LEARNING)
            Rating.HARD -> Triple(parameters.w[2], parameters.w[3], CardState.LEARNING)
            Rating.GOOD -> Triple(parameters.w[4], parameters.w[5], CardState.REVIEW)
            Rating.EASY -> Triple(parameters.w[6], parameters.w[7], CardState.REVIEW)
        }
    }

    private fun learningState(
        progress: SrsProgress,
        rating: Rating,
        parameters: FsrsParameters
    ): Triple<Double, Double, CardState> {
        val difficulty = nextDifficulty(progress.difficulty, rating, parameters)

        return when (rating) {
            Rating.AGAIN -> Triple(parameters.w[0], difficulty, CardState.LEARNING)
            Rating.HARD -> Triple(parameters.w[2], difficulty, CardState.LEARNING)
            Rating.GOOD -> Triple(parameters.w[4], difficulty, CardState.REVIEW)
            Rating.EASY -> Triple(parameters.w[6], difficulty, CardState.REVIEW)
        }
    }

    private fun reviewState(
        progress: SrsProgress,
        rating: Rating,
        parameters: FsrsParameters
    ): Triple<Double, Double, CardState> {
        val difficulty = nextDifficulty(progress.difficulty, rating, parameters)
        val stability = nextStability(progress.stability, progress.difficulty, rating, parameters)
        val state = if (rating == Rating.AGAIN) CardState.RELEARNING else CardState.REVIEW

        return Triple(stability, difficulty, state)
    }

    private fun relearningState(
        progress: SrsProgress,
        rating: Rating,
        parameters: FsrsParameters
    ): Triple<Double, Double, CardState> {
        val difficulty = nextDifficulty(progress.difficulty, rating, parameters)

        return when (rating) {
            Rating.AGAIN -> Triple(parameters.w[0], difficulty, CardState.RELEARNING)
            Rating.HARD -> Triple(parameters.w[2], difficulty, CardState.RELEARNING)
            Rating.GOOD -> Triple(parameters.w[4], difficulty, CardState.REVIEW)
            Rating.EASY -> Triple(parameters.w[6], difficulty, CardState.REVIEW)
        }
    }

    private fun nextDifficulty(difficulty: Double, rating: Rating, parameters: FsrsParameters): Double {
        val w = parameters.w
        val nextDifficulty = difficulty - w[8] * (rating.fsrsValue - 3).toDouble() + w[9] * (rating.fsrsValue - 3).toDouble().pow(2)
        return nextDifficulty.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
    }

    private fun nextStability(
        stability: Double,
        difficulty: Double,
        rating: Rating,
        parameters: FsrsParameters
    ): Double {
        val w = parameters.w
        val retrievability = calculateRetrievability(stability, 0.0)

        return when (rating) {
            Rating.AGAIN -> w[10] * stability.pow(-w[11]) * difficulty.pow(-w[12]) * (1 - retrievability).pow(w[13])
            Rating.HARD -> w[14] * stability.pow(-w[15]) * difficulty.pow(-w[16]) * (1 - retrievability).pow(w[17])
            Rating.GOOD -> stability * (1 + w[18] * (1 - retrievability))
            Rating.EASY -> stability * (1 + w[19] * (1 - retrievability))
        }
    }

    private fun nextInterval(stability: Double): Double {
        return max(0.01, stability * 0.9)
    }

    internal fun calculateRetrievability(stability: Double, elapsedDays: Double): Double {
        if (stability <= 0) return 0.0
        return exp(-elapsedDays / stability)
    }

    internal fun calculateRetrievability(progress: SrsProgress, now: Long = System.currentTimeMillis()): Double {
        val elapsedDays = (now - progress.lastReview) / (24.0 * 60 * 60 * 1000)
        return calculateRetrievability(progress.stability, elapsedDays)
    }

    internal fun isDue(progress: SrsProgress, now: Long = System.currentTimeMillis()): Boolean {
        return now >= progress.dueDate
    }

    internal fun createInitialProgress(
        userId: String,
        vocabularyId: Long,
        direction: Direction,
        now: Long = System.currentTimeMillis()
    ): SrsProgress {
        return SrsProgress(
            userId = userId,
            vocabularyId = vocabularyId,
            direction = direction,
            stability = 0.0,
            difficulty = 0.0,
            dueAt = now,
            lastReviewAt = now,
            reviewCount = 0,
            lapses = 0
        )
    }

    internal fun getDirectionUnlockThreshold(direction: Direction): Double {
        return when (direction) {
            Direction.KANJI_TO_MEANING -> 0.0
            Direction.KANJI_TO_HIRAGANA -> 7.0
            Direction.HIRAGANA_TO_MEANING -> 7.0
            Direction.MEANING_TO_HIRAGANA -> 7.0
            Direction.HIRAGANA_TO_KANJI -> 14.0
            Direction.MEANING_TO_KANJI -> 21.0
        }
    }

    internal fun shouldUnlockNextDirection(currentDirection: Direction, stability: Double): Boolean {
        val threshold = getDirectionUnlockThreshold(currentDirection)
        return stability > threshold
    }
}