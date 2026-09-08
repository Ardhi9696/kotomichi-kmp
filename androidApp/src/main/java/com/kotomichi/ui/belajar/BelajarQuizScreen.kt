/**
 * File: BelajarQuizScreen.kt
 * Responsibility: Alur quiz Belajar per deck (MCQ) + komponen quiz bersama yang
 *                 dipakai ulang oleh ReviewQuizScreen.
 *                 1 sesi = 5 kosakata × 3 arah dari mode yang dipilih.
 *                 Jawaban benar → jeda 500 ms lalu lanjut otomatis; salah →
 *                 tunjukkan jawaban benar (hijau) + tombol "Lanjut" manual.
 *                 Penilaian berdasar kecepatan: <8 dtk Mudah, 8–15 dtk Baik,
 *                 >15 dtk Sulit, salah = Salah. Tiap masuk memulai run baru
 *                 (progress di-reset + percobaan dihitung); setiap jawaban
 *                 dicatat ke ReviewLog & EXP. Tiap sesi ada ringkasan, sesi
 *                 terakhir diakhiri ringkasan keseluruhan deck.
 */
package com.kotomichi.ui.belajar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kotomichi.di.get
import com.kotomichi.model.Direction
import com.kotomichi.model.Rating
import com.kotomichi.model.Vocabulary
import com.kotomichi.ui.common.getPlainAnswerText
import com.kotomichi.ui.common.getQuestionText
import com.kotomichi.ui.theme.KotomichiDimens
import com.kotomichi.ui.theme.KotomichiSpacing
import com.kotomichi.ui.theme.extendedColors
import com.kotomichi.usecase.BelajarMode
import com.kotomichi.usecase.BelajarQuizResult
import com.kotomichi.usecase.BelajarQuizUseCase
import kotlin.math.round
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val AUTO_ADVANCE_DELAY_MS = 500L

/** Satu soal kuis: kosakata, arah tanya, prompt, dan daftar pilihan (tanpa hint). */
data class BelajarQuizItem(
    val vocab: Vocabulary,
    val direction: Direction,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int
)

/** Satu hasil jawaban soal (dipakai untuk rincian & rata-rata per arah di ringkasan). */
data class QuizResultDetail(
    val vocabText: String,
    val direction: Direction,
    val elapsedMs: Long,
    val rating: Rating,
    val isCorrect: Boolean
)

/** Statistik kuis yang diakumulasi per sesi maupun keseluruhan. */
data class QuizSessionStats(
    val total: Int = 0,
    val correct: Int = 0,
    val fast: Int = 0,
    val good: Int = 0,
    val slow: Int = 0,
    val wrong: Int = 0,
    val totalTimeMs: Long = 0L,
    val details: List<QuizResultDetail> = emptyList()
) {
    val scorePercent: Double get() = if (total == 0) 0.0 else correct.toDouble() * 100.0 / total

    fun record(detail: QuizResultDetail): QuizSessionStats {
        val r = detail.rating
        return copy(
            total = total + 1,
            correct = correct + if (detail.isCorrect) 1 else 0,
            fast = fast + if (detail.isCorrect && r == Rating.EASY) 1 else 0,
            good = good + if (detail.isCorrect && r == Rating.GOOD) 1 else 0,
            slow = slow + if (detail.isCorrect && r == Rating.HARD) 1 else 0,
            wrong = wrong + if (!detail.isCorrect) 1 else 0,
            totalTimeMs = totalTimeMs + detail.elapsedMs,
            details = details + detail
        )
    }

    fun plus(other: QuizSessionStats): QuizSessionStats = QuizSessionStats(
        total = total + other.total,
        correct = correct + other.correct,
        fast = fast + other.fast,
        good = good + other.good,
        slow = slow + other.slow,
        wrong = wrong + other.wrong,
        totalTimeMs = totalTimeMs + other.totalTimeMs,
        details = details + other.details
    )

    /** Semua arah yang muncul pada statistik ini, urut sesuai kuis. */
    fun presentDirections(): List<Direction> = details.map { it.direction }.distinct()

    /** Rata-rata waktu (detik) untuk satu arah; null bila arah belum muncul. */
    fun averageSecondsPerDirection(direction: Direction): Double? {
        val items = details.filter { it.direction == direction }
        if (items.isEmpty()) return null
        return items.sumOf { it.elapsedMs } / items.size.toDouble() / 1000.0
    }
}

/** Hasil satu sesi: statistik + hasil unlock mode sulit. */
data class QuizSessionOutcome(
    val stats: QuizSessionStats,
    val result: BelajarQuizResult
)

/**
 * Bangun soal kuis satu sesi belajar: tiap kosakata dilatih pada 3 arah mode,
 * lalu seluruh soal (kosakata × arah) diacak agar tidak mudah ditebak —
 * kosakata yang sama tidak dilatih berurutan.
 */
internal fun buildBelajarQuestions(
    questions: List<Vocabulary>,
    distractorPool: List<Vocabulary>,
    mode: BelajarMode,
    random: Random = Random.Default
): List<BelajarQuizItem> {
    val items = mutableListOf<BelajarQuizItem>()
    for (vocab in questions) {
        for (direction in mode.directions) {
            items += buildQuizItem(vocab, direction, distractorPool, random)
        }
    }
    return noAdjacentSameVocab(items, random)
}

/**
 * Bangun soal kuis review: 1 kartu (kosakata + arah) menghasilkan 1 soal.
 * Pilihan jawaban diambil dari deck asal kosakata; bila kurang dari 3
 * distractor, fallback ke seluruh kartu due pada sesi. Urutan dipertahankan
 * sesuai antrean due (kurva lupa Ebbinghaus).
 */
internal fun buildReviewQuestions(
    dueCards: List<Pair<Vocabulary, Direction>>,
    deckVocabMap: Map<Long, List<Vocabulary>>,
    random: Random = Random.Default
): List<BelajarQuizItem> =
    dueCards.map { (vocab, direction) ->
        val deckPool = deckVocabMap[vocab.id].orEmpty()
        val fallbackPool = dueCards.map { it.first }
        buildQuizItem(vocab, direction, deckPool, random, fallbackPool)
    }

/**
 * Acak daftar soal lalu perbaiki agar dua soal berurutan tidak memakai
 * kosakata yang sama (mencegah tebak-tiruan urutan 3 arah).
 */
private fun noAdjacentSameVocab(
    items: List<BelajarQuizItem>,
    random: Random
): List<BelajarQuizItem> {
    if (items.size < 3) return items
    val result = items.shuffled(random).toMutableList()
    var pass = 0
    while (pass < items.size) {
        var repositioned = false
        for (i in 1 until result.size) {
            if (result[i].vocab.id == result[i - 1].vocab.id) {
                val j = (i + 1 until result.size)
                    .firstOrNull { result[it].vocab.id != result[i].vocab.id && result[it].vocab.id != result[i - 1].vocab.id }
                if (j != null) {
                    val tmp = result[i]
                    result[i] = result[j]
                    result[j] = tmp
                    repositioned = true
                }
            }
        }
        if (!repositioned) break
        pass++
    }
    return result
}

/**
 * Bangun satu item kuis MCQ: prompt + 4 pilihan jawaban (selalu 4).
 * Jika distractorPool kurang dari 3, ambil dari fallbackPool.
 */
private fun buildQuizItem(
    vocab: Vocabulary,
    direction: Direction,
    distractorPool: List<Vocabulary>,
    random: Random = Random.Default,
    fallbackPool: List<Vocabulary> = emptyList()
): BelajarQuizItem {
    val correct = getPlainAnswerText(vocab, direction)
    val distractors = mutableListOf<String>()

    fun addFromPool(pool: List<Vocabulary>) {
        for (candidate in pool.shuffled(random)) {
            if (candidate.id == vocab.id) continue
            val text = getPlainAnswerText(candidate, direction)
            if (text.isNotBlank() && text != correct && text !in distractors) {
                distractors += text
            }
            if (distractors.size >= 3) return
        }
    }

    addFromPool(distractorPool)
    if (distractors.size < 3) addFromPool(fallbackPool)

    val options = (listOf(correct) + distractors).shuffled(random)
    return BelajarQuizItem(
        vocab = vocab,
        direction = direction,
        prompt = getQuestionText(vocab, direction),
        options = options,
        correctIndex = options.indexOf(correct)
    )
}

/**
 * Bangun ulang satu item kuis dari key (vocab + arah) yang tersimpan, memakai
 * distractor dari seluruh pool deck. Dipakai saat resume run yang sudah berjalan.
 */
private fun buildBelajarItemFromKey(
    vocab: Vocabulary,
    direction: Direction,
    distractorPool: List<Vocabulary>,
    random: Random = Random.Default
): BelajarQuizItem? {
    val correct = getPlainAnswerText(vocab, direction)
    if (correct.isBlank()) return null
    return buildQuizItem(vocab, direction, distractorPool, random)
}

/**
 * Layar kuis Belajar untuk sebuah deck. Beberapa sesi berurutan; tiap sesi
 * 5 kosakata × 3 arah. Run yang belum selesai dilanjutkan dari posisi terakhir
 * (progress tersimpan per jawaban); hasil tiap sesi diakumulasi ke statistik harian.
 * @param paddingValues Padding dari parent
 * @param userId ID user aktif
 * @param deck Deck yang dikuis
 * @param mode Mode awal (Normal/Sulit)
 * @param onExit Callback keluar dari layar kuis
 */
@Composable
fun BelajarQuizScreen(
    paddingValues: PaddingValues,
    userId: String,
    deck: com.kotomichi.model.Deck,
    mode: BelajarMode,
    onExit: () -> Unit
) {
    val belajarUseCase: BelajarQuizUseCase = get()

    var sessions by remember { mutableStateOf<List<List<BelajarQuizItem>>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var attempt by remember(deck.id, mode) { mutableIntStateOf(0) }
    var startSessionIndex by remember(deck.id, mode) { mutableIntStateOf(0) }
    var startItemIndex by remember(deck.id, mode) { mutableIntStateOf(0) }
    var ready by remember { mutableStateOf(false) }

    LaunchedEffect(deck.id, userId, mode) {
        loading = true
        loadError = null
        ready = false
        try {
            val vocabSessions = belajarUseCase.prepareSessions(userId, deck.id)
            if (vocabSessions.isNotEmpty()) {
                val pool = vocabSessions.flatten()
                // Lanjutkan run yang masih berjalan bila ada; jika tidak, mulai run baru.
                val resumed = belajarUseCase.resumeRun(userId, deck.id, mode)
                if (resumed != null) {
                    attempt = resumed.attempt
                    sessions = resumed.sessions.map { sess ->
                        sess.mapNotNull { key ->
                            val vocab = pool.firstOrNull { it.id == key.vocabId } ?: return@mapNotNull null
                            buildBelajarItemFromKey(vocab, key.direction, pool)
                        }
                    }.filter { it.isNotEmpty() }
                    // Posisi lanjut: cari sesi & indeks item dari jumlah yang sudah dijawab.
                    val sess = sessions!!
                    var remaining = resumed.answered
                    var si = 0
                    var ii = 0
                    for ((idx, s) in sess.withIndex()) {
                        if (remaining < s.size) { si = idx; ii = remaining; break }
                        remaining -= s.size
                        si = idx + 1
                        ii = 0
                    }
                    startSessionIndex = si.coerceAtMost(sess.lastIndex)
                    startItemIndex = ii
                } else {
                    val built = vocabSessions.map { buildBelajarQuestions(it, pool, mode) }
                    attempt = belajarUseCase.startRun(
                        userId, deck.id, mode,
                        built.map { sess -> sess.map { BelajarQuizUseCase.BelajarQuestionKey(it.vocab.id, it.direction) } }
                    )
                    sessions = built
                }
            } else {
                sessions = emptyList()
            }
        } catch (e: Exception) {
            loadError = e.message ?: "Gagal memuat kosakata"
            sessions = emptyList()
        } finally {
            loading = false
            ready = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = KotomichiSpacing.lg, vertical = KotomichiSpacing.md),
        verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.md)
    ) {
        when {
            loading -> LoadingRow(text = "Memuat soal…")

            loadError != null -> {
                Text(
                    text = loadError ?: "Terjadi kesalahan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                OutlinedButton(onClick = onExit) { Text("Keluar") }
            }

            !ready || sessions.isNullOrEmpty() -> {
                Text(
                    text = "Deck ini belum memiliki kosakata yang cukup untuk kuis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = onExit) { Text("Keluar") }
            }

            else -> QuizSessionFlow(
                sessions = sessions!!,
                sessionNoun = "Sesi",
                userId = userId,
                chipText = "Mode ${mode.label}",
                mode = mode,
                extraChip = if (attempt > 0) "Percobaan ke-$attempt" else null,
                startSessionIndex = startSessionIndex,
                startItemIndex = startItemIndex,
                onEverythingComplete = { },
                onCompleteRun = {
                    runCatchingSuspend { belajarUseCase.completeRun(userId, deck.id, mode) }.getOrElse { }
                },
                onAnswer = {
                    runCatchingSuspend { belajarUseCase.advanceRun(userId, deck.id, mode) }.getOrElse { }
                },
                finalizeSession = { stats ->
                    runCatchingSuspend {
                        belajarUseCase.recordQuizResult(userId, deck.id, mode, stats.correct, stats.total)
                    }.getOrElse { BelajarQuizResult(stats.total, stats.correct, stats.scorePercent, false) }
                        .let { QuizSessionOutcome(stats, it) }
                },
                onExit = onExit
            )
        }
    }
}

internal suspend fun <T> runCatchingSuspend(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: Exception) {
        Result.failure(e)
    }

/**
 * Mesin alur kuis generik: sesi-sesi soal MCQ → body → ringkasan sesi →
 * ringkasan keseluruhan. Dipakai bersama BelajarQuizScreen dan ReviewQuizScreen.
 */
@Composable
internal fun QuizSessionFlow(
    sessions: List<List<BelajarQuizItem>>,
    sessionNoun: String,
    userId: String,
    chipText: String,
    mode: BelajarMode?,
    extraChip: String? = null,
    startSessionIndex: Int = 0,
    startItemIndex: Int = 0,
    onEverythingComplete: () -> Unit,
    onCompleteRun: suspend () -> Unit = {},
    onAnswer: suspend () -> Unit = {},
    finalizeSession: suspend (QuizSessionStats) -> QuizSessionOutcome,
    onExit: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var sessionIndex by rememberSaveable { mutableIntStateOf(startSessionIndex.coerceIn(0, (sessions.size - 1).coerceAtLeast(0))) }
    var completedSessions by remember { mutableStateOf<List<QuizSessionStats>>(emptyList()) }
    var currentOutcome by remember { mutableStateOf<QuizSessionOutcome?>(null) }
    var showOverall by rememberSaveable { mutableStateOf(false) }

    if (sessions.isEmpty()) {
        Text("Tidak ada soal.", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = onExit) { Text("Keluar") }
        return
    }

    val overall = completedSessions.fold(QuizSessionStats()) { acc, s -> acc.plus(s) }

    Column(verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.md)) {
        when {
            showOverall -> OverallSummary(
                stats = overall,
                sessionNoun = sessionNoun,
                mode = mode,
                onExit = onExit
            )

            currentOutcome != null -> SessionSummary(
                sessionNumber = sessionIndex + 1,
                sessionNoun = sessionNoun,
                outcome = currentOutcome!!,
                mode = mode,
                isLastSession = sessionIndex == sessions.lastIndex,
                onNextSession = {
                    currentOutcome = null
                    sessionIndex++
                },
                onShowOverall = {
                    showOverall = true
                    onEverythingComplete()
                    scope.launch { onCompleteRun() }
                },
                onExit = onExit
            )

            else -> {
                val items = sessions.getOrNull(sessionIndex)
                if (items.isNullOrEmpty()) {
                    OverallSummary(stats = overall, sessionNoun = sessionNoun, mode = mode, onExit = onExit)
                } else {
                    key(sessionIndex) {
                        BelajarSessionBody(
                            userId = userId,
                            items = items,
                            chipText = chipText,
                            extraChip = extraChip,
                            startIndex = if (sessionIndex == startSessionIndex) startItemIndex else 0,
                            onAnswer = onAnswer,
                            onSessionComplete = { stats ->
                                scope.launch {
                                    val outcome = finalizeSession(stats)
                                    completedSessions = completedSessions + stats
                                    currentOutcome = outcome
                                    if (sessionIndex == sessions.lastIndex) {
                                        onCompleteRun()
                                    }
                                }
                            },
                            onExit = onExit
                        )
                    }
                }
            }
        }
    }
}

/**
 * Body satu sesi kuis: siklus soal → umpan balik → lapor hasil.
 * Di-reset penuh (key) setiap pindah sesi.
 */
@Composable
internal fun BelajarSessionBody(
    userId: String,
    items: List<BelajarQuizItem>,
    chipText: String,
    extraChip: String? = null,
    startIndex: Int = 0,
    onAnswer: suspend () -> Unit = {},
    onSessionComplete: (QuizSessionStats) -> Unit,
    onExit: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val belajarUseCase: BelajarQuizUseCase = get()

    var index by remember { mutableIntStateOf(startIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0))) }
    var stats by remember { mutableStateOf(QuizSessionStats()) }
    var finished by remember { mutableStateOf(false) }

    if (items.isEmpty()) {
        Text("Tidak ada soal.", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = onExit) { Text("Keluar") }
        return
    }

    if (finished) {
        LoadingRow(text = "Menyimpan hasil…")
        return
    }

    fun advanceOrFinish() {
        if (index == items.lastIndex) {
            finished = true
            onSessionComplete(stats)
        } else {
            index++
        }
    }

    val current = items[index]
    var selected by remember(current) { mutableStateOf<Int?>(null) }
    var questionStart by remember(current) { mutableStateOf(System.currentTimeMillis()) }

    fun onAnswered(answerIndex: Int) {
        val elapsed = System.currentTimeMillis() - questionStart
        val correct = answerIndex == current.correctIndex
        val rating = belajarUseCase.mapRating(correct, elapsed)
        stats = stats.record(
            QuizResultDetail(
                vocabText = current.vocab.displayText,
                direction = current.direction,
                elapsedMs = elapsed,
                rating = rating,
                isCorrect = correct
            )
        )
        scope.launch {
            belajarUseCase.submitAnswer(
                userId = userId,
                vocabularyId = current.vocab.id,
                direction = current.direction,
                rating = rating,
                responseTimeMs = elapsed
            )
            onAnswer()
        }
    }

    // Benar → tunggu 500 ms lalu lanjut otomatis.
    LaunchedEffect(selected) {
        val sel = selected
        if (sel != null && sel == current.correctIndex) {
            delay(AUTO_ADVANCE_DELAY_MS)
            advanceOrFinish()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.md)) {
        // ── Penghitung, keluar, chip ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Soal ${index + 1} / ${items.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.sm)) {
                extraChip?.let { PillChip(text = it) }
                PillChip(text = chipText)
            }
        }

        // ── Progress bar ──
        LinearProgressIndicator(
            progress = { (index + 1).toFloat() / items.size },
            modifier = Modifier
                .fillMaxWidth()
                .height(KotomichiDimens.progressTrackMedium),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        // ── Prompt soal ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KotomichiSpacing.lg, vertical = KotomichiSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = current.direction.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(KotomichiSpacing.sm))
                Text(
                    text = current.prompt,
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            }
        }

        // ── Pilihan jawaban ──
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.sm)
        ) {
            current.options.forEachIndexed { optionIndex, optionText ->
                QuizOptionButton(
                    text = optionText,
                    state = when {
                        selected == null -> QuizOptionState.Neutral
                        optionIndex == current.correctIndex -> QuizOptionState.Correct
                        optionIndex == selected -> QuizOptionState.Wrong
                        else -> QuizOptionState.Muted
                    },
                    onClick = {
                        if (selected == null) {
                            selected = optionIndex
                            onAnswered(optionIndex)
                        }
                    }
                )
            }
        }

        // ── Umpan balik ──
        if (selected != null) {
            val isCorrect = selected == current.correctIndex
            if (!isCorrect) {
                Text(
                    text = "Oops, salah!",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Jawaban benar: ${current.options[current.correctIndex]}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.extendedColors.success
                )
                Button(
                    onClick = { advanceOrFinish() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(KotomichiDimens.ratingButtonHeight)
                ) {
                    Text(
                        text = if (index == items.lastIndex) "Lihat Hasil" else "Lanjut",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Spacer(Modifier.height(KotomichiSpacing.xs))
    }
}

/** Ringkasan satu sesi yang baru selesai. */
@Composable
private fun SessionSummary(
    sessionNumber: Int,
    sessionNoun: String,
    outcome: QuizSessionOutcome,
    mode: BelajarMode?,
    isLastSession: Boolean,
    onNextSession: () -> Unit,
    onShowOverall: () -> Unit,
    onExit: () -> Unit
) {
    val stats = outcome.stats
    val result = outcome.result
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = KotomichiSpacing.xl2, bottom = KotomichiSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.lg)
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(KotomichiDimens.completionIconSize)
        )
        Text("$sessionNoun $sessionNumber Selesai", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Kamu menjawab benar ${stats.correct} dari ${stats.total} (${formatScore(stats.scorePercent)}%)",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        SpeedBreakdownRow(stats = stats)

        if (mode != null) {
            when {
                result.hardUnlockedNow -> Text(
                    text = "Mode Sulit terbuka untuk deck ini!",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.extendedColors.success
                )

                result.passed -> Text(
                    text = "Mode Sulit sudah terbuka untuk deck ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                else -> Text(
                    text = "Skor ≥ 90% untuk membuka Mode Sulit.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Rincian jawaban per soal ──
        SectionHeader(title = "Rincian Jawaban")
        stats.details.forEach { detail ->
            AnswerDetailRow(detail = detail)
        }

        // ── Rata-rata waktu per arah ──
        SectionHeader(title = "Rata-rata per Arah")
        DirectionAverageList(stats = stats)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = KotomichiSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.md)
        ) {
            OutlinedButton(
                onClick = onExit,
                modifier = Modifier.weight(1f).height(KotomichiDimens.ratingButtonHeight)
            ) {
                Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(KotomichiSpacing.sm))
                Text("Keluar", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
            Button(
                onClick = if (isLastSession) onShowOverall else onNextSession,
                modifier = Modifier.weight(1.2f).height(KotomichiDimens.ratingButtonHeight)
            ) {
                Text(
                    text = if (isLastSession) "Lihat Ringkasan" else "Sesi Berikutnya",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

/** Ringkasan keseluruhan setelah semua sesi selesai. */
@Composable
private fun OverallSummary(
    stats: QuizSessionStats,
    sessionNoun: String,
    mode: BelajarMode?,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = KotomichiSpacing.xl2, bottom = KotomichiSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.lg)
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(KotomichiDimens.completionIconSize)
        )
        Text("Ringkasan Keseluruhan", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Total benar ${stats.correct} dari ${stats.total} (${formatScore(stats.scorePercent)}%)",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        SpeedBreakdownRow(stats = stats)
        if (stats.total > 0) {
            val avgSeconds = stats.totalTimeMs / stats.total.toDouble() / 1000.0
            Text(
                text = "Rata-rata waktu menjawab: ${formatScore(avgSeconds)} dtk",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val message = when {
            mode == BelajarMode.HARD -> "Deck selesai dalam mode tersulit. Hebat!"
            mode != null && stats.scorePercent >= 90.0 -> "Seluruh deck selesai dengan sangat baik!"
            mode == null -> "Semua kosakata jatuh tempo selesai direview!"
            else -> "Deck selesai. Ulangi sesi untuk meningkatkan skor."
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.extendedColors.success
        )

        SectionHeader(title = "Rata-rata per Arah (Semua Sesi)")
        DirectionAverageList(stats = stats)

        Button(
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .height(KotomichiDimens.ratingButtonHeight)
        ) {
            Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(KotomichiSpacing.sm))
            Text("Selesai", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        }
    }
}

/** Judul seksi dalam ringkasan. */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = KotomichiSpacing.sm),
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
    )
}

/** Satu baris rincian jawaban: kosakata, arah, waktu, rating. */
@Composable
private fun AnswerDetailRow(detail: QuizResultDetail) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KotomichiSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = detail.vocabText,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            Text(
                text = detail.direction.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = formatDuration(detail.elapsedMs),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        RatingBadge(rating = detail.rating)
    }
}

/** Daftar rata-rata waktu per arah dari statistik. */
@Composable
private fun DirectionAverageList(stats: QuizSessionStats) {
    val directions = stats.presentDirections()
    if (directions.isEmpty()) {
        Text(
            text = "Belum ada data.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.xs)
    ) {
        directions.forEach { direction ->
            val avg = stats.averageSecondsPerDirection(direction)
            val count = stats.details.count { it.direction == direction }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KotomichiSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = direction.label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${count} soal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (avg != null) "${formatScore(avg)} dtk" else "—",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

/** Lencana kecil berwarna menurut rating (Mudah/Baik/Sulit/Salah). */
@Composable
private fun RatingBadge(rating: Rating) {
    val (container, content) = when (rating) {
        Rating.EASY -> Pair(MaterialTheme.extendedColors.successContainer, MaterialTheme.extendedColors.onSuccessContainer)
        Rating.GOOD -> Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        Rating.HARD -> Pair(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        Rating.AGAIN -> Pair(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
    }
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = KotomichiSpacing.sm, vertical = 3.dp)
    ) {
        Text(
            text = rating.label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = content
        )
    }
}

internal fun formatDuration(ms: Long): String = "${formatScore(ms / 1000.0)} dtk"

@Composable
private fun SpeedBreakdownRow(stats: QuizSessionStats) {
    Text(
        text = "Cepat (<8 dtk) ${stats.fast} · Baik (8–15 dtk) ${stats.good} · " +
            "Lambat (>15 dtk) ${stats.slow} · Salah ${stats.wrong}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
internal fun LoadingRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = KotomichiSpacing.xl),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(KotomichiSpacing.sm))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

internal enum class QuizOptionState {
    Neutral, Correct, Wrong, Muted
}

@Composable
private fun QuizOptionButton(
    text: String,
    state: QuizOptionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = when (state) {
        QuizOptionState.Neutral -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurface)
        QuizOptionState.Correct -> Pair(MaterialTheme.extendedColors.successContainer, MaterialTheme.extendedColors.onSuccessContainer)
        QuizOptionState.Wrong -> Pair(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        QuizOptionState.Muted -> Pair(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.first,
            contentColor = colors.second
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            maxLines = 2
        )
    }
}

/** Chip kecil dengan latar/pak warna berdasarkan mode atau tipe quiz. */
@Composable
internal fun PillChip(text: String) {
    val (container, content) = when {
        text.contains("Sulit") -> Pair(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        text.contains("Normal") -> Pair(MaterialTheme.extendedColors.successContainer, MaterialTheme.extendedColors.onSuccessContainer)
        else -> Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
    }
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = KotomichiSpacing.md, vertical = KotomichiSpacing.xs)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = content
        )
    }
}

internal fun formatScore(percent: Double): String {
    val rounded = round(percent * 10) / 10
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}