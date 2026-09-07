package com.kotomichi.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kotomichi.ui.components.KotomichiProgressBar
import com.kotomichi.ui.theme.KotomichiDimens
import com.kotomichi.ui.theme.KotomichiSpacing

@Composable
fun HomeTopBar(
    userName: String,
    level: Int,
    totalExp: Long,
    currentLevelExp: Long,
    nextLevelExp: Long,
    expProgress: Double,
    onNotificationsClick: () -> Unit,
    onDebugTap: () -> Unit
) {
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapMs by remember { mutableLongStateOf(0L) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = KotomichiSpacing.lg, vertical = KotomichiSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                name = userName,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        val now = System.currentTimeMillis()
                        tapCount = if (now - lastTapMs > 600L) 1 else tapCount + 1
                        lastTapMs = now
                        if (tapCount >= 7) {
                            tapCount = 0
                            onDebugTap()
                        }
                    })
                }
            )

            Column(verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.xs)) {
                Text(
                    text = "Selamat datang kembali,",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = userName.ifBlank { "Pengguna" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
Text(
                    text = "Level $level • ${(totalExp - currentLevelExp)}/${(nextLevelExp - currentLevelExp)} EXP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                    KotomichiProgressBar(
                        progress = (expProgress / 100).toFloat().coerceIn(0f, 1f),
                        modifier = Modifier
                            .width(KotomichiDimens.topBarProgressWidth)
                            .height(KotomichiDimens.progressTrackThin)
                    )
                }
            }
        }

        IconButton(onClick = onNotificationsClick) {
            Icon(
                imageVector = Icons.Rounded.Notifications,
                contentDescription = "Notifikasi",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun Avatar(name: String, modifier: Modifier = Modifier, size: Dp = KotomichiDimens.avatarSize) {
    val initial = name.trim().firstOrNull()?.uppercase() ?: "?"
    Column(
        modifier = modifier
            .size(size)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}