/**
 * File: VocabRow.kt
 * Responsibility: Menampilkan satu baris kosakata dalam daftar kosakata deck.
 *                 Berisi: badge level (JLPT/JFT), kata (kanji dengan furigana,
 *                 atau hiragana/katakana saja), dan arti (bahasa Indonesia).
 */
package com.kotomichi.ui.main

import androidx.compose.foundation.background
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
import com.kotomichi.model.Vocabulary
import com.kotomichi.ui.theme.KotomichiSpacing
import com.turtlekazu.furiganable.compose.m3.TextWithReading

/**
 * Satu baris kosakata dalam daftar.
 * @param vocab Data kosakata yang ditampilkan
 * @param lastItem Apakah ini item terakhir (untuk separator)
 */
@Composable
fun VocabRow(vocab: Vocabulary, lastItem: Boolean) {
    val levelLabel = if (vocab.jftBasic) "JFT" else vocab.jlptLevel?.name
    val displayText = vocab.kanji?.trim().takeIf { it?.isNotEmpty() == true } ?: vocab.hiragana
    val hasKanji = vocab.kanji?.trim()?.isNotEmpty() == true && containsKanji(vocab.kanji!!)
    val reading = vocab.hiragana.ifEmpty { null }
    val formattedText = if (hasKanji && reading != null) {
        "[$displayText[$reading]]"
    } else {
        displayText
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KotomichiSpacing.md, vertical = KotomichiSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (levelLabel != null) {
            LevelBadge(level = levelLabel)
        }
        Column(modifier = Modifier.width(90.dp)) {
            TextWithReading(
                formattedText = formattedText,
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

/**
 * Deteksi apakah string mengandung karakter kanji (CJK Unified Ideographs U+4E00–U+9FFF)
 * atau kana majemuk. Jika true, furigana ditampilkan.
 */
private fun containsKanji(text: String): Boolean =
    text.any { it.code in 0x4E00..0x9FFF }