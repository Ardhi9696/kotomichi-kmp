/**
 * File: HubScreen.kt
 * Responsibility: Menampilkan halaman utama tab Belajar setelah deck dipilih.
 *                 Berisi: kartu konteks deck, grid 2x2 aksi cepat, dan daftar kosakata.
 *                 Memuat kosakata dari repository saat deck berubah.
 */
package com.kotomichi.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kotomichi.di.get
import com.kotomichi.model.Deck
import com.kotomichi.repository.VocabRepository
import com.kotomichi.ui.theme.KotomichiSpacing

/**
 * Halaman utama tab Belajar (hub).
 * @param paddingValues Padding dari parent
 * @param deck Deck aktif yang dipilih
 * @param onMenuClick Callback saat menu aksi diklik
 */
@Composable
fun HubScreen(
    paddingValues: PaddingValues,
    deck: Deck,
    onMenuClick: (LearnMenu) -> Unit
) {
    var vocabList by remember { mutableStateOf<List<com.kotomichi.model.Vocabulary>?>(null) }
    var loadingVocab by remember { mutableStateOf(true) }
    var errorVocab by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(deck.id) {
        try {
            loadingVocab = true
            val repo = get<VocabRepository>()
            vocabList = repo.getVocabularyByDeck(deck.id)
        } catch (e: Exception) {
            errorVocab = e.message ?: "Gagal memuat kosakata"
        } finally {
            loadingVocab = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = KotomichiSpacing.lg, vertical = KotomichiSpacing.md),
        verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.md)
    ) {
        item {
            // ── Daftar aksi menu (vertikal, full-width) ──
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.sm)
            ) {
                LearnMenu.entries.forEach { menu ->
                    ActionCard(
                        icon = menu.icon,
                        title = menu.title,
                        subtitle = menu.subtitle,
                        badgeCount = menu.badgeCount,
                        onClick = { onMenuClick(menu) }
                    )
                }
            }
        }

        item {
            // Header kosakata
            Text(
                text = "Kosakata (${deck.vocabularyCount})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (loadingVocab && vocabList == null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = KotomichiSpacing.md),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(KotomichiSpacing.sm))
                    Text("Memuat kosakata…", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else if (errorVocab != null) {
            item {
                Text(
                    text = errorVocab!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            val list = vocabList
            if (list != null && list.isNotEmpty()) {
                items(list.size) { index ->
                    VocabRow(vocab = list[index], lastItem = index == list.lastIndex)
                }
            }
        }

        item {
            if (vocabList != null) {
                Spacer(Modifier.height(KotomichiSpacing.xl))
            }
        }
    }
}