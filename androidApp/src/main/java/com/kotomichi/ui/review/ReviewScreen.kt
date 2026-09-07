package com.kotomichi.ui.review

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotomichi.di.get
import com.kotomichi.model.Rating
import com.kotomichi.model.SrsProgress
import com.kotomichi.model.Vocabulary
import com.kotomichi.model.VocabularyTranslation
import com.kotomichi.ui.components.KotomichiCompletionScreen
import com.kotomichi.ui.components.KotomichiLoadingScreen
import com.kotomichi.usecase.ReviewCardUseCase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(onComplete: () -> Unit) {
    val reviewUseCase: ReviewCardUseCase = get()
    val scope = rememberCoroutineScope()

    var currentProgress by remember { mutableStateOf<SrsProgress?>(null) }
    var currentVocab by remember { mutableStateOf<Vocabulary?>(null) }
    var showAnswer by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var cardsReviewed by remember { mutableStateOf(0) }
    var startTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var showComplete by remember { mutableStateOf(false) }

    val dueCount by reviewUseCase.observeDueCount("user_id").collectAsStateWithLifecycle(0)

    suspend fun loadNextCard() {
        showAnswer = false
        startTime = System.currentTimeMillis()
        val dueCards = reviewUseCase.getDueCards("user_id", 1)
        if (dueCards.isNotEmpty()) {
            currentProgress = dueCards.first()
        } else {
            showComplete = true
        }
    }

    suspend fun handleAnswer(progress: SrsProgress, vocab: Vocabulary, rating: Rating, responseTime: Long) {
        reviewUseCase.submitReviewAnswer("user_id", vocab.id, progress.direction, rating, responseTime)
        cardsReviewed++
        loadNextCard()
    }

    // Load first card
    scope.launch {
        val dueCards = reviewUseCase.getDueCards("user_id", 1)
        if (dueCards.isNotEmpty()) {
            currentProgress = dueCards.first()
        }
        isLoading = false
    }

    if (isLoading) {
        KotomichiLoadingScreen()
        return
    }

    if (showComplete) {
        KotomichiCompletionScreen(
            title = "Review Selesai!",
            subtitle = "$cardsReviewed kartu direview hari ini",
            onComplete = onComplete
        )
        return
    }

    currentProgress?.let { progress ->
        // Mock vocab for now
        val vocab = Vocabulary(
            id = progress.vocabularyId,
            kanji = "勉強",
            hiragana = "べんきょう",
            romaji = "benkyou",
            translations = listOf(
                VocabularyTranslation(vocabularyId = progress.vocabularyId, locale = "id", meaning = "belajar"),
                VocabularyTranslation(vocabularyId = progress.vocabularyId, locale = "en", meaning = "study")
            )
        )
        currentVocab = vocab

        ReviewCardScreen(
            progress = progress,
            vocab = vocab,
            showAnswer = showAnswer,
            onShowAnswer = { showAnswer = true },
            onAnswer = { rating, responseTime ->
                scope.launch { handleAnswer(progress, vocab, rating, responseTime) }
            },
            dueCount = dueCount,
            cardsReviewed = cardsReviewed,
            onComplete = onComplete
        )
    } ?: KotomichiCompletionScreen(
        title = "Review Selesai!",
        subtitle = "$cardsReviewed kartu direview hari ini",
        onComplete = onComplete
    )
}