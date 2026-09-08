/**
 * File: CekKemampuanScreen.kt
 * Responsibility: Layar "Cek Kemampuan" - memeriksa kemampuan memori kosakata
 *                 dalam deck via flashcard. Berisi progress bar posisi kartu,
 *                 kartu flip dengan swipe penilaian (kanan=tahu, kiri=tidak tahu),
 *                 tombol penilaian di bawah kartu, dan ringkasan hasil setelah
 *                 kartu terakhir dinilai.
 */
package com.kotomichi.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    var knownCount by remember(deck.id) { mutableIntStateOf(0) }
    var finished by remember(deck.id) { mutableStateOf(false) }

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
        val list = vocabList
        if (list != null && list.isNotEmpty() && !finished) {
            // ── Penghitung kartu ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentIndex.coerceIn(0, list.lastIndex) + 1} / ${list.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when {
            finished && !list.isNullOrEmpty() -> {
                val asked = list.size
                val known = knownCount
                val unknown = asked - known
                KotomichiQuizSummary(
                    total = asked,
                    known = known,
                    unknown = unknown,
                    onRetry = {
                        currentIndex = 0
                        knownCount = 0
                        finished = false
                    },
                    onBack = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }

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
                val items = list ?: return@Column
                val totalSize = items.size
                val safeIndex = currentIndex.coerceIn(0, items.lastIndex)
                val current = items[safeIndex]

                val advance = { known: Boolean ->
                    if (known) knownCount++
                    if (safeIndex < items.lastIndex) currentIndex = safeIndex + 1 else finished = true
                }

                // ── Progress bar ──
                LinearProgressIndicator(
                    progress = { (safeIndex + 1).toFloat() / totalSize },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(KotomichiDimens.progressTrackMedium),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                // ── Kartu flash (swipe kiri/kanan) ──
                Flashcard(
                    vocab = current,
                    onAssess = { known -> advance(known) },
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
                    textAlign = TextAlign.Center
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
                        onClick = { advance(false) },
                        modifier = Modifier.weight(1f)
                    )
                    AssessmentButton(
                        label = "Tahu",
                        containerColor = MaterialTheme.extendedColors.successContainer,
                        contentColor = MaterialTheme.extendedColors.onSuccessContainer,
                        onClick = { advance(true) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(KotomichiSpacing.xs))
    }
}

/**
 * Ringkasan hasil Cek Kemampuan yang tampil setelah semua kartu dinilai.
 * Menampilkan jumlah yang diingat (tahu) dan yang belum, serta pilihan
 * mengulang kuis atau kembali ke Study Hub.
 * @param total Jumlah kartu yang diuji
 * @param known Jumlah kartu yang dijawab "tahu"
 * @param unknown Jumlah kartu yang dijawab "tidak tahu"
 * @param onRetry Callback untuk mengulang dari awal
 * @param onBack Callback kembali ke Study Hub
 * @param modifier Modifier eksternal
 */
@Composable
private fun KotomichiQuizSummary(
    total: Int,
    known: Int,
    unknown: Int,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(top = KotomichiSpacing.xl2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.lg)
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(KotomichiDimens.completionIconSize)
        )
        Text("Cek Kemampuan Selesai", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Kamu mengingat $known dari $total kosakata",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.sm)
        ) {
            SummaryCountCard(
                label = "Diingat",
                count = known,
                containerColor = MaterialTheme.extendedColors.successContainer,
                contentColor = MaterialTheme.extendedColors.onSuccessContainer
            )
            SummaryCountCard(
                label = "Belum diingat",
                count = unknown,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = KotomichiSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.md)
        ) {
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.weight(1f).height(KotomichiDimens.ratingButtonHeight)
            ) {
                Icon(
                    imageVector = Icons.Filled.Replay,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(KotomichiSpacing.sm))
                Text("Ulangi", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
            Button(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(KotomichiDimens.ratingButtonHeight)
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(KotomichiSpacing.sm))
                Text("Ke Study Hub", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

/**
 * Kartu berisi label dan jumlah untuk ringkasan hasil.
 */
@Composable
private fun SummaryCountCard(
    label: String,
    count: Int,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KotomichiSpacing.md))
            .height(52.dp)
            .padding(horizontal = KotomichiSpacing.md)
            .background(containerColor),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = contentColor
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = contentColor
        )
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