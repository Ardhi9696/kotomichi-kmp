/**
 * File: SplitBelajarActionCard.kt
 * Responsibility: Kartu aksi cepat "Belajar" yang terbagi menjadi dua zona warna
 *                 (Normal hijau, Sulit merah/oranye). Setiap zona terpisah dapat disentuh
 *                 dan mode Sulit menampilkan status terkunci bila belum dibuka.
 */
package com.kotomichi.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kotomichi.ui.theme.KotomichiSpacing
import com.kotomichi.ui.theme.extendedColors
import com.kotomichi.usecase.BelajarMode

/**
 * Kartu aksi "Belajar" dengan dua zona: Normal (hijau) dan Sulit (merah/oranye).
 * @param hardUnlocked Apakah mode sulit sudah terbuka untuk deck aktif
 * @param normalProgressPercent Progress (0.0–1.0) run belajar mode Normal
 * @param hardProgressPercent Progress (0.0–1.0) run belajar mode Sulit
 * @param normalRunLabel Label run mode Normal (mis. "Percobaan ke-2 · 40%"); null = belum dimulai
 * @param hardRunLabel Label run mode Sulit; null = belum dimulai
 * @param onNormalClick Callback saat zona Normal disentuh
 * @param onHardClick Callback saat zona Sulit disentuh (bila sudah terbuka)
 * @param modifier Modifier eksternal
 */
@Composable
fun SplitBelajarActionCard(
    hardUnlocked: Boolean,
    normalProgressPercent: Float = 0f,
    hardProgressPercent: Float = 0f,
    normalRunLabel: String? = null,
    hardRunLabel: String? = null,
    onNormalClick: () -> Unit,
    onHardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            BelajarZone(
                icon = Icons.Rounded.School,
                iconTint = MaterialTheme.extendedColors.success,
                containerColor = MaterialTheme.extendedColors.successContainer,
                contentColor = MaterialTheme.extendedColors.onSuccessContainer,
                title = "Belajar · ${BelajarMode.NORMAL.label}",
                subtitle = BelajarMode.NORMAL.description,
                progress = normalProgressPercent,
                progressLabel = normalRunLabel,
                onClick = onNormalClick
            )
            BelajarZone(
                icon = if (hardUnlocked) Icons.Rounded.LocalFireDepartment else Icons.Rounded.Lock,
                iconTint = MaterialTheme.colorScheme.error,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                title = "Belajar · ${BelajarMode.HARD.label}",
                subtitle = if (hardUnlocked) {
                    BelajarMode.HARD.description
                } else {
                    "Terkunci · Selesaikan normal dengan nilai ≥ 90% untuk deck ini"
                },
                progress = hardProgressPercent,
                progressLabel = hardRunLabel,
                onClick = onHardClick
            )
        }
    }
}

/**
 * Satu zona (kolom) pada kartu Belajar.
 * @param icon Ikon zona
 * @param iconTint Warna ikon
 * @param containerColor Warna latar zona
 * @param contentColor Warna teks pada zona
 * @param title Judul zona
 * @param subtitle Subjudul zona
 * @param progress Progress run (0.0–1.0); ditampilkan bila progressLabel tidak null
 * @param progressLabel Label persen/percobaan run; null = run belum dimulai
 * @param onClick Callback saat zona disentuh
 */
@Composable
private fun BelajarZone(
    icon: ImageVector,
    iconTint: Color,
    containerColor: Color,
    contentColor: Color,
    title: String,
    subtitle: String,
    progress: Float,
    progressLabel: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(containerColor)
            .padding(horizontal = KotomichiSpacing.md, vertical = KotomichiSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(contentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            progressLabel?.let { label ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = contentColor,
                        trackColor = contentColor.copy(alpha = 0.2f)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor.copy(alpha = 0.9f)
                    )
                }
            }
        }
        Spacer(Modifier.width(KotomichiSpacing.xs))
    }
}