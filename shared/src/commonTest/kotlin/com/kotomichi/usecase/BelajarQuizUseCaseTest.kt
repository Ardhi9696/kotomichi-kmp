package com.kotomichi.usecase

import com.kotomichi.model.Direction
import com.kotomichi.model.Rating
import com.kotomichi.model.UserProfile
import com.kotomichi.model.Vocabulary
import com.kotomichi.model.VocabularyTranslation
import com.kotomichi.repository.AuthRepository
import com.kotomichi.repository.ProgressRepository
import com.kotomichi.repository.VocabRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BelajarQuizUseCaseTest {

    private class FakeProgressRepository : ProgressRepository {
        val config = mutableMapOf<String, String>()
        var learnedIds: Set<Long> = emptySet()
        var dueCards: List<com.kotomichi.model.SrsProgress> = emptyList()
        var storedProgress: com.kotomichi.model.SrsProgress? = null
        var userStats = com.kotomichi.model.UserStatistics(
            totalVocabLearned = 0, totalReviews = 0, totalCorrect = 0, totalStudyTimeMs = 0L,
            currentStreak = 0, longestStreak = 0, totalExp = 0L, currentLevel = 1,
            averageAccuracy = 0.0, directionBreakdown = emptyMap()
        )
        val reviewLogs = mutableListOf<com.kotomichi.model.ReviewLog>()

        override suspend fun getConfigValue(key: String): String? = config[key]
        override suspend fun setConfigValue(key: String, value: String) {
            config[key] = value
        }
        override suspend fun addDailyStreakBonus(userId: String, dayStart: Long, bonusExp: Long) {
            val key = "streak_bonus:$userId:$dayStart"
            val current = config[key]?.toLongOrNull() ?: 0L
            config[key] = (current + bonusExp).toString()
        }

        override suspend fun getProgress(userId: String, vocabularyId: Long, direction: Direction) = storedProgress
        override suspend fun getAllProgress(userId: String) = emptyList<com.kotomichi.model.SrsProgress>()
        override suspend fun getDueCards(userId: String, limit: Int): List<com.kotomichi.model.SrsProgress> = dueCards
        override suspend fun getNewCards(userId: String, deckId: Long, limit: Int) = emptyList<com.kotomichi.model.SrsProgress>()
        override suspend fun getLearnedVocabIds(userId: String): Set<Long> = learnedIds
        override suspend fun getProgressByDeck(userId: String, deckId: Long) = emptyList<com.kotomichi.model.SrsProgress>()
        override suspend fun insertProgress(progress: com.kotomichi.model.SrsProgress) {}
        override suspend fun updateProgress(progress: com.kotomichi.model.SrsProgress) {}
        override suspend fun upsertProgress(progress: com.kotomichi.model.SrsProgress) {}
        override suspend fun deleteProgress(userId: String, vocabularyId: Long, direction: Direction) {}
        override suspend fun insertReviewLog(log: com.kotomichi.model.ReviewLog) {
            reviewLogs += log
        }
        override suspend fun getReviewLogs(userId: String, limit: Int, offset: Int) = emptyList<com.kotomichi.model.ReviewLog>()
        override suspend fun getReviewLogsByVocab(userId: String, vocabularyId: Long) = emptyList<com.kotomichi.model.ReviewLog>()
        override suspend fun getDeckProgress(userId: String, deckId: Long) = com.kotomichi.model.DeckProgress(
            deckId = deckId, totalVocab = 0, learnedVocab = 0, reviewingVocab = 0,
            masteredVocab = 0, averageRetrievability = 0.0, masteryPercent = 0.0
        )
        override suspend fun getDailyStats(userId: String, days: Int) = emptyList<com.kotomichi.model.DailyStats>()
        override suspend fun getUserStatistics(userId: String) = userStats
        override suspend fun getHeatmapData(userId: String, days: Int) = emptyList<com.kotomichi.model.HeatmapData>()
        override fun observeDueCount(userId: String): Flow<Int> = emptyFlow()
        override fun observeDeckProgress(userId: String, deckId: Long): Flow<com.kotomichi.model.DeckProgress> = emptyFlow()
    }

    private class FakeAuthRepository : AuthRepository {
        var profile: UserProfile? = null
        var published: UserProfile? = null

        override val currentUser: Flow<UserProfile?> = emptyFlow()
        override val isAuthenticated: Flow<Boolean> = emptyFlow()
        override suspend fun getCurrentUser(): UserProfile? = profile
        override suspend fun publishProfile(profile: UserProfile) {
            published = profile
            this.profile = profile
        }
        override suspend fun login(request: com.kotomichi.model.LoginRequest) = throw NotImplementedError()
        override suspend fun register(request: com.kotomichi.model.RegisterRequest) = throw NotImplementedError()
        override suspend fun logout() = throw NotImplementedError()
        override suspend fun refreshToken() = throw NotImplementedError()
        override suspend fun resetPassword(request: com.kotomichi.model.ResetPasswordRequest) = throw NotImplementedError()
        override suspend fun updatePassword(request: com.kotomichi.model.UpdatePasswordRequest) = throw NotImplementedError()
        override suspend fun updateProfile(profile: UserProfile) = profile
        override suspend fun changeRole(userId: String, role: com.kotomichi.model.UserRole) = throw NotImplementedError()
        override suspend fun deleteUser(userId: String) = throw NotImplementedError()
        override suspend fun getAccessToken(): String? = null
        override suspend fun currentUserId(): String? = null
        override suspend fun syncRemoteProfile() = throw NotImplementedError()
        override fun observeCurrentUser(): Flow<UserProfile?> = emptyFlow()
    }

    private class FakeVocabRepository : VocabRepository {
        var byDeck: MutableMap<Long, List<Vocabulary>> = mutableMapOf()
        var byId: Map<Long, Vocabulary> = emptyMap()

        override suspend fun getVocabularyByDeck(deckId: Long): List<Vocabulary> = byDeck[deckId].orEmpty()
        override suspend fun getVocabularyById(id: Long) = byId[id]
        override suspend fun getVocabularyByIds(ids: List<Long>): List<Vocabulary> = ids.mapNotNull { byId[it] }
        override suspend fun getVocabIdsInDeck(deckId: Long) = emptyList<Long>()
        override suspend fun searchVocabulary(query: String, limit: Int) = emptyList<Vocabulary>()
        override suspend fun getAllVocabulary(limit: Int, offset: Int) = emptyList<Vocabulary>()
        override suspend fun insertVocabulary(vocab: Vocabulary) = 0L
        override suspend fun updateVocabulary(vocab: Vocabulary) {}
        override suspend fun deleteVocabulary(id: Long) {}
        override suspend fun linkVocabToDeck(deckVocab: com.kotomichi.model.DeckVocabulary) {}
        override suspend fun unlinkVocabFromDeck(deckId: Long, vocabId: Long) {}
        override fun observeVocabulary(vocabId: Long): Flow<Vocabulary?> = emptyFlow()
    }

    private val fakeRepo = FakeProgressRepository()
    private val fakeVocab = FakeVocabRepository()
    private val fakeAuth = FakeAuthRepository()
    private val useCase = BelajarQuizUseCase(
        vocabRepository = fakeVocab,
        progressRepository = fakeRepo,
        authRepository = fakeAuth,
        gamificationUseCase = GamificationUseCase(fakeRepo)
    )

    private fun vocab(id: Long) = Vocabulary(
        id = id,
        kanji = "漢$id",
        hiragana = "h$id",
        translations = listOf(VocabularyTranslation(vocabularyId = id, locale = "id", meaning = "arti$id"))
    )

    @Test
    fun `mode directions split by difficulty`() {
        assertEquals(
            listOf(Direction.KANJI_TO_MEANING, Direction.KANJI_TO_HIRAGANA, Direction.HIRAGANA_TO_MEANING),
            BelajarMode.NORMAL.directions
        )
        assertEquals(
            listOf(Direction.MEANING_TO_HIRAGANA, Direction.HIRAGANA_TO_KANJI, Direction.MEANING_TO_KANJI),
            BelajarMode.HARD.directions
        )
    }

    @Test
    fun `mapRating maps speed to four rating tiers`() {
        assertEquals(Rating.AGAIN, useCase.mapRating(correct = false, responseTimeMs = 1_000))
        assertEquals(Rating.EASY, useCase.mapRating(correct = true, responseTimeMs = 0))
        assertEquals(Rating.EASY, useCase.mapRating(correct = true, responseTimeMs = 7_999))
        assertEquals(Rating.GOOD, useCase.mapRating(correct = true, responseTimeMs = 8_000))
        assertEquals(Rating.GOOD, useCase.mapRating(correct = true, responseTimeMs = 15_000))
        assertEquals(Rating.HARD, useCase.mapRating(correct = true, responseTimeMs = 15_001))
    }

    @Test
    fun `prepareSessions chunks pool by session size`() = runTest {
        fakeRepo.learnedIds = (6L..12L).toSet()
        fakeVocab.byDeck[10L] = (1L..12L).map { vocab(it) }

        val sessions = useCase.prepareSessions("user1", 10L)

        assertEquals(listOf(5, 5, 2), sessions.map { it.size })
        assertEquals(12, sessions.flatten().size)
        assertTrue(sessions.first().all { it.id in 1L..5L })
    }

    @Test
    fun `prepareSessions returns empty for empty deck`() = runTest {
        assertEquals(emptyList(), useCase.prepareSessions("user1", 10L))
    }

    @Test
    fun `prepareReviewSessions builds global due sessions`() = runTest {
        val v1 = vocab(1L)
        val v2 = vocab(2L)
        val v3 = vocab(3L)
        val v4 = vocab(4L)
        val v5 = vocab(5L)
        fakeVocab.byId = (1L..5L).associateWith { vocab(it) }
        val dirs = listOf(Direction.KANJI_TO_MEANING, Direction.KANJI_TO_HIRAGANA, Direction.HIRAGANA_TO_MEANING)
        fakeRepo.dueCards = buildList {
            for (vocabId in 1L..4L) dirs.forEach { add(due(vocabId, it)) }
            add(due(5L, Direction.HIRAGANA_TO_MEANING))
        }

        val sessions = useCase.prepareReviewSessions("user1")

        assertEquals(listOf(10, 3), sessions.map { it.size })
        val first = sessions.flatten()
        assertTrue(first.any { it.first.id == v1.id && it.second == Direction.KANJI_TO_MEANING })
        assertTrue(first.any { it.first.id == v2.id && it.second == Direction.KANJI_TO_HIRAGANA })
        assertEquals(1, first.count { it.first.id == v5.id })
    }

    @Test
    fun `prepareReviewSessions returns empty if nothing due`() = runTest {
        fakeRepo.dueCards = emptyList()
        assertEquals(emptyList(), useCase.prepareReviewSessions("user1"))
    }

    @Test
    fun `startRun stores question order and progress is null before first run`() = runTest {
        fakeVocab.byDeck[10L] = (1L..4L).map { vocab(it) }

        assertNull(useCase.getRunProgress("user1", 10L, BelajarMode.NORMAL))
        assertNull(useCase.resumeRun("user1", 10L, BelajarMode.NORMAL))

        val questions = (1L..4L).map { id ->
            BelajarMode.NORMAL.directions.map { BelajarQuizUseCase.BelajarQuestionKey(id, it) }
        }
        assertEquals(1, useCase.startRun("user1", 10L, BelajarMode.NORMAL, questions))

        val run = useCase.getRunProgress("user1", 10L, BelajarMode.NORMAL)
        assertTrue(run != null)
        assertEquals(1, run!!.attempt)
        assertEquals(0, run.answered)
        assertEquals(12, run.totalQuestions) // 4 kosakata × 3 arah
        assertEquals(0f, run.percent)
    }

    @Test
    fun `advanceRun and resume reflect answered count`() = runTest {
        fakeVocab.byDeck[10L] = (1L..10L).map { vocab(it) }

        val questions = (1L..10L).map { id ->
            BelajarMode.NORMAL.directions.map { BelajarQuizUseCase.BelajarQuestionKey(id, it) }
        }
        useCase.startRun("user1", 10L, BelajarMode.NORMAL, questions)
        repeat(15) { useCase.advanceRun("user1", 10L, BelajarMode.NORMAL) }

        val run = useCase.getRunProgress("user1", 10L, BelajarMode.NORMAL)!!
        assertEquals(15, run.answered)
        assertEquals(30, run.totalQuestions) // 10 kosakata × 3 arah
        assertEquals(50, (run.percent * 100).toInt())
        assertEquals("50%", run.percentLabel)
        assertEquals("Percobaan ke-1 · 50%", run.runLabel)

        // resume kembali state dengan posisi yang sama
        val resumed = useCase.resumeRun("user1", 10L, BelajarMode.NORMAL)
        assertEquals(15, resumed!!.answered)
        assertEquals(10, resumed.sessions.size)

        // lanjut sisa soal lalu tuntas -> resume null setelahnya
        repeat(15) { useCase.advanceRun("user1", 10L, BelajarMode.NORMAL) }
        assertEquals(100, (useCase.getRunProgress("user1", 10L, BelajarMode.NORMAL)!!.percent * 100).toInt())
        useCase.completeRun("user1", 10L, BelajarMode.NORMAL)
        assertNull(useCase.resumeRun("user1", 10L, BelajarMode.NORMAL))
    }

    @Test
    fun `run progress is null before first run`() = runTest {
        fakeVocab.byDeck[10L] = (1L..4L).map { vocab(it) }
        assertNull(useCase.getRunProgress("user1", 10L, BelajarMode.NORMAL))
    }

    private fun due(vocabId: Long, direction: Direction) = com.kotomichi.model.SrsProgress(
        userId = "user1",
        vocabularyId = vocabId,
        direction = direction,
        dueAt = 0L
    )

    @Test
    fun `submitAnswer on new card records log and awards learn exp`() = runTest {
        fakeAuth.profile = UserProfile(id = "user1", exp = 0, level = 1, currentStreak = 0, longestStreak = 0)

        useCase.submitAnswer("user1", 1L, Direction.KANJI_TO_MEANING, Rating.EASY, 2_000)

        val log = fakeRepo.reviewLogs.single()
        assertTrue(log.isNew)
        assertTrue(log.correctness)
        assertEquals(Rating.EASY, log.rating)
        assertEquals(2_000, log.elapsedMs)
        assertEquals(Direction.KANJI_TO_MEANING, log.direction)

        val published = fakeAuth.published
        assertTrue(published != null)
        assertEquals(GamificationUseCase.EXP_LEARN_NEW_CARD, published!!.exp.toLong())
        assertEquals(1, published.level)
        assertEquals(1, published.currentStreak)
    }

    @Test
    fun `submitAnswer wrong on new card gives no learn exp`() = runTest {
        fakeAuth.profile = UserProfile(id = "user1", exp = 0, level = 1, currentStreak = 0, longestStreak = 0)

        useCase.submitAnswer("user1", 1L, Direction.KANJI_TO_MEANING, Rating.AGAIN, 30_000)

        val log = fakeRepo.reviewLogs.single()
        assertTrue(log.isNew)
        assertFalse(log.correctness)
        assertEquals(Rating.AGAIN, log.rating)
        assertEquals(0L, fakeAuth.published!!.totalExp)
    }

    @Test
    fun `submitAnswer on known card awards review exp by rating`() = runTest {
        fakeAuth.profile = UserProfile(id = "user1", exp = 10, level = 1, currentStreak = 1, longestStreak = 1)
        fakeRepo.storedProgress = com.kotomichi.model.SrsProgress(
            userId = "user1",
            vocabularyId = 1L,
            direction = Direction.KANJI_TO_MEANING,
            stability = 2.0,
            difficulty = 3.0,
            dueAt = 0L,
            reviewCount = 1
        )

        useCase.submitAnswer("user1", 1L, Direction.KANJI_TO_MEANING, Rating.GOOD, 10_000)

        val log = fakeRepo.reviewLogs.single()
        assertFalse(log.isNew)
        assertTrue(log.correctness)
        assertEquals(Rating.GOOD, log.rating)
        assertEquals(2.0, log.stabilityBefore)
        assertEquals(3.0, log.difficultyBefore)

        // exp lama 10 + EXP_REVIEW_CORRECT (5) = 15
        assertEquals(15L, fakeAuth.published!!.totalExp)
    }

    @Test
    fun `submitAnswer wrong on known card gives no exp`() = runTest {
        fakeAuth.profile = UserProfile(id = "user1", exp = 10, level = 1, currentStreak = 1, longestStreak = 1)
        fakeRepo.storedProgress = com.kotomichi.model.SrsProgress(
            userId = "user1",
            vocabularyId = 1L,
            direction = Direction.KANJI_TO_MEANING,
            stability = 2.0,
            difficulty = 3.0,
            dueAt = 0L,
            reviewCount = 1
        )

        useCase.submitAnswer("user1", 1L, Direction.KANJI_TO_MEANING, Rating.AGAIN, 30_000)

        val log = fakeRepo.reviewLogs.single()
        assertFalse(log.isNew)
        assertFalse(log.correctness)
        assertEquals(Rating.AGAIN, log.rating)
        assertEquals(10L, fakeAuth.published!!.totalExp)
    }

    @Test
    fun `normal quiz with passing score unlocks hard mode`() = runTest {
        val result = useCase.recordQuizResult("user1", 1L, BelajarMode.NORMAL, correct = 9, total = 10)

        assertEquals(90.0, result.scorePercent)
        assertTrue(result.passed)
        assertTrue(result.hardUnlockedNow)
        assertTrue(useCase.isHardUnlocked("user1", 1L))
    }

    @Test
    fun `normal quiz below threshold does not unlock`() = runTest {
        val result = useCase.recordQuizResult("user1", 2L, BelajarMode.NORMAL, correct = 8, total = 10)

        assertFalse(result.passed)
        assertFalse(result.hardUnlockedNow)
        assertFalse(useCase.isHardUnlocked("user1", 2L))
    }

    @Test
    fun `hard mode never unlocks hard flag`() = runTest {
        val result = useCase.recordQuizResult("user1", 3L, BelajarMode.HARD, correct = 10, total = 10)

        assertFalse(result.hardUnlockedNow)
        assertFalse(useCase.isHardUnlocked("user1", 3L))
    }

    @Test
    fun `unlock is per deck`() = runTest {
        fakeRepo.config[BelajarQuizUseCase.hardUnlockKey("user1", 1L)] = "true"
        assertTrue(useCase.isHardUnlocked("user1", 1L))
        assertFalse(useCase.isHardUnlocked("user1", 2L))
    }
}