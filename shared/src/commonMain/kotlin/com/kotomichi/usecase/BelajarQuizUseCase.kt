package com.kotomichi.usecase

import com.kotomichi.fsrs.FsrsCalculator
import com.kotomichi.model.Direction
import com.kotomichi.model.Rating
import com.kotomichi.model.ReviewLog
import com.kotomichi.model.SrsProgress
import com.kotomichi.model.Vocabulary
import com.kotomichi.repository.AuthRepository
import com.kotomichi.repository.ProgressRepository
import com.kotomichi.repository.VocabRepository

/**
 * Mode belajar pada quick action "Belajar".
 *
 * NORMAL = recognition (3 arah termudah): Kanji → Arti, Kanji → Hiragana, Hiragana → Arti.
 * HARD   = production (3 arah tersulit):  Arti → Hiragana, Hiragana → Kanji, Arti → Kanji.
 */
enum class BelajarMode(val label: String, val description: String) {
    NORMAL("Normal", "Kanji → Arti · Kanji → Hiragana · Hiragana → Arti"),
    HARD("Sulit", "Arti → Hiragana · Hiragana → Kanji · Arti → Kanji");

    val directions: List<Direction>
        get() = Direction.entries.filter { (this == NORMAL) == (it.ordinal < 3) }
}

data class BelajarQuizResult(
    val total: Int,
    val correct: Int,
    val scorePercent: Double,
    val hardUnlockedNow: Boolean
) {
    val passed: Boolean get() = scorePercent >= BelajarQuizUseCase.HARD_UNLOCK_THRESHOLD_PERCENT
}

/**
 * Use case untuk kuis Belajar (MCQ): menyiapkan kosakata, memetakan hasil
 * jawaban ke rating FSRS, menyimpan progress, dan mengelola unlock mode
 * sulit per deck (nilai ≥90% pada mode normal).
 */
class BelajarQuizUseCase(
    private val vocabRepository: VocabRepository,
    private val progressRepository: ProgressRepository,
    private val authRepository: AuthRepository,
    private val gamificationUseCase: GamificationUseCase,
    private val fsrsCalculator: FsrsCalculator = FsrsCalculator
) {
    companion object {
        const val VOCAB_PER_SESSION = 5
        const val REVIEW_SESSION_SIZE = 10
        const val FAST_ANSWER_THRESHOLD_MS = 8_000L
        const val GOOD_ANSWER_THRESHOLD_MS = 15_000L
        const val HARD_UNLOCK_THRESHOLD_PERCENT = 90.0

        fun hardUnlockKey(userId: String, deckId: Long): String = "belajar_hard_unlocked:$userId:$deckId"
        private fun runAttemptKey(userId: String, deckId: Long, mode: BelajarMode): String =
            "belajar_attempts:$userId:$deckId:${mode.name}"
        private fun runAnsweredKey(userId: String, deckId: Long, mode: BelajarMode): String =
            "belajar_answered:$userId:$deckId:${mode.name}"
        private fun runQuestionsKey(userId: String, deckId: Long, mode: BelajarMode): String =
            "belajar_run_questions:$userId:$deckId:${mode.name}"
        private fun runDoneKey(userId: String, deckId: Long, mode: BelajarMode): String =
            "belajar_run_done:$userId:$deckId:${mode.name}"
    }

    /** Identitas satu soal dalam run (cukup untuk merekonstruksi urutan saat resume). */
    data class BelajarQuestionKey(val vocabId: Long, val direction: Direction)

    /** Urutan seluruh soal run ter-encode per sesi: "sesi1;sesi2;..." · tiap sesi "v:d,v:d,...". */
    private fun encodeQuestions(sessions: List<List<BelajarQuestionKey>>): String =
        sessions.joinToString(";") { sess ->
            sess.joinToString(",") { "${it.vocabId}:${it.direction.ordinal}" }
        }

    private fun decodeQuestions(raw: String?): List<List<BelajarQuestionKey>> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(";").map { sess ->
            sess.split(",").mapNotNull { token ->
                val parts = token.split(":")
                if (parts.size != 2) return@mapNotNull null
                val vocabId = parts[0].toLongOrNull() ?: return@mapNotNull null
                val dir = parts[1].toIntOrNull() ?: return@mapNotNull null
                val direction = Direction.entries.getOrNull(dir) ?: return@mapNotNull null
                BelajarQuestionKey(vocabId, direction)
            }
        }
    }

    /** Siapkan pool kosakata deck, utamakan kosakata baru lalu diacak. */
    suspend fun preparePool(userId: String, deckId: Long): List<Vocabulary> {
        val pool = vocabRepository.getVocabularyByDeck(deckId)
        if (pool.isEmpty()) return emptyList()
        val learnedIds = progressRepository.getLearnedVocabIds(userId)
        val newWords = pool.filter { it.id !in learnedIds }.shuffled()
        val rest = pool.filter { it.id in learnedIds }.shuffled()
        return newWords + rest
    }

    /**
     * Bagi pool deck menjadi sesi-sesi berisi [VOCAB_PER_SESSION] kosakata.
     * Setiap sesi melatih setiap kosakata pada 3 arah mode yang dipilih.
     */
    suspend fun prepareSessions(userId: String, deckId: Long): List<List<Vocabulary>> =
        preparePool(userId, deckId).chunked(VOCAB_PER_SESSION)

    /**
     * Sesi review global: kartu jatuh tempo dari semua deck (tanpa terikat
     * pada satu deck), diurutkan sesuai antrean due. Tiap kartu = 1 soal.
     */
    suspend fun prepareReviewSessions(userId: String): List<List<Pair<Vocabulary, Direction>>> {
        val due = progressRepository.getDueCards(userId, Int.MAX_VALUE)
        if (due.isEmpty()) return emptyList()
        val vocabById = vocabRepository
            .getVocabularyByIds(due.map { it.vocabularyId }.distinct())
            .associateBy { it.id }
        val items = due.mapNotNull { progress ->
            vocabById[progress.vocabularyId]?.let { it to progress.direction }
        }
        return items.chunked(REVIEW_SESSION_SIZE)
    }

    /**
     * Penilaian berdasar kecepatan menjawab:
     * benar < 8 dtk → EASY (cepat), benar 8–15 dtk → GOOD (baik),
     * benar > 15 dtk → HARD (lambat), salah → AGAIN.
     */
    fun mapRating(correct: Boolean, responseTimeMs: Long): Rating {
        if (!correct) return Rating.AGAIN
        return when {
            responseTimeMs < FAST_ANSWER_THRESHOLD_MS -> Rating.EASY
            responseTimeMs <= GOOD_ANSWER_THRESHOLD_MS -> Rating.GOOD
            else -> Rating.HARD
        }
    }

    /** Simpan jawaban kuis ke FSRS, catat ReviewLog, dan beri EXP/streak. */
    suspend fun submitAnswer(
        userId: String,
        vocabularyId: Long,
        direction: Direction,
        rating: Rating,
        responseTimeMs: Long
    ): SrsProgress {
        val stale = progressRepository.getProgress(userId, vocabularyId, direction)
        val wasNew = stale?.reviewCount == null || stale.reviewCount == 0

        var progress = stale ?: fsrsCalculator.createInitialProgress(userId, vocabularyId, direction)

        val stabilityBefore = progress.stability
        val difficultyBefore = progress.difficulty
        val retrievabilityBefore = progress.retrievability

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

        // FP: catat log review (sumber kalender/statistik harian) + EXP & streak.
        runCatching {
            progressRepository.insertReviewLog(
                ReviewLog(
                    userId = userId,
                    vocabularyId = vocabularyId,
                    direction = direction,
                    isNew = wasNew,
                    correctness = rating != Rating.AGAIN,
                    elapsedMs = responseTimeMs,
                    rating = rating,
                    stabilityBefore = stabilityBefore,
                    stabilityAfter = result.newStability,
                    difficultyBefore = difficultyBefore,
                    difficultyAfter = result.newDifficulty,
                    retrievabilityBefore = retrievabilityBefore,
                    reviewedAt = progress.lastReviewAt ?: System.currentTimeMillis()
                )
            )
            awardExpAndStreak(userId, wasNew, rating)
        }
        return progress
    }

    /**
     * EXP hanya naik saat mengerjakan Belajar/Review: kartu baru dijawab benar
     * +EXP, benar cepat (EASY) +kecil, benar lambat (HARD) sedang, benar baik
     * (GOOD) paling besar, salah (AGAIN) 0 untuk kartu baru maupun lama.
     * Streak & level diperbarui ke profil.
     */
    private suspend fun awardExpAndStreak(userId: String, wasNew: Boolean, rating: Rating) {
        val current = authRepository.getCurrentUser() ?: return
        val correct = rating != Rating.AGAIN
        val exp = if (wasNew) {
            // Kartu baru pun hanya dapat EXP jika dijawab benar.
            if (correct) gamificationUseCase.awardLearnExp(userId) else 0L
        } else {
            gamificationUseCase.awardReviewExp(userId, correct, rating)
        }
        val now = System.currentTimeMillis()
        val (newStreak, bonusExp) = gamificationUseCase.updateStreak(userId, current.lastActiveDate)
        val newTotalExp = current.totalExp + exp + bonusExp
        val updated = current.copy(
            exp = newTotalExp.toInt(),
            level = gamificationUseCase.calculateLevelFromExp(newTotalExp),
            lastReviewDate = now,
            currentStreak = newStreak,
            longestStreak = maxOf(current.longestStreak, newStreak),
            updatedAt = now
        )
        authRepository.publishProfile(updated)
    }

    /**
     * Catat hasil akhir satu sesi kuis. Mode normal dengan nilai ≥90%
     * membuka mode sulit untuk deck tersebut.
     */
    suspend fun recordQuizResult(
        userId: String,
        deckId: Long,
        mode: BelajarMode,
        correct: Int,
        total: Int
    ): BelajarQuizResult {
        val scorePercent = if (total > 0) correct.toDouble() / total * 100.0 else 0.0
        val passed = mode == BelajarMode.NORMAL && scorePercent >= HARD_UNLOCK_THRESHOLD_PERCENT
        val wasUnlocked = isHardUnlocked(userId, deckId)
        if (passed && !wasUnlocked) {
            setHardUnlocked(userId, deckId, true)
        }
        return BelajarQuizResult(
            total = total,
            correct = correct,
            scorePercent = scorePercent,
            hardUnlockedNow = passed && !wasUnlocked
        )
    }

    suspend fun isHardUnlocked(userId: String, deckId: Long): Boolean =
        progressRepository.getConfigValue(hardUnlockKey(userId, deckId))?.toBoolean() == true

    private suspend fun setHardUnlocked(userId: String, deckId: Long, unlocked: Boolean) {
        progressRepository.setConfigValue(hardUnlockKey(userId, deckId), unlocked.toString())
    }

    /**
     * Mulai run belajar baru untuk deck+mode: menyimpan urutan seluruh soal dan
     * mereset penghitung jawaban. Memakai nomor percobaan = jumlah run yang pernah
     * dimulai + 1. Dipanggil hanya saat belum ada run aktif (masuk pertama kali).
     */
    suspend fun startRun(
        userId: String,
        deckId: Long,
        mode: BelajarMode,
        questions: List<List<BelajarQuestionKey>>
    ): Int {
        val attempts = progressRepository.getConfigValue(runAttemptKey(userId, deckId, mode))?.toIntOrNull() ?: 0
        val nextAttempt = attempts + 1
        progressRepository.setConfigValue(runAttemptKey(userId, deckId, mode), nextAttempt.toString())
        progressRepository.setConfigValue(runAnsweredKey(userId, deckId, mode), "0")
        progressRepository.setConfigValue(runQuestionsKey(userId, deckId, mode), encodeQuestions(questions))
        progressRepository.setConfigValue(runDoneKey(userId, deckId, mode), "false")
        return nextAttempt
    }

    /**
     * Lanjutkan run yang masih berjalan: mengembalikan urutan soal tersimpan
     * beserta nomor percobaan dan jumlah soal yang sudah dijawab. Null bila
     * belum ada run aktif (masuk pertama) atau run sudah tuntas.
     */
    suspend fun resumeRun(userId: String, deckId: Long, mode: BelajarMode): BelajarResumeState? {
        val done = progressRepository.getConfigValue(runDoneKey(userId, deckId, mode))?.toBoolean() == true
        val answered = progressRepository.getConfigValue(runAnsweredKey(userId, deckId, mode))?.toIntOrNull() ?: 0
        val attempt = progressRepository.getConfigValue(runAttemptKey(userId, deckId, mode))?.toIntOrNull() ?: 0
        val sessions = decodeQuestions(progressRepository.getConfigValue(runQuestionsKey(userId, deckId, mode)))
        if (sessions.isEmpty() || done) return null
        return BelajarResumeState(attempt = attempt, answered = answered, sessions = sessions)
    }

    /** Tandai satu soal terjawab pada run aktif (naikkan penghitung). */
    suspend fun advanceRun(userId: String, deckId: Long, mode: BelajarMode) {
        val current = progressRepository.getConfigValue(runAnsweredKey(userId, deckId, mode))?.toIntOrNull() ?: 0
        progressRepository.setConfigValue(runAnsweredKey(userId, deckId, mode), (current + 1).toString())
    }

    /** Tandai run tuntas (seluruh soal dijawab) agar tidak dilanjutkan saat masuk berikutnya. */
    suspend fun completeRun(userId: String, deckId: Long, mode: BelajarMode) {
        progressRepository.setConfigValue(runDoneKey(userId, deckId, mode), "true")
    }

    /**
     * Progress run aktif per deck+mode: percobaan ke berapa, berapa soal sudah
     * dijawab dari total urutan tersimpan. Null bila run belum pernah dimulai.
     */
    suspend fun getRunProgress(userId: String, deckId: Long, mode: BelajarMode): BelajarRunProgress? {
        val answered = progressRepository.getConfigValue(runAnsweredKey(userId, deckId, mode))?.toIntOrNull() ?: return null
        val attempt = progressRepository.getConfigValue(runAttemptKey(userId, deckId, mode))?.toIntOrNull() ?: 0
        val totalQuestions = decodeQuestions(progressRepository.getConfigValue(runQuestionsKey(userId, deckId, mode)))
            .sumOf { it.size }
        if (totalQuestions == 0) return null
        return BelajarRunProgress(attempt = attempt, answered = answered, totalQuestions = totalQuestions)
    }
}

/** State run yang sedang berjalan: urutan seluruh soal + posisi jawaban. */
data class BelajarResumeState(
    val attempt: Int,
    val answered: Int,
    val sessions: List<List<BelajarQuizUseCase.BelajarQuestionKey>>
)

/** Progress run belajar satu deck+mode (dipakai kartu aksi cepat Belajar). */
data class BelajarRunProgress(
    val attempt: Int,
    val answered: Int,
    val totalQuestions: Int
) {
    val percent: Float
        get() = if (totalQuestions > 0) answered.toFloat() / totalQuestions else 0f

    val percentLabel: String
        get() = "${(percent * 100).toInt()}%"

    val attemptLabel: String
        get() = "Percobaan ke-$attempt"

    val runLabel: String
        get() = "$attemptLabel · $percentLabel"
}