/**
 * File: ReviewQuizScreen.kt
 * Responsibility: Layar Review global berbasis MCQ (identik dengan Belajar).
 *                 Menarik semua kartu jatuh tempo dari seluruh deck (tidak
 *                 terikat deck tertentu), membaginya menjadi sesi-sesi, lalu
 *                 memakai alur quiz yang sama (jeda 500 ms saat benar, manual
 *                 saat salah, ringkasan per sesi + ringkasan keseluruhan).
 */
package com.kotomichi.ui.belajar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kotomichi.di.get
import com.kotomichi.model.Vocabulary
import com.kotomichi.repository.DeckRepository
import com.kotomichi.repository.VocabRepository
import com.kotomichi.ui.theme.KotomichiSpacing
import com.kotomichi.usecase.BelajarQuizResult
import com.kotomichi.usecase.BelajarQuizUseCase

/**
 * Layar Review global.
 * @param paddingValues Padding dari parent
 * @param userId ID user aktif
 * @param onExit Callback keluar dari layar review
 */
@Composable
fun ReviewQuizScreen(
    paddingValues: PaddingValues,
    userId: String,
    onExit: () -> Unit
) {
    val belajarUseCase: BelajarQuizUseCase = get()

    var sessions by remember { mutableStateOf<List<List<BelajarQuizItem>>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        loading = true
        loadError = null
        try {
            val dueSessions = belajarUseCase.prepareReviewSessions(userId)
            val vocabRepo: VocabRepository = get()
            val deckRepo: DeckRepository = get()

            // Petakan vocabId → vocab dari deck asal, agar distraktor diambil
            // dari deck yang sama (opsi selalu 4; fallback ke kartu due sesi).
            val allDecks = deckRepo.getAllDecks()
            val deckVocabMap = mutableMapOf<Long, List<Vocabulary>>()
            for (deck in allDecks) {
                val deckVocab = vocabRepo.getVocabularyByDeck(deck.id)
                for (v in deckVocab) {
                    deckVocabMap[v.id] = deckVocab
                }
            }

            sessions = dueSessions.map { sessionDue ->
                buildReviewQuestions(sessionDue, deckVocabMap)
            }
        } catch (e: Exception) {
            loadError = e.message ?: "Gagal memuat kartu jatuh tempo"
            sessions = emptyList()
        } finally {
            loading = false
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
            loading -> LoadingRow(text = "Memuat kartu jatuh tempo…")

            loadError != null -> {
                Text(
                    text = loadError ?: "Terjadi kesalahan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                OutlinedButton(onClick = onExit) { Text("Keluar") }
            }

            sessions.isNullOrEmpty() -> {
                Text(
                    text = "Belum ada kosakata yang jatuh tempo. Kamu sudah meninjau semua kartu wajib.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = onExit) { Text("Keluar") }
            }

            else -> QuizSessionFlow(
                sessions = sessions!!,
                sessionNoun = "Review",
                userId = userId,
                chipText = "Review",
                mode = null,
                onEverythingComplete = {},
                finalizeSession = { stats ->
                    QuizSessionOutcome(
                        stats = stats,
                        result = BelajarQuizResult(stats.total, stats.correct, stats.scorePercent, false)
                    )
                },
                onExit = onExit
            )
        }
    }
}