package com.kotomichi.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kotomichi.ui.theme.KotomichiDimens
import com.kotomichi.ui.theme.KotomichiSpacing

@Composable
fun KotomichiCompletionScreen(
    title: String,
    subtitle: String,
    onComplete: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.lg)
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(KotomichiDimens.completionIconSize)
            )
            Text(title, style = MaterialTheme.typography.headlineLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onComplete,
                modifier = Modifier.padding(top = KotomichiSpacing.lg)
            ) {
                Text("Kembali ke Dashboard", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}