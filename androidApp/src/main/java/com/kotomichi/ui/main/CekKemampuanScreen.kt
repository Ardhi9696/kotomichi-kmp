/**
 * File: CekKemampuanScreen.kt
 * Responsibility: Layar "Cek Kemampuan" - memeriksa kemampuan memori kosakata
 *                 dalam deck via flashcard. Berisi progress bar posisi kartu,
 *                 kartu flip dengan swipe penilaian (kanan=tahu, kiri=tidak tahu),
 *                 dan tombol penilaian di bawah kartu.
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kotomichi.di.get
import com.kotomichi.model.Deck
import com.kotomichi.model.Vocabulary
import com.kotomichi.repository.VocabRepository
import com.kotomichi.ui.theme.KotomichiDimens
import com.kotomichi.ui.theme.KotomichiSpacing
import com.kotomichi.ui.theme.extendedColors

/**
 * Layar Cek Kemampuan untuk sebuah deck.
 * @param paddingValues Padding dari parent
 * @param deck Deck yang sedang diuji
 * @param onBack Callback saat kembali ke hub
 */
@Composable
fun CekKemampuanScreen(
    paddingValues: PaddingValues,
    deck: Deck,
    onBack: () -> Unit
) {
    var vocabList by remember { mutableStateOf<List<Vocabulary>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var currentIndex by remember(deck.id) { mutableIntStateOf(0) }

    LaunchedEffect(deck.id) {
        try {
            loading = true
            val repo = get<VocabRepository>()
            vocabList = repo.getVocabularyByDeck(deck.id)
        } catch (e: Exception) {
            error = e.message ?: "Gagal memuat kosakata"
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = KotomichiSpacing.lg, vertical = KotomichiSpacing.md),
        verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.md)
    ) {
        // ── Penghitung kartu ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val list = vocabList
            if (list != null && list.isNotEmpty()) {
                Text(
                    text = "${currentIndex + 1} / ${list.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when {
            loading && vocabList == null -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = KotomichiSpacing.xl),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(KotomichiSpacing.sm))
                    Text("Memuat kosakata…", style = MaterialTheme.typography.bodySmall)
                }
            }

            error != null -> {
                val errorText = error
                Text(
                    text = errorText ?: "Terjadi kesalahan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            vocabList.isNullOrEmpty() -> {
                Text(
                    text = "Deck ini belum memiliki kosakata.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> {
                val list = vocabList!!
                val safeIndex = currentIndex.coerceIn(0, list.lastIndex)
                val current = list[safeIndex]
                val isLast = safeIndex == list.lastIndex

                val advance = {
                    if (safeIndex < list.lastIndex) currentIndex = safeIndex + 1 else onBack()
                }

                // ── Progress bar ──
                LinearProgressIndicator(
                    progress = { (safeIndex + 1).toFloat() / list.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(KotomichiDimens.progressTrackMedium),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                // ── Kartu flash (swipe kiri/kana) ──
                Flashcard(
                    vocab = current,
                    onAssess = { advance() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                // ── Hint swipe ──
                Text(
                    text = "Swipe ke kanan = tahu · Swipe ke kiri = tidak tahu",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // ── Tombol penilaian ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.md)
                ) {
                    AssessmentButton(
                        label = "Tidak tahu",
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        onClick = { advance() },
                        modifier = Modifier.weight(1f)
                    )
                    AssessmentButton(
                        label = "Tahu",
                        containerColor = MaterialTheme.extendedColors.successContainer,
                        contentColor = MaterialTheme.extendedColors.onSuccessContainer,
                        onClick = { advance() },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isLast) {
                    Spacer(Modifier.height(KotomichiSpacing.xs))
                    Text(
                        text = "Kartu terakhir — selesai untuk kembali ke hub",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(KotomichiSpacing.xs))
    }
}

/**
 * Tombol penilaian subjektif Cek Kemampuan.
 * @param label Label tombol
 * @param containerColor Warna latar tombol (token desain Kotomichi)
 * @param contentColor Warna teks tombol
 * @param onClick Callback saat tombol diklik
 * @param modifier Modifier eksternal
 */
@Composable
private fun AssessmentButton(
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(KotomichiDimens.ratingButtonHeight),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}