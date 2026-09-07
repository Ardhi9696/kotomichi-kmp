package com.kotomichi.ui.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.kotomichi.model.Direction
import com.kotomichi.ui.theme.KotomichiDimens
import com.kotomichi.ui.theme.KotomichiSpacing

@Composable
fun DirectionTabs(
    unlockedDirections: List<Direction>,
    currentDirection: Direction
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KotomichiSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.sm)
    ) {
        unlockedDirections.forEach { direction ->
            val isSelected = direction == currentDirection
            val isCompleted = direction.ordinal < currentDirection.ordinal

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(KotomichiDimens.directionTabHeight)
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else if (isCompleted) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
                    .padding(horizontal = KotomichiSpacing.sm),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = direction.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else if (isCompleted) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}