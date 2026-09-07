package com.kotomichi.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.kotomichi.ui.components.KotomichiCard
import com.kotomichi.ui.components.KotomichiCardVariant
import com.kotomichi.ui.theme.KotomichiSpacing

@Composable
fun ReviewTab(
    paddingValues: PaddingValues,
    dueCount: Int,
    isLoading: Boolean,
    onStartReview: () -> Unit
) {
    if (isLoading) {
        LoadingTab(paddingValues)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KotomichiSpacing.lg, vertical = KotomichiSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.lg)
    ) {
        Text(
            text = "Review",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        KotomichiCard(variant = KotomichiCardVariant.Filled) {
            Column(verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.md)) {
                Text(
                    text = if (dueCount > 0) "$dueCount kartu siap direview"
                    else "Tidak ada kartu due hari ini",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ulangi kartu yang menunggu jadwal ulang untuk menguatkan ingatanmu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onStartReview,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = dueCount > 0
                ) {
                    Text(
                        text = if (dueCount > 0) "Mulai Review" else "Belum ada kartu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}