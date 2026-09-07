package com.kotomichi.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.kotomichi.ui.components.KotomichiCard
import com.kotomichi.ui.components.KotomichiCardVariant
import com.kotomichi.ui.components.KotomichiDialog
import com.kotomichi.ui.theme.KotomichiSpacing
import com.kotomichi.ui.theme.LanguagePreference
import com.kotomichi.ui.theme.ThemePreference

@Composable
fun ProfilTab(
    paddingValues: PaddingValues,
    userName: String,
    userEmail: String,
    onLogout: () -> Unit,
    onUpdateName: suspend (String) -> Unit
) {
    val context = LocalContext.current
    val themeMode by ThemePreference.mode.collectAsStateWithLifecycle()
    val languageMode by LanguagePreference.mode.collectAsStateWithLifecycle()
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var showNameDialog by rememberSaveable { mutableStateOf(false) }
    var comingSoonFeature by rememberSaveable { mutableStateOf<String?>(null) }

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

        // ── Section: Akun ──
        SectionTitle("Akun")
        KotomichiCard(variant = KotomichiCardVariant.Outlined) {
            Column {
                SettingsRow(
                    icon = Icons.Filled.CameraAlt,
                    title = "Foto Profil",
                    value = userName.ifBlank { "Pengguna" },
                    onClick = { comingSoonFeature = "Ubah Foto Profil" }
                )
                HorizontalDivider()
                SettingsRow(
                    icon = Icons.Filled.Person,
                    title = "Nama",
                    value = userName.ifBlank { "Pengguna" },
                    onClick = { showNameDialog = true }
                )
                HorizontalDivider()
                SettingsRow(
                    icon = Icons.Filled.Email,
                    title = "Email",
                    value = userEmail.ifBlank { "-" },
                    enabled = false
                )
            }
        }

        // ── Section: Umum ──
        SectionTitle("Umum")
        KotomichiCard(variant = KotomichiCardVariant.Outlined) {
            Column {
                SettingsRow(
                    icon = Icons.Filled.Palette,
                    title = "Tema",
                    value = themeLabel(themeMode),
                    onClick = { showThemeDialog = true }
                )
                HorizontalDivider()
                SettingsRow(
                    icon = Icons.Filled.Language,
                    title = "Bahasa",
                    value = languageLabel(languageMode),
                    onClick = { showLanguageDialog = true }
                )
            }
        }

        // ── Logout di paling bawah ──
        KotomichiCard(
            variant = KotomichiCardVariant.Outlined,
            onClick = { showLogoutDialog = true },
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

    comingSoonFeature?.let { feature ->
        KotomichiDialog(
            title = feature,
            text = "Fitur ini akan hadir segera.",
            confirmLabel = "OK",
            dismissLabel = "Tutup",
            onConfirm = { comingSoonFeature = null },
            onDismiss = { comingSoonFeature = null }
        )
    }

    if (showThemeDialog) {
        OptionSelectionDialog(
            title = "Pilih Tema",
            options = listOf(
                ThemePreference.MODE_DARK to "Gelap",
                ThemePreference.MODE_LIGHT to "Terang",
                ThemePreference.MODE_SYSTEM to "Sistem"
            ),
            current = themeMode,
            onSelect = { mode ->
                showThemeDialog = false
                ThemePreference.setMode(context, mode)
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showLanguageDialog) {
        OptionSelectionDialog(
            title = "Pilih Bahasa",
            options = listOf(
                LanguagePreference.LANG_ID to "Indonesia",
                LanguagePreference.LANG_EN to "English",
                LanguagePreference.LANG_SYSTEM to "Sistem"
            ),
            current = languageMode,
            onSelect = { mode ->
                showLanguageDialog = false
                LanguagePreference.setMode(context, mode)
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showNameDialog) {
        EditNameDialog(
            currentName = userName,
            onSave = onUpdateName,
            onDismiss = { showNameDialog = false }
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = KotomichiSpacing.xs)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = KotomichiSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier
                .size(KotomichiSpacing.xl2)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(KotomichiSpacing.sm)
                .size(KotomichiSpacing.lg)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EditNameDialog(
    currentName: String,
    onSave: suspend (String) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by rememberSaveable(currentName) { mutableStateOf(currentName) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        title = {
            Text(text = "Ubah Nama", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.sm)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Nama") },
                    isError = error != null,
                    supportingText = error?.let { message ->
                        { Text(message, color = MaterialTheme.colorScheme.error) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (isSaving) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Menyimpan…",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isBlank()) {
                        error = "Nama tidak boleh kosong"
                        return@TextButton
                    }
                    if (trimmed == currentName) {
                        onDismiss()
                        return@TextButton
                    }
                    isSaving = true
                    error = null
                    scope.launch {
                        try {
                            onSave(trimmed)
                            isSaving = false
                            onDismiss()
                        } catch (e: Exception) {
                            isSaving = false
                            error = e.message ?: "Gagal menyimpan nama"
                        }
                    }
                },
                enabled = !isSaving
            ) {
                Text("Simpan", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Batal", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

@Composable
private fun OptionSelectionDialog(
    title: String,
    options: List<Pair<String, String>>,
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        title = {
            Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                options.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .selectable(
                                selected = current == mode,
                                onClick = { onSelect(mode) }
                            )
                            .padding(horizontal = KotomichiSpacing.sm, vertical = KotomichiSpacing.md),
                        horizontalArrangement = Arrangement.spacedBy(KotomichiSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = current == mode,
                            onClick = { onSelect(mode) }
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (current == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

private fun themeLabel(mode: String): String = when (mode) {
    ThemePreference.MODE_DARK -> "Gelap"
    ThemePreference.MODE_LIGHT -> "Terang"
    else -> "Sistem"
}

private fun languageLabel(mode: String): String = when (mode) {
    LanguagePreference.LANG_ID -> "Indonesia"
    LanguagePreference.LANG_EN -> "English"
    else -> "Sistem"
}