package com.kotomichi.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.kotomichi.ui.theme.KotomichiDimens
import com.kotomichi.ui.theme.KotomichiSpacing

@Composable
fun ActionButtonsRow(
    dueCount: Int,
    onLearnClick: () -> Unit,
    onReviewClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.md)
    ) {
        Button(
            onClick = onLearnClick,
            modifier = Modifier.weight(1f).height(KotomichiDimens.actionButtonHeight)
        ) {
            Text("Belajar Baru", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        }

        FilledTonalButton(
            onClick = onReviewClick,
            modifier = Modifier.weight(1f).height(KotomichiDimens.actionButtonHeight),
            enabled = dueCount > 0
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                if (dueCount > 0) {
                    Spacer(modifier = Modifier.width(KotomichiSpacing.sm))
                    Box(
                        modifier = Modifier
                            .size(KotomichiDimens.dueBadgeSize)
                            .background(
                                color = MaterialTheme.colorScheme.error,
                                shape = CircleShape
                            )
                    ) {
                        Text(
                            text = dueCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onError,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}