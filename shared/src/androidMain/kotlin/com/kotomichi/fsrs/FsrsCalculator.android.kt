package com.kotomichi.fsrs

import com.kotomichi.model.CardState
import com.kotomichi.model.Direction
import com.kotomichi.model.Rating
import com.kotomichi.model.SrsProgress
import io.github.openspacedrepetition.Card
import io.github.openspacedrepetition.Scheduler
import io.github.openspacedrepetition.State
import java.time.Instant

/**
 * Official FSRS implementation (java-fsrs) used on Android.
 *
 * The legacy pure-Kotlin algorithm is still used for the retrievability and
 * unlock helpers so the deck progress metric stays consistent across platforms.
 */
actual object FsrsCalculator {

    private val scheduler: Scheduler by lazy { Scheduler.builder().build() }

    private const val DAY_MS = 24.0 * 60.0 * 60.0 * 1000.0

    actual fun calculateNextReview(
        progress: SrsProgress,
        rating: Rating,
        parameters: FsrsParameters,
        now: Long
    ): ReviewResult {
        val reviewedCard = scheduler.reviewCard(
            toCard(progress, now),
            rating.toFsrsRating(),
            Instant.ofEpochMilli(now)
        ).card()

        val newState = when (reviewedCard.getState()) {
            State.REVIEW -> CardState.REVIEW
            State.RELEARNING -> CardState.RELEARNING
            State.LEARNING -> CardState.LEARNING
        }
        val dueMs = reviewedCard.getDue().toEpochMilli().toDouble()

        return ReviewResult(
            newStability = reviewedCard.getStability() ?: 0.0,
            newDifficulty = reviewedCard.getDifficulty() ?: 0.0,
            newState = newState,
            scheduledDays = (dueMs - now) / DAY_MS,
            dueDate = reviewedCard.getDue().toEpochMilli()
        )
    }

    private fun toCard(progress: SrsProgress, now: Long): Card {
        return when {
            progress.reviewCount == 0 -> Card.builder().build()
            progress.lapses > 0 -> Card.builder()
                .state(State.RELEARNING)
                .step(0)
                .stability(progress.stability)
                .difficulty(progress.difficulty)
                .due(Instant.ofEpochMilli(progress.dueAt))
                .lastReview(progress.lastReviewAt?.let { Instant.ofEpochMilli(it) })
                .build()
            else -> Card.builder()
                .state(State.REVIEW)
                .stability(progress.stability)
                .difficulty(progress.difficulty)
                .due(Instant.ofEpochMilli(progress.dueAt))
                .lastReview(progress.lastReviewAt?.let { Instant.ofEpochMilli(it) })
                .build()
        }
    }

    private fun Rating.toFsrsRating(): io.github.openspacedrepetition.Rating =
        io.github.openspacedrepetition.Rating.values().first { it.getValue() == fsrsValue }

    actual fun calculateRetrievability(stability: Double, elapsedDays: Double): Double =
        FsrsCalculatorLegacy.calculateRetrievability(stability, elapsedDays)

    actual fun calculateRetrievability(progress: SrsProgress, now: Long): Double =
        FsrsCalculatorLegacy.calculateRetrievability(progress, now)

    actual fun isDue(progress: SrsProgress, now: Long): Boolean =
        FsrsCalculatorLegacy.isDue(progress, now)

    actual fun createInitialProgress(
        userId: String,
        vocabularyId: Long,
        direction: Direction,
        now: Long
    ): SrsProgress = FsrsCalculatorLegacy.createInitialProgress(userId, vocabularyId, direction, now)

    actual fun getDirectionUnlockThreshold(direction: Direction): Double =
        FsrsCalculatorLegacy.getDirectionUnlockThreshold(direction)

    actual fun shouldUnlockNextDirection(currentDirection: Direction, stability: Double): Boolean =
        FsrsCalculatorLegacy.shouldUnlockNextDirection(currentDirection, stability)
}