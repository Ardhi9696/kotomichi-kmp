/**
 * File: DeckPickerScreen.kt
 * Responsibility: Menampilkan layar pemilihan deck saat pertama kali
 *                 mengakses tab Belajar atau saat user ingin ganti deck.
 *                 Menampilkan daftar deck tersedia dengan progress.
 */
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
 * Layar pemilihan deck.
 * @param paddingValues Padding dari parent
 * @param decks Daftar deck tersedia
 * @param deckProgressMap Progress per deck
 * @param onClose Callback saat user menutup picker (setelah pilih deck)
 */
@Composable
fun DeckPickerScreen(
    paddingValues: PaddingValues,
    decks: List<Deck>,
    deckProgressMap: Map<Long, DeckProgress>,
    onClose: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = KotomichiSpacing.lg),
        contentPadding = PaddingValues(vertical = KotomichiSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.md)
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Tutup"
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.xs)) {
                    Text(
                        text = "Pilih Deck",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pilih deck yang ingin dipelajari",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (decks.isEmpty()) {
            item {
                Text(
                    text = "Belum ada deck tersedia. Silakan tarik untuk menyinkronkan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(decks) { deck ->
            val progress = deckProgressMap[deck.id]
            DeckPickerItem(deck = deck, progress = progress, onClick = { onClose() })
        }
    }
}
