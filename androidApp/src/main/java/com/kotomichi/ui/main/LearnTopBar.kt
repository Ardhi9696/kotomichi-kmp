/**
 * File: LearnTopBar.kt
 * Responsibility: Menampilkan top bar tab Belajar dengan judul elegan
 *                 (label kecil "belajar" + judul besar "Belajar") dan
 *                 tombol chip deck di pojok kanan atas untuk ganti deck.
 */
package com.kotomichi.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.ReplayCircleFilled
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kotomichi.ui.theme.KotomichiSpacing

/**
 * Top bar tab Belajar (biasa).
 * @param deckTitle Judul deck aktif yang ditampilkan di chip
 * @param deckSubtitle Subjudul deck aktif (opsional, ditampilkan di baris kedua chip)
 * @param onDeckClick Callback saat chip deck diklik untuk buka deck picker
 */
@Composable
fun LearnTopBar(
    deckTitle: String,
    deckSubtitle: String?,
    onDeckClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = KotomichiSpacing.lg, vertical = KotomichiSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Belajar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        FilledTonalButton(
            onClick = onDeckClick,
            contentPadding = PaddingValues(start = KotomichiSpacing.md, end = KotomichiSpacing.sm),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(KotomichiSpacing.sm))
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.widthIn(max = 200.dp)
            ) {
                Text(
                    text = deckTitle,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                deckSubtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(KotomichiSpacing.xs))
            Icon(
                imageVector = Icons.Rounded.ReplayCircleFilled,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
