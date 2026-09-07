package com.kotomichi.ui.review

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotomichi.di.get
import com.kotomichi.model.Direction
import com.kotomichi.model.Rating
import com.kotomichi.model.SrsProgress
import com.kotomichi.model.Vocabulary
import com.kotomichi.model.VocabularyTranslation
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
        LoadingScreen()
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
    } ?: CompletionScreen(cardsReviewed = cardsReviewed, onComplete = onComplete)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewCardScreen(
    progress: SrsProgress,
    vocab: Vocabulary,
    showAnswer: Boolean,
    onShowAnswer: () -> Unit,
    onAnswer: (Rating, Long) -> Unit,
    dueCount: Int,
    cardsReviewed: Int,
    onComplete: () -> Unit
) {
    val retrievability = com.kotomichi.fsrs.FsrsCalculator.calculateRetrievability(progress)
    val progressPercent = (cardsReviewed.toFloat() / dueCount).coerceIn(0f, 1f)
    var startTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Bar
            TopAppBar(
                title = {
                    Text("Review ($cardsReviewed/$dueCount)", fontSize = 16.sp)
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
                progress = { progressPercent },
                modifier = Modifier.fillMaxWidth().height(4.dp)
            )
            
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatItem(
                    label = "Retrievability",
                    value = "%.0f%%".format(retrievability * 100),
                    icon = Icons.Filled.Psychology,
                    color = if (retrievability > 0.9) androidx.compose.material3.MaterialTheme.colorScheme.primary
                    else if (retrievability > 0.7) androidx.compose.material3.MaterialTheme.colorScheme.tertiary
                    else androidx.compose.material3.MaterialTheme.colorScheme.error
                )
                StatItem(
                    label = "Stability",
                    value = "%.1f hari".format(progress.stability),
                    icon = Icons.Filled.Schedule,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.secondary
                )
                StatItem(
                    label = "Due",
                    value = if (progress.dueDate < System.currentTimeMillis()) "Sekarang" else "Belum",
                    icon = Icons.Filled.AccessTime,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary
                )
            }
            
            // Direction Indicator
            DirectionIndicator(direction = progress.direction)
            
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
                                text = getQuestionText(vocab, progress.direction),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            
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
                                    text = getAnswerText(vocab, progress.direction),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                                )
                                
                                // Auto-rating info
                                Text(
                                    text = "Rating otomatis berdasarkan waktu jawab",
                                    fontSize = 12.sp,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                // Rating Buttons (auto-rated but user can override)
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
fun androidx.compose.foundation.layout.RowScope.StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = "", tint = color, modifier = Modifier.size(20.dp))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 10.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun DirectionIndicator(direction: Direction) {
    val color = when (direction) {
        Direction.KANJI_TO_MEANING -> androidx.compose.material3.MaterialTheme.colorScheme.primary
        Direction.KANJI_TO_HIRAGANA -> androidx.compose.material3.MaterialTheme.colorScheme.secondary
        Direction.HIRAGANA_TO_MEANING -> androidx.compose.material3.MaterialTheme.colorScheme.tertiary
        Direction.MEANING_TO_HIRAGANA -> androidx.compose.material3.MaterialTheme.colorScheme.error
        Direction.HIRAGANA_TO_KANJI -> androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
        Direction.MEANING_TO_KANJI -> androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = direction.label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
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
                label = "Salah (Again)",
                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                onClick = { onRating(Rating.AGAIN) }
            )
            RatingButton(
                rating = Rating.HARD,
                label = "Sulit (Hard)",
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
                label = "Baik (Good)",
                color = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                onClick = { onRating(Rating.GOOD) }
            )
            RatingButton(
                rating = Rating.EASY,
                label = "Mudah (Easy)",
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
fun CompletionScreen(cardsReviewed: Int, onComplete: () -> Unit) {
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
            Text("Review Selesai!", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text("$cardsReviewed kartu direview hari ini", fontSize = 18.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
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

fun getQuestionText(vocab: Vocabulary, direction: Direction): String {
    return when (direction) {
        Direction.KANJI_TO_MEANING -> vocab.kanji ?: vocab.hiragana
        Direction.KANJI_TO_HIRAGANA -> vocab.kanji ?: vocab.hiragana
        Direction.HIRAGANA_TO_MEANING -> vocab.hiragana
        Direction.MEANING_TO_HIRAGANA -> vocab.meaningIndonesian
        Direction.HIRAGANA_TO_KANJI -> vocab.hiragana
        Direction.MEANING_TO_KANJI -> vocab.meaningIndonesian
    }
}

fun getAnswerText(vocab: Vocabulary, direction: Direction): String {
    return when (direction) {
        Direction.KANJI_TO_MEANING -> "${vocab.meaningIndonesian} (${vocab.hiragana})"
        Direction.KANJI_TO_HIRAGANA -> vocab.hiragana
        Direction.HIRAGANA_TO_MEANING -> "${vocab.meaningIndonesian} (${vocab.kanji ?: vocab.hiragana})"
        Direction.MEANING_TO_HIRAGANA -> vocab.hiragana
        Direction.HIRAGANA_TO_KANJI -> vocab.kanji ?: vocab.hiragana
        Direction.MEANING_TO_KANJI -> vocab.kanji ?: vocab.hiragana
    }
}