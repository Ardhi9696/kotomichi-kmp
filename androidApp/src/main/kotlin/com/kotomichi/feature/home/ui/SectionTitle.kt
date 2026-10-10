/**
 * File: SectionTitle.kt
 * Responsibility: Komponen heading untuk section di halaman pengaturan.
 */
package com.kotomichi.feature.home.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kotomichi.core.designsystem.theme.KotomichiSpacing

@Composable
internal fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = KotomichiSpacing.xs)
    )
}
