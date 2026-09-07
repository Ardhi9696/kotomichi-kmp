package com.kotomichi.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.kotomichi.model.Deck
import com.kotomichi.model.DeckProgress
import com.kotomichi.ui.theme.KotomichiSpacing

@Composable
fun DeckListSection(
    decks: List<Deck>,
    deckProgressMap: Map<Long, DeckProgress>,
    onDeckClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Daftar Bab",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${decks.size} bab tersedia",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.sm)
        ) {
            decks.forEach { deck ->
                val progress = deckProgressMap[deck.id]
                DeckItem(deck = deck, progress = progress, onClick = { onDeckClick(deck.id) })
            }
        }
    }
}