package com.kotomichi.ui.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kotomichi.di.get
import com.kotomichi.model.Direction
import com.kotomichi.model.Rating
import com.kotomichi.model.Vocabulary
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
        LoadingScreen()
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
            onNextCard = { scope.launch { loadNextCard() } },
            progress = progress,
            cardsLearned = cardsLearned,
            onComplete = onComplete
        )
    } ?: CompletionScreen(cardsLearned = cardsLearned, onComplete = onComplete)

    if (showDirectionUnlocked) {
        unlockedDirection?.let { dir ->
            DirectionUnlockedDialog(
                direction = dir,
                onDismiss = { showDirectionUnlocked = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnCardScreen(
    card: Vocabulary,
    currentDirection: Direction,
    unlockedDirections: List<Direction>,
    showAnswer: Boolean,
    onShowAnswer: () -> Unit,
    onAnswer: (Rating, Long) -> Unit,
    onNextCard: () -> Unit,
    progress: Float,
    cardsLearned: Int,
    onComplete: () -> Unit
) {
    var startTime by remember { mutableStateOf(System.currentTimeMillis()) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Bar with Progress
            TopAppBar(
                title = {
                    Text("Belajar: ${currentDirection.label}", fontSize = 16.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onComplete) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Tutup"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                )
            )
            
            // Progress Bar
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp)
            )
            
            Text("$cardsLearned kartu dipelajari", fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            
            // Direction Tabs
            DirectionTabs(
                unlockedDirections = unlockedDirections,
                currentDirection = currentDirection
            )
            
            // Card Content
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Question
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = getQuestionText(card, currentDirection),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            
                            if (currentDirection == Direction.KANJI_TO_HIRAGANA || currentDirection == Direction.HIRAGANA_TO_KANJI) {
                                Text(
                                    text = getPronunciation(card, currentDirection),
                                    fontSize = 24.sp,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            IconButton(onClick = { /* Play audio */ }) {
                                Icon(
                                    imageVector = Icons.Filled.VolumeUp,
                                    contentDescription = "Dengarkan",
                                    tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        // Answer
                        if (showAnswer) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                androidx.compose.material3.HorizontalDivider(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant
                                )
                                
                                Text(
                                    text = getAnswerText(card, currentDirection),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                                )
                                
                                // Rating Buttons
                                RatingButtons(
                                    onRating = { rating ->
                                        val timeTaken = (System.currentTimeMillis() - startTime).toLong()
                                        onAnswer(rating, timeTaken)
                                    }
                                )
                            }
                        } else {
                            Button(
                                onClick = onShowAnswer,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Tampilkan Jawaban", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DirectionTabs(
    unlockedDirections: List<Direction>,
    currentDirection: Direction
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        unlockedDirections.forEach { direction ->
            val isSelected = direction == currentDirection
            val isCompleted = direction.ordinal < currentDirection.ordinal
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .background(
                        color = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
                        else if (isCompleted) androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer
                        else androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 8.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = direction.label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
                    else if (isCompleted) androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer
                    else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun RatingButtons(onRating: (Rating) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RatingButton(
                rating = Rating.AGAIN,
                label = "Salah",
                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                onClick = { onRating(Rating.AGAIN) }
            )
            RatingButton(
                rating = Rating.HARD,
                label = "Sulit",
                color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
                onClick = { onRating(Rating.HARD) }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RatingButton(
                rating = Rating.GOOD,
                label = "Baik",
                color = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                onClick = { onRating(Rating.GOOD) }
            )
            RatingButton(
                rating = Rating.EASY,
                label = "Mudah",
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                onClick = { onRating(Rating.EASY) }
            )
        }
    }
}

@Composable
internal fun androidx.compose.foundation.layout.RowScope.RatingButton(
    rating: Rating,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f).height(48.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = color
        )
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
fun CompletionScreen(cardsLearned: Int, onComplete: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "",
                tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp)
            )
            Text("Selesai!", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text("$cardsLearned kartu baru dipelajari", fontSize = 18.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onComplete, modifier = Modifier.padding(top = 16.dp)) {
                Text("Kembali ke Dashboard", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.CircularProgressIndicator()
    }
}

@Composable
fun DirectionUnlockedDialog(
    direction: Direction,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Arah Baru Terbuka!") },
        text = { Text("Kamu telah menguasai ${direction.label}\nSekarang bisa belajar: ${Direction.values()[direction.ordinal + 1].label}") },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Lanjutkan") }
        }
    )
}

fun getQuestionText(card: Vocabulary, direction: Direction): String {
    return when (direction) {
        Direction.KANJI_TO_MEANING -> card.kanji ?: card.hiragana
        Direction.KANJI_TO_HIRAGANA -> card.kanji ?: card.hiragana
        Direction.HIRAGANA_TO_MEANING -> card.hiragana
        Direction.MEANING_TO_HIRAGANA -> card.meaningIndonesian
        Direction.HIRAGANA_TO_KANJI -> card.hiragana
        Direction.MEANING_TO_KANJI -> card.meaningIndonesian
    }
}

fun getPronunciation(card: Vocabulary, direction: Direction): String {
    return when (direction) {
        Direction.KANJI_TO_HIRAGANA -> card.hiragana
        Direction.HIRAGANA_TO_KANJI -> card.hiragana
        else -> ""
    }
}

fun getAnswerText(card: Vocabulary, direction: Direction): String {
    return when (direction) {
        Direction.KANJI_TO_MEANING -> "${card.meaningIndonesian} (${card.hiragana})"
        Direction.KANJI_TO_HIRAGANA -> card.hiragana
        Direction.HIRAGANA_TO_MEANING -> "${card.meaningIndonesian} (${card.kanji ?: card.hiragana})"
        Direction.MEANING_TO_HIRAGANA -> card.hiragana
        Direction.HIRAGANA_TO_KANJI -> card.kanji ?: card.hiragana
        Direction.MEANING_TO_KANJI -> card.kanji ?: card.hiragana
    }
}