package com.kotomichi.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kotomichi.model.Rating
import com.kotomichi.ui.theme.KotomichiDimens
import com.kotomichi.ui.theme.KotomichiSpacing
import com.kotomichi.ui.theme.extendedColors

@Composable
fun KotomichiRatingButtons(onRating: (Rating) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.sm)
        ) {
            RatingButton(
                label = "Salah",
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                onClick = { onRating(Rating.AGAIN) }
            )
            RatingButton(
                label = "Sulit",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                onClick = { onRating(Rating.HARD) }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.sm)
        ) {
            RatingButton(
                label = "Baik",
                containerColor = MaterialTheme.extendedColors.successContainer,
                contentColor = MaterialTheme.extendedColors.onSuccessContainer,
                onClick = { onRating(Rating.GOOD) }
            )
            RatingButton(
                label = "Mudah",
                containerColor = MaterialTheme.extendedColors.successContainer,
                contentColor = MaterialTheme.extendedColors.onSuccessContainer,
                onClick = { onRating(Rating.EASY) }
            )
        }
    }
}

@Composable
private fun RowScope.RatingButton(
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f).height(KotomichiDimens.ratingButtonHeight),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}