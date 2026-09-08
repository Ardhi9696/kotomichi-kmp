/**
 * File: VocabRow.kt
 * Responsibility: Menampilkan satu baris kosakata dalam daftar kosakata deck.
 *                 Berisi: badge level (JLPT/JFT), kata (kanji dengan furigana,
 *                 atau hiragana/katakana saja), dan arti (bahasa Indonesia).
 */
package com.kotomichi.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.kotomichi.furigana.FuriganaFormatter
import com.kotomichi.model.Vocabulary
import com.kotomichi.ui.components.FuriganaText
import com.kotomichi.ui.theme.KotomichiSpacing

/**
 * Satu baris kosakata dalam daftar. Dapat diklik untuk membuka detail.
 * @param vocab Data kosakata yang ditampilkan
 * @param lastItem Apakah ini item terakhir (untuk separator)
 * @param onClick Callback saat baris diklik
 */
@Composable
fun VocabRow(vocab: Vocabulary, lastItem: Boolean, onClick: () -> Unit) {
    val levelLabel = if (vocab.jftBasic) "JFT" else vocab.jlptLevel?.name
    val formattedText =
        FuriganaFormatter.formatted(vocab.kanji, vocab.hiragana, vocab.furigana)
            ?: (vocab.kanji?.trim().takeIf { it?.isNotEmpty() == true } ?: vocab.hiragana)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = KotomichiSpacing.md, vertical = KotomichiSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (levelLabel != null) {
            LevelBadge(level = levelLabel)
        }
        Column(modifier = Modifier.width(90.dp)) {
            FuriganaText(
                text = formattedText,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 2
            )
        }
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