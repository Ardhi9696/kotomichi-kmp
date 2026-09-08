package com.kotomichi.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
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
import com.kotomichi.fsrs.FsrsCalculator
import com.kotomichi.model.Rating
import com.kotomichi.model.SrsProgress
import com.kotomichi.model.Vocabulary
import com.kotomichi.ui.common.getAnswerText
import com.kotomichi.ui.common.getAnswerFurigana
import com.kotomichi.ui.common.getQuestionFurigana
import com.kotomichi.ui.common.getQuestionText
import com.kotomichi.ui.components.FuriganaText
import com.kotomichi.ui.components.KotomichiCard
import com.kotomichi.ui.components.KotomichiCardVariant
import com.kotomichi.ui.components.KotomichiProgressBar
import com.kotomichi.ui.components.KotomichiRatingButtons
import com.kotomichi.ui.components.KotomichiTopBar
import com.kotomichi.ui.components.KotomichiTopBarVariant
import com.kotomichi.ui.theme.KotomichiDimens
import com.kotomichi.ui.theme.KotomichiSpacing
import com.kotomichi.ui.theme.VocabDisplayStyle

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
    val retrievability = FsrsCalculator.calculateRetrievability(progress)
    val progressPercent = (cardsReviewed.toFloat() / dueCount).coerceIn(0f, 1f)
    var startTime by remember { mutableStateOf(System.currentTimeMillis()) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(KotomichiSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.lg)
        ) {
            KotomichiTopBar(
                title = "Review ($cardsReviewed/$dueCount)",
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
                progress = progressPercent,
                modifier = Modifier.fillMaxWidth().height(KotomichiDimens.progressTrackThin)
            )

            KotomichiCard(
                variant = KotomichiCardVariant.Outlined,
                contentPadding = PaddingValues(KotomichiSpacing.md)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.lg)
                ) {
                    StatItem(
                        label = "Retrievability",
                        value = "%.0f%%".format(retrievability * 100),
                        icon = Icons.Filled.Psychology,
                        color = if (retrievability > 0.9) MaterialTheme.colorScheme.primary
                        else if (retrievability > 0.7) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.error
                    )
                    StatItem(
                        label = "Stability",
                        value = "%.1f hari".format(progress.stability),
                        icon = Icons.Filled.Schedule,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    StatItem(
                        label = "Due",
                        value = if (progress.dueDate < System.currentTimeMillis()) "Sekarang" else "Belum",
                        icon = Icons.Filled.AccessTime,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            DirectionIndicator(direction = progress.direction)

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
                                    text = getQuestionFurigana(vocab, progress.direction)
                                        ?: getQuestionText(vocab, progress.direction),
                                    style = if (progress.direction.name.startsWith("MEANING")) {
                                        MaterialTheme.typography.headlineMedium
                                    } else {
                                        VocabDisplayStyle
                                    },
                                    textAlign = TextAlign.Center
                                )

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
                                        text = getAnswerFurigana(vocab, progress.direction)
                                            ?: getAnswerText(vocab, progress.direction),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Text(
                                        text = "Rating otomatis berdasarkan waktu jawab",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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