package com.kotomichi.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.kotomichi.model.Deck
import com.kotomichi.model.DeckProgress
import com.kotomichi.ui.theme.KotomichiSpacing

@Composable
fun StudyHubTab(
    paddingValues: PaddingValues,
    decks: List<Deck>,
    deckProgressMap: Map<Long, DeckProgress>,
    isLoading: Boolean,
    onDeckClick: (Long) -> Unit
) {
    if (isLoading) {
        LoadingTab(paddingValues)
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = KotomichiSpacing.lg),
        contentPadding = PaddingValues(vertical = KotomichiSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.lg)
    ) {
        item {
            Text(
                text = "Kartu Baru",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Pilih bab untuk mulai belajar kartu baru",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(decks) { deck ->
            val progress = deckProgressMap[deck.id]
            LearningDeckItem(deck = deck, progress = progress, onClick = { onDeckClick(deck.id) })
        }
    }
}

@Composable
private fun LearningDeckItem(deck: Deck, progress: DeckProgress?, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KotomichiSpacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.xs)) {
                Text(
                    text = deck.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${deck.vocabularyCount} kosakata",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val vocabLeft = deck.vocabularyCount - (progress?.learnedVocab ?: 0)
            Text(
                text = "$vocabLeft baru",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
internal fun LoadingTab(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(KotomichiSpacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Memuat…",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}