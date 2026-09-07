package com.kotomichi.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.kotomichi.model.Deck
import com.kotomichi.model.DeckProgress
import com.kotomichi.ui.components.KotomichiCard
import com.kotomichi.ui.components.KotomichiCardVariant
import com.kotomichi.ui.components.KotomichiProgressBar
import com.kotomichi.ui.theme.KotomichiDimens
import com.kotomichi.ui.theme.KotomichiSpacing

@Composable
fun DeckItem(
    deck: Deck,
    progress: DeckProgress?,
    onClick: () -> Unit
) {
    val mastery = progress?.masteryPercent ?: 0.0
    val vocabCount = progress?.totalVocab ?: deck.vocabularyCount
    val isUnlocked = progress?.averageRetrievability ?: 0.0 >= 0.90 || deck.orderIndex == 1

    KotomichiCard(
        variant = KotomichiCardVariant.Filled,
        onClick = onClick
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deck.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = deck.subtitle ?: "JLPT ${deck.jlptLevel?.name ?: ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!isUnlocked) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Terkunci",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(start = KotomichiSpacing.md)
                ) {
                    Text(
                        text = "$vocabCount",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "kosakata",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(KotomichiSpacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.md)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kemahiran",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    KotomichiProgressBar(
                        progress = (mastery / 100).toFloat().coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth().height(KotomichiDimens.progressTrackMedium)
                    )
                    Text(
                        text = "%.0f%%".format(mastery),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (progress != null) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kartu",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${progress.learnedVocab} / ${progress.totalVocab}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}