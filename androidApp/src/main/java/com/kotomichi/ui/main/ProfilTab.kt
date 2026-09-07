package com.kotomichi.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotomichi.ui.components.KotomichiCard
import com.kotomichi.ui.components.KotomichiCardVariant
import com.kotomichi.ui.components.KotomichiDialog
import com.kotomichi.ui.theme.KotomichiDimens
import com.kotomichi.ui.theme.KotomichiSpacing
import com.kotomichi.ui.theme.ThemePreference

@Composable
fun ProfilTab(
    paddingValues: PaddingValues,
    userName: String,
    userEmail: String,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val themeMode by ThemePreference.mode.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KotomichiSpacing.lg, vertical = KotomichiSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.lg)
    ) {
        Text(
            text = "Pengaturan",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        ProfileHeader(name = userName, email = userEmail)

        KotomichiCard(variant = KotomichiCardVariant.Filled) {
            Column(verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.md)) {
                Text(
                    text = "Tema",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                ThemeHero(
                    current = themeMode,
                    onSelect = { mode -> ThemePreference.setMode(context, mode) }
                )
            }
        }

        KotomichiCard(variant = KotomichiCardVariant.Outlined) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = KotomichiSpacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                TextButton(onClick = { showLogoutDialog = true }) {
                    Text(
                        text = "Keluar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showLogoutDialog) {
        KotomichiDialog(
            title = "Keluar",
            text = "Yakin ingin keluar dari akun ini?",
            confirmLabel = "Keluar",
            dismissLabel = "Batal",
            isDestructive = true,
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}

@Composable
private fun ProfileHeader(name: String, email: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(name = name)
        Column(verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.xs)) {
            Text(
                text = name.ifBlank { "Pengguna" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ThemeHero(current: String, onSelect: (String) -> Unit) {
    val options = listOf(
        ThemePreference.MODE_DARK to "Gelap",
        ThemePreference.MODE_LIGHT to "Terang",
        ThemePreference.MODE_SYSTEM to "Sistem"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.large
            )
            .padding(KotomichiSpacing.md),
        verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.xs)
    ) {
        Text(
            text = "Mode Tampilan",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        options.forEach { (mode, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(KotomichiDimens.heroSwitchHeight)
                    .selectable(
                        selected = current == mode,
                        onClick = { onSelect(mode) }
                    )
                    .padding(horizontal = KotomichiSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = current == mode,
                    onClick = { onSelect(mode) }
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}