/**
 * File: Flashcard.kt
 * Responsibility: Komponen kartu flash untuk Cek Kemampuan. Sisi depan menampilkan
 *                 kosakata (kanji + furigana), sisi belakang menampilkan cara baca,
 *                 arti, contoh kalimat (jika ada), badge level, dan catatan (jika ada).
 *                 Kartu berputar (flip) saat diketuk dan dapat di-swipe:
 *                 kanan = tahu (success), kiri = tidak tahu (error).
 */
package com.kotomichi.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kotomichi.model.Vocabulary
import com.kotomichi.ui.theme.KotomichiSpacing
import com.kotomichi.ui.theme.extendedColors
import com.turtlekazu.furiganable.compose.m3.TextWithReading
import kotlinx.coroutines.launch

/**
 * Kartu flash yang bisa dibalik (flip 180 derajat) dan di-swipe untuk penilaian.
 * swipe kanan -> tahu, swipe kiri -> tidak tahu.
 * @param vocab Kosakata yang ditampilkan
 * @param onAssess Callback hasil penilaian: true = tahu, false = tidak tahu
 * @param modifier Modifier eksternal
 */
@Composable
fun Flashcard(
    vocab: Vocabulary,
    onAssess: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var flipped by remember(vocab.id) { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "flashcard_flip"
    )
    val showFront = rotation <= 90f

    val offsetX = remember(vocab.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var containerWidth by remember(vocab.id) { mutableStateOf(1f) }

    Box(
        modifier = modifier
            .onSizeChanged { containerWidth = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(vocab.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            val threshold = containerWidth * 0.22f
                            when {
                                offsetX.value > threshold -> {
                                    offsetX.animateTo(containerWidth * 1.6f, tween(260))
                                    onAssess(true)
                                }
                                offsetX.value < -threshold -> {
                                    offsetX.animateTo(-containerWidth * 1.6f, tween(260))
                                    onAssess(false)
                                }
                                else -> offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch { offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                }
            }
    ) {
        // Overlay indicator kanan (tahu)
        SwipeIndicator(
            tint = MaterialTheme.extendedColors.successContainer,
            label = "Tahu",
            labelColor = MaterialTheme.extendedColors.onSuccessContainer,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val f = (offsetX.value / containerWidth).coerceIn(0f, 1f)
                    alpha = f
                    scaleX = 0.6f + 0.4f * f
                    scaleY = 0.6f + 0.4f * f
                    rotationZ = 4f
                }
        )

        // Overlay indicator kiri (tidak tahu)
        SwipeIndicator(
            tint = MaterialTheme.colorScheme.errorContainer,
            label = "Tidak tahu",
            labelColor = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val f = (-offsetX.value / containerWidth).coerceIn(0f, 1f)
                    alpha = f
                    scaleX = 0.6f + 0.4f * f
                    scaleY = 0.6f + 0.4f * f
                    rotationZ = -4f
                }
        )

        // Kartu yang ikut bergeser dan berotasi mengikuti drag
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = offsetX.value
                    rotationZ = (offsetX.value / containerWidth) * 8f
                }
        ) {
            Card(
                onClick = { flipped = !flipped },
                modifier = Modifier.fillMaxSize(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                if (showFront) {
                    FlashcardFront(vocab = vocab)
                } else {
                    FlashcardBack(vocab = vocab)
                }
            }
            // Indikator ketuk (hanya di depan, sisi belum dibalik penuh)
            if (showFront) {
                Text(
                    text = "Ketuk kartu untuk melihat jawaban",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = KotomichiSpacing.md)
                )
            }
        }
    }
}

/**
 * Overlay besar berisi label yang muncul saat kartu di-swipe ke arah tertentu.
 */
@Composable
private fun SwipeIndicator(
    tint: androidx.compose.ui.graphics.Color,
    label: String,
    labelColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(KotomichiSpacing.lg))
            .background(tint),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = labelColor,
            modifier = Modifier.padding(KotomichiSpacing.md)
        )
    }
}

/**
 * Sisi depan kartu: menampilkan kosakata (kanji dengan furigana, atau hiragana saja).
 */
@Composable
private fun FlashcardFront(vocab: Vocabulary) {
    val displayText = vocab.kanji?.trim().takeIf { it?.isNotEmpty() == true } ?: vocab.hiragana
    val hasKanji = vocab.kanji?.trim()?.isNotEmpty() == true && containsKanji(vocab.kanji!!)
    val reading = vocab.hiragana.ifEmpty { null }
    val formattedText = if (hasKanji && reading != null) {
        "[$displayText[$reading]]"
    } else {
        displayText
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(KotomichiSpacing.xl2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TextWithReading(
            formattedText = formattedText,
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Sisi belakang kartu: cara baca, arti, contoh kalimat, badge level, dan catatan.
 */
@Composable
private fun FlashcardBack(vocab: Vocabulary) {
    val levelLabel = if (vocab.jftBasic) "JFT" else vocab.jlptLevel?.name
    val indonesian = vocab.meaningIndonesian
    val romaji = vocab.romaji
    val notes = vocabNotes(vocab)

    val example = vocab.exampleSentences.firstOrNull { it.japanese.isNotBlank() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(KotomichiSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.md)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.md),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(KotomichiSpacing.xs)
        ) {
            item {
                val displayText = vocab.kanji?.trim().takeIf { it?.isNotEmpty() == true } ?: vocab.hiragana
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                levelLabel?.let {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        LevelBadge(level = it)
                    }
                }
            }

            item {
                SectionDetail(label = "Cara baca") {
                    Text(
                        text = vocab.hiragana.ifEmpty { "—" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (romaji != null && romaji.isNotBlank()) {
                        Text(
                            text = romaji,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (indonesian.isNotBlank()) {
                item {
                    SectionDetail(label = "Arti") {
                        Text(
                            text = indonesian,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (example != null) {
                item {
                    SectionDetail(label = "Contoh kalimat") {
                        Text(
                            text = example.japanese,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (example.indonesian.isNotBlank()) {
                            Text(
                                text = example.indonesian,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (notes.isNotBlank()) {
                item {
                    SectionDetail(label = "Catatan") {
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Blok detail berlabel pada bagian belakang flashcard.
 * @param label Label seksi
 * @param content Konten seksi
 */
@Composable
private fun SectionDetail(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                .clip(CircleShape)
        ) {}
        Spacer(Modifier.height(KotomichiSpacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(KotomichiSpacing.xs))
        content()
    }
}

/**
 * Deteksi apakah string mengandung karakter kanji (CJK Unified Ideographs U+4E00–U+9FFF).
 * Jika true, furigana ditampilkan pada sisi depan kartu.
 */
private fun containsKanji(text: String): Boolean =
    text.any { it.code in 0x4E00..0x9FFF }

/**
 * Bangun catatan dari kolom tata bahasa (part of speech, jenis kata kerja/adjektiva)
 * yang tersedia pada model kosakata. Mengembalikan string kosong jika tidak ada.
 */
private fun vocabNotes(vocab: Vocabulary): String {
    val parts = mutableListOf<String>()
    val partOfSpeech = vocab.partOfSpeech
    if (!partOfSpeech.isNullOrBlank()) parts.add(partOfSpeech)
    if (vocab.godanVerb) parts.add("Godan verb")
    if (vocab.ichidanVerb) parts.add("Ichidan verb")
    if (vocab.fukisoku) parts.add("Fukisoku")
    if (vocab.iAdjective) parts.add("I-adjective")
    if (vocab.naAdjective) parts.add("Na-adjective")
    if (vocab.jidoushi) parts.add("Jidoushi")
    if (vocab.tadoushi) parts.add("Tadoushi")
    if (vocab.verbCollocation) parts.add("Verb collocation")
    return parts.joinToString(" · ")
}