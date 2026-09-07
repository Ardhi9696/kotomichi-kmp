package com.kotomichi.ui.learn

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.kotomichi.di.get
import com.kotomichi.model.Direction
import com.kotomichi.model.Rating
import com.kotomichi.model.Vocabulary
import com.kotomichi.ui.components.KotomichiCompletionScreen
import com.kotomichi.ui.components.KotomichiLoadingScreen
import com.kotomichi.usecase.LearnCardUseCase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(
    deckId: Long,
    onComplete: () -> Unit
) {
    val learnUseCase: LearnCardUseCase = get()
    val scope = rememberCoroutineScope()

    var currentCard by remember { mutableStateOf<Vocabulary?>(null) }
    var currentDirection by remember { mutableStateOf(Direction.KANJI_TO_MEANING) }
    var showAnswer by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var cardsLearned by remember { mutableStateOf(0) }
    var unlockedDirections by remember { mutableStateOf<List<Direction>>(listOf(Direction.KANJI_TO_MEANING)) }
    var showDirectionUnlocked by remember { mutableStateOf(false) }
    var unlockedDirection by remember { mutableStateOf<Direction?>(null) }
    var startTime by remember { mutableStateOf(System.currentTimeMillis()) }

    suspend fun loadNextCard() {
        showAnswer = false
        startTime = System.currentTimeMillis()
        val card = learnUseCase.getNextNewCard("user_id", deckId)
        currentCard = card
        if (card != null) {
            unlockedDirections = learnUseCase.getUnlockedDirections("user_id", card.id)
            currentDirection = unlockedDirections.firstOrNull() ?: Direction.KANJI_TO_MEANING
        }
        isLoading = false
    }

    suspend fun handleAnswer(card: Vocabulary, direction: Direction, rating: Rating, responseTime: Long) {
        learnUseCase.submitLearnAnswer("user_id", card.id, direction, rating, responseTime)

        val shouldUnlock = learnUseCase.checkDirectionUnlock("user_id", card.id, direction)
        if (shouldUnlock && direction != Direction.MEANING_TO_KANJI) {
            val nextDir = Direction.values()[direction.ordinal + 1]
            unlockedDirection = nextDir
            showDirectionUnlocked = true
            unlockedDirections = learnUseCase.getUnlockedDirections("user_id", card.id)
        }

        if (direction != Direction.MEANING_TO_KANJI && unlockedDirections.contains(Direction.values()[direction.ordinal + 1])) {
            currentDirection = Direction.values()[direction.ordinal + 1]
            showAnswer = false
            startTime = System.currentTimeMillis()
        } else {
            cardsLearned++
            loadNextCard()
        }
    }

    // Load first card
    scope.launch {
        loadNextCard()
    }

    val progress = cardsLearned / 20f // placeholder

    if (isLoading) {
        KotomichiLoadingScreen()
        return
    }

    currentCard?.let { card ->
        LearnCardScreen(
            card = card,
            currentDirection = currentDirection,
            unlockedDirections = unlockedDirections,
            showAnswer = showAnswer,
            onShowAnswer = { showAnswer = true },
            onAnswer = { rating, responseTime ->
                scope.launch { handleAnswer(card, currentDirection, rating, responseTime) }
            },
            progress = progress,
            cardsLearned = cardsLearned,
            onComplete = onComplete
        )
    } ?: KotomichiCompletionScreen(
        title = "Selesai!",
        subtitle = "$cardsLearned kartu baru dipelajari",
        onComplete = onComplete
    )

    if (showDirectionUnlocked) {
        unlockedDirection?.let { dir ->
            DirectionUnlockedDialog(
                direction = dir,
                onDismiss = { showDirectionUnlocked = false }
            )
        }
    }
}