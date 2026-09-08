/**
 * File: VocabDetailSheet.kt
 * Responsibility: Bottom sheet detail kosakata yang muncul saat satu kosakata
 *                 dalam Study Hub diklik. Menampilkan seluruh kolom yang ada di
 *                 database Vocabulary: level, part of speech (badge kanji),
 *                 kanji (tanpa furigana) + cara baca + romaji, arti (ID/EN),
 *                 contoh kalimat, kolokasi, dan audio.
 */
package com.kotomichi.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kotomichi.model.Vocabulary
import com.kotomichi.ui.theme.KotomichiSpacing

/**
 * Bottom sheet detail kosakata yang tampil saat satu baris kosakata diklik.
 * @param vocab Kosakata yang didetailkan
 * @param onDismiss Callback saat sheet ditutup
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabDetailSheet(
    vocab: Vocabulary,
    onDismiss: () -> Unit
) {
    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        VocabSheetContent(vocab = vocab, modifier = Modifier.padding(horizontal = KotomichiSpacing.lg))
    }
}

/**
 * Konten scrollable dari bottom sheet detail kosakata.
 */
@Composable
private fun VocabSheetContent(vocab: Vocabulary, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.md)
    ) {
        item { VocabHeader(vocab = vocab) }
        item { VocabBadges(vocab = vocab) }

        if (vocab.meaningIndonesian.isNotBlank()) {
            item { VocabSection(label = "Arti") { ArtiText(vocab = vocab) } }
        }

        if (vocab.exampleSentences.isNotEmpty()) {
            item { VocabSection(label = "Contoh kalimat") { ExampleSentences(vocab = vocab) } }
        }

        if (vocab.collocations.isNotEmpty()) {
            item { VocabSection(label = "Kolokasi") { Collocations(vocab = vocab) } }
        }

        if (vocab.audioAssets.isNotEmpty()) {
            item { VocabSection(label = "Audio") { AudioAssets(vocab = vocab) } }
        }

        item { Spacer(Modifier.height(KotomichiSpacing.xl2)) }
    }
}

/**
 * Header: kanji besar (tanpa furigana, karena hiragana sudah ditampilkan
 * terpisah di bawahnya), cara baca, dan romaji.
 */
@Composable
private fun VocabHeader(vocab: Vocabulary) {
    val displayText = vocab.kanji?.trim().takeIf { it?.isNotEmpty() == true } ?: vocab.hiragana

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = KotomichiSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.xs)
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (vocab.hiragana.isNotBlank()) {
                Text(
                    text = vocab.hiragana,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val romaji = vocab.romaji
            if (romaji != null && romaji.isNotBlank()) {
                Text(
                    text = romaji,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Baris badge: level (JLPT/JFT) + part of speech dengan label kanji.
 */
@Composable
private fun VocabBadges(vocab: Vocabulary) {
    val levelLabel = if (vocab.jftBasic) "JFT" else vocab.jlptLevel?.name
    val tags = partOfSpeechTags(vocab)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (levelLabel != null) {
            LevelBadge(level = levelLabel)
        }
        tags.forEachIndexed { index, tag ->
            if (levelLabel != null || index > 0) Spacer(Modifier.width(KotomichiSpacing.xs))
            TagBadge(text = tag)
        }
    }
}

/**
 * Badge kanji part of speech yang ringkas.
 */
@Composable
internal fun TagBadge(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.padding(vertical = 1.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

/**
 * Peta flag tata bahasa menjadi badge kanji lengkap.
 * 五段動詞 = godan, 一段動詞 = ichidan, 自動詞 = intransitif, 他動詞 = transitif,
 * イ形容詞 = i-adjektiva, ナ形容詞 = na-adjektiva, 不規則動詞 = tidak beraturan,
 * 連語 = kolokasi (verb), 名詞 = kata benda (bila tidak ada flag lain).
 */
internal fun partOfSpeechTags(vocab: Vocabulary): List<String> =
    buildList {
        if (vocab.godanVerb) add("五段動詞")
        if (vocab.ichidanVerb) add("一段動詞")
        if (vocab.jidoushi) add("自動詞")
        if (vocab.tadoushi) add("他動詞")
        if (vocab.iAdjective) add("イ形容詞")
        if (vocab.naAdjective) add("ナ形容詞")
        if (vocab.fukisoku) add("不規則動詞")
        if (vocab.verbCollocation) add("連語")
        val hasType =
            vocab.godanVerb || vocab.ichidanVerb || vocab.fukisoku ||
            vocab.iAdjective || vocab.naAdjective ||
            vocab.jidoushi || vocab.tadoushi || vocab.verbCollocation
        if (!hasType) add("名詞")
    }

/**
 * Seksi berlabel dalam detail.
 */
@Composable
private fun VocabSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.xs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

/**
 * Arti bahasa Indonesia (dan Inggris jika tersedia).
 */
@Composable
private fun ArtiText(vocab: Vocabulary) {
    Column(verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.xs)) {
        Text(vocab.meaningIndonesian, style = MaterialTheme.typography.bodyLarge)
        vocab.meaningEnglish?.takeIf { it.isNotBlank() && it != vocab.meaningIndonesian }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Daftar contoh kalimat dengan terjemahan bahasa Indonesia.
 */
@Composable
private fun ExampleSentences(vocab: Vocabulary) {
    Column(verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.md)) {
        vocab.exampleSentences.forEach { example ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(example.japanese, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (example.indonesian.isNotBlank()) {
                    Text(example.indonesian, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/**
 * Daftar kolokasi (verb collocation) beserta artinya.
 */
@Composable
private fun Collocations(vocab: Vocabulary) {
    Column(verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.md)) {
        vocab.collocations.forEach { collocation ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(collocation.collocation, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                val meaning = collocation.meaning
                if (meaning != null && meaning.isNotBlank()) {
                    Text(meaning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/**
 * Daftar audio asset (jika ada).
 */
@Composable
private fun AudioAssets(vocab: Vocabulary) {
    Column(verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.xs)) {
        vocab.audioAssets.forEach { asset ->
            Text(
                text = asset.filename ?: asset.storageKey,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}