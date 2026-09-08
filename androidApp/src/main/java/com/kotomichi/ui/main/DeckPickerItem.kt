/**
 * File: DeckPickerItem.kt
 * Responsibility: Menampilkan satu item deck dalam DeckPickerScreen.
 *                 Menampilkan judul deck, jumlah kosakata, dan sisa kartu baru.
 */
package com.kotomichi.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.kotomichi.model.Deck
import com.kotomichi.model.DeckProgress
import com.kotomichi.ui.theme.KotomichiSpacing

/**
 * Satu item deck dalam picker.
 * @param deck Data deck yang ditampilkan
 * @param progress Progress deck (opsional)
 * @param onClick Callback saat deck diklik
 */
@Composable
fun DeckPickerItem(deck: Deck, progress: DeckProgress?, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KotomichiSpacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.xs)) {
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
                Text(
                    text = "${deck.vocabularyCount} kosakata",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val vocabLeft = (deck.vocabularyCount - (progress?.learnedVocab ?: 0))
                .coerceIn(0, deck.vocabularyCount)
            Text(
                text = "$vocabLeft baru",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
