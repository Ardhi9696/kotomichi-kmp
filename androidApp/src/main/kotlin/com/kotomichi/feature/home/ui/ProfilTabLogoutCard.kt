/**
 * File: ProfilTabLogoutCard.kt
 * Responsibility: Kartu logout di bagian bawah halaman profil.
 */
package com.kotomichi.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.kotomichi.core.designsystem.common.KotomichiCard
import com.kotomichi.core.designsystem.common.KotomichiCardVariant
import com.kotomichi.core.designsystem.theme.KotomichiSpacing

@Composable
internal fun ProfilTabLogoutCard(onClick: () -> Unit) {
    KotomichiCard(
        variant = KotomichiCardVariant.Outlined,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = KotomichiSpacing.lg, vertical = KotomichiSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Keluar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}