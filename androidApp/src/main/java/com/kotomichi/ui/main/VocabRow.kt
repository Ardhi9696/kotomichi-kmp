/**
 * File: VocabRow.kt
 * Responsibility: Menampilkan satu baris kosakata dalam daftar kosakata deck.
 *                 Menampilkan kanji, hiragana, dan arti (bahasa Indonesia).
 */
package com.kotomichi.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kotomichi.model.Vocabulary
import com.kotomichi.ui.theme.KotomichiSpacing

/**
 * Satu baris kosakata dalam daftar.
 * @param vocab Data kosakata yang ditampilkan
 * @param lastItem Apakah ini item terakhir (untuk separator)
 */
@Composable
fun VocabRow(vocab: Vocabulary, lastItem: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KotomichiSpacing.md, vertical = KotomichiSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = vocab.kanji.orEmpty(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = vocab.hiragana.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(Modifier.weight(1f))
        val meaningText = vocab.translations.firstOrNull { it.locale == "id" }?.meaning
        if (meaningText != null) {
            Text(
                text = meaningText.split("\n").firstOrNull()?.take(50) ?: "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    if (!lastItem) {
        Spacer(Modifier.height(1.dp))
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))
    }
}
