package com.kotomichi.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.kotomichi.model.UserProfile
import com.kotomichi.ui.components.KotomichiCard
import com.kotomichi.ui.components.KotomichiCardVariant
import com.kotomichi.ui.components.KotomichiProgressBar
import com.kotomichi.ui.theme.KotomichiDimens
import com.kotomichi.ui.theme.KotomichiSpacing

@Composable
fun ProfileCard(
    user: UserProfile,
    currentLevel: Int,
    totalExp: Long,
    nextLevelExp: Long,
    currentLevelExp: Long,
    expProgress: Double,
    currentStreak: Int
) {
    KotomichiCard(variant = KotomichiCardVariant.Filled) {
        Column(
            verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Selamat datang kembali, ${user.name}!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Level $currentLevel • ${totalExp.toString()} EXP",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(KotomichiDimens.levelBadgeSize)
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape
                        )
                        .padding(KotomichiSpacing.sm)
                ) {
                    Text(
                        text = "$currentLevel",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.xs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Progress ke Level ${currentLevel + 1}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "${(totalExp - currentLevelExp)} / ${(nextLevelExp - currentLevelExp)} EXP",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                KotomichiProgressBar(
                    progress = (expProgress / 100).toFloat().coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth().height(KotomichiDimens.progressTrackLarge)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.lg)
            ) {
                StreakItem(
                    icon = Icons.Filled.LocalFireDepartment,
                    label = "Streak",
                    value = "$currentStreak hari",
                    color = MaterialTheme.colorScheme.primary
                )
                StreakItem(
                    icon = Icons.Filled.Psychology,
                    label = "Kuasai",
                    value = "${user.totalExp / 100} kata",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}