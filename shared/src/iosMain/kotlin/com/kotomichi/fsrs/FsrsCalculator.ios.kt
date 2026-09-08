package com.kotomichi.fsrs

import com.kotomichi.model.Direction
import com.kotomichi.model.Rating
import com.kotomichi.model.SrsProgress

/**
 * Fallback pure-Kotlin FSRS implementation for iOS.
 *
 * The official java-fsrs library is JVM-only, so iOS keeps the historical
 * algorithm while Android uses the official scheduler.
 */
actual object FsrsCalculator {

    actual fun calculateNextReview(
        progress: SrsProgress,
        rating: Rating,
        parameters: FsrsParameters,
        now: Long
    ): ReviewResult = FsrsCalculatorLegacy.calculateNextReview(progress, rating, parameters, now)

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