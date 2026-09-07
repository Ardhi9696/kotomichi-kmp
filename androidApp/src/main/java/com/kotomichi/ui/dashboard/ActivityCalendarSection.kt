package com.kotomichi.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.kotomichi.ui.components.KotomichiCard
import com.kotomichi.ui.components.KotomichiCardVariant
import com.kotomichi.ui.theme.KotomichiSpacing

@Composable
fun ActivityCalendarSection() {
    KotomichiCard(variant = KotomichiCardVariant.Filled) {
        Column {
            Text(
                text = "Kalender Aktivitas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(KotomichiSpacing.lg))
            Text(
                text = "Heatmap aktivitas belajar akan ditampilkan di sini",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = KotomichiSpacing.xl2)
            )
        }
    }
}