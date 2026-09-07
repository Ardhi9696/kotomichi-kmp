package com.kotomichi.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.kotomichi.model.Direction
import com.kotomichi.ui.theme.KotomichiSpacing

@Composable
fun DirectionIndicator(direction: Direction) {
    val color = when (direction) {
        Direction.KANJI_TO_MEANING -> MaterialTheme.colorScheme.primary
        Direction.KANJI_TO_HIRAGANA -> MaterialTheme.colorScheme.secondary
        Direction.HIRAGANA_TO_MEANING -> MaterialTheme.colorScheme.tertiary
        Direction.MEANING_TO_HIRAGANA -> MaterialTheme.colorScheme.error
        Direction.HIRAGANA_TO_KANJI -> MaterialTheme.colorScheme.primaryContainer
        Direction.MEANING_TO_KANJI -> MaterialTheme.colorScheme.outlineVariant
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KotomichiSpacing.lg)
            .background(
                color = color.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium
            )
            .padding(KotomichiSpacing.md)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = direction.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}