/**
 * File: LevelBadge.kt
 * Responsibility: Badge kecil untuk menampilkan level kosakata (JLPT N1–N5 atau JFT).
 *                 Warna diadaptasi berdasarkan level untuk memudahkan scan cepat.
 */
package com.kotomichi.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kotomichi.ui.theme.extendedColors

/**
 * Badge level kosakata.
 * @param level Label level (contoh "N5", "N4", "JFT")
 * @param modifier Modifier eksternal
 */
@Composable
fun LevelBadge(level: String, modifier: Modifier = Modifier) {
    val (container, content) = levelColor(level)
    Surface(
        shape = RoundedCornerShape(50),
        color = container,
        contentColor = content,
        modifier = modifier
    ) {
        Text(
            text = level,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Pilih warna container & content berdasarkan level.
 * JFT = biru (tertiary), N5 = hijau (sukses), N4 = secondary,
 * N3 = primary, N2 = merah muda, N1 = gelap.
 */
@Composable
private fun levelColor(level: String): Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> {
    return when (level.uppercase()) {
        "JFT" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        "N5" -> MaterialTheme.extendedColors.successContainer to MaterialTheme.extendedColors.onSuccessContainer
        "N4" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        "N3" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "N2" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        "N1" -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
}