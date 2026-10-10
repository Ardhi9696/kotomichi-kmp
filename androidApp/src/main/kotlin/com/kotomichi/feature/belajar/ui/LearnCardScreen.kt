package com.kotomichi.feature.belajar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.kotomichi.shared.core.model.Direction
import com.kotomichi.shared.core.model.Rating
import com.kotomichi.shared.core.model.Vocabulary
import com.kotomichi.core.designsystem.common.getAnswerText
import com.kotomichi.core.designsystem.common.getAnswerFurigana
import com.kotomichi.core.designsystem.common.getPronunciation
import com.kotomichi.core.designsystem.common.getQuestionFurigana
import com.kotomichi.core.designsystem.common.getQuestionText
import com.kotomichi.core.designsystem.common.FuriganaText
import com.kotomichi.core.designsystem.common.KotomichiCard
import com.kotomichi.core.designsystem.common.KotomichiCardVariant
import com.kotomichi.core.designsystem.common.KotomichiProgressBar
import com.kotomichi.core.designsystem.common.KotomichiRatingButtons
import com.kotomichi.core.designsystem.common.KotomichiTopBar
import com.kotomichi.core.designsystem.common.KotomichiTopBarVariant
import com.kotomichi.core.designsystem.theme.KotomichiDimens
import com.kotomichi.core.designsystem.theme.KotomichiSpacing
import com.kotomichi.core.designsystem.theme.VocabDisplayStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnCardScreen(
    card: Vocabulary,
    currentDirection: Direction,
    unlockedDirections: List<Direction>,
    showAnswer: Boolean,
    onShowAnswer: () -> Unit,
    onAnswer: (Rating, Long) -> Unit,
    progress: Float,
    cardsLearned: Int,
    onComplete: () -> Unit
) {
    var startTime by remember { mutableStateOf(System.currentTimeMillis()) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(KotomichiSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.lg)
        ) {
            KotomichiTopBar(
                title = "Belajar: ${currentDirection.label}",
                variant = KotomichiTopBarVariant.CenterAlignedSmall,
                navigationIcon = {
                    IconButton(onClick = onComplete) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Tutup"
                        )
                    }
                }
            )

            KotomichiProgressBar(
                progress = progress.coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth().height(KotomichiDimens.progressTrackThin)
            )

            Text(
                text = "$cardsLearned kartu dipelajari",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            DirectionTabs(
                unlockedDirections = unlockedDirections,
                currentDirection = currentDirection
            )

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                KotomichiCard(
                    variant = KotomichiCardVariant.Elevated,
                    contentPadding = PaddingValues(KotomichiSpacing.xl)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.lg)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.sm)
                            ) {
                                FuriganaText(
                                    text = getQuestionFurigana(card, currentDirection)
                                        ?: getQuestionText(card, currentDirection),
                                    style = if (currentDirection.name.startsWith("MEANING")) {
                                        MaterialTheme.typography.headlineMedium
                                    } else {
                                        VocabDisplayStyle
                                    },
                                    textAlign = TextAlign.Center
                                )

                                if (currentDirection == Direction.KANJI_TO_HIRAGANA || currentDirection == Direction.HIRAGANA_TO_KANJI) {
                                    Text(
                                        text = getPronunciation(card, currentDirection),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(onClick = { /* Play audio */ }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Dengarkan",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (showAnswer) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.xl)
                                ) {
                                    HorizontalDivider(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )

                                    FuriganaText(
                                        text = getAnswerFurigana(card, currentDirection)
                                            ?: getAnswerText(card, currentDirection),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    KotomichiRatingButtons(
                                        onRating = { rating ->
                                            val timeTaken = (System.currentTimeMillis() - startTime).toLong()
                                            onAnswer(rating, timeTaken)
                                        }
                                    )
                                }
                            } else {
                                Button(
                                    onClick = onShowAnswer,
                                    modifier = Modifier.fillMaxWidth().height(KotomichiDimens.actionButtonHeight)
                                ) {
                                    Text(
                                        text = "Tampilkan Jawaban",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}