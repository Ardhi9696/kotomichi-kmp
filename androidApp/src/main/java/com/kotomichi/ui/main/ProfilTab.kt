/**
 * File: ProfilTab.kt
 * Responsibility: Orchestrator halaman profil - mengelola state dialog, memanggil section components,
 *                 dan merender dialog. Menggunakan komponen terpisah untuk setiap section & dialog.
 */
package com.kotomichi.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotomichi.ui.components.KotomichiCard
import com.kotomichi.ui.components.KotomichiCardVariant
import com.kotomichi.ui.components.KotomichiDialog
import com.kotomichi.ui.components.KotomichiPullToRefresh
import com.kotomichi.ui.theme.KotomichiSpacing
import com.kotomichi.ui.theme.LanguagePreference
import com.kotomichi.ui.theme.ThemePreference
import kotlinx.coroutines.launch

@Composable
fun ProfilTab(
    paddingValues: PaddingValues,
    userName: String,
    userEmail: String,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onLogout: () -> Unit,
    onUpdateName: suspend (String) -> Unit
) {
    val context = LocalContext.current
    val themeMode by ThemePreference.mode.collectAsStateWithLifecycle()
    val languageMode by LanguagePreference.mode.collectAsStateWithLifecycle()
    val showLogoutDialog = rememberSaveable { mutableStateOf(false) }
    val showThemeDialog = rememberSaveable { mutableStateOf(false) }
    val showLanguageDialog = rememberSaveable { mutableStateOf(false) }
    val showNameDialog = rememberSaveable { mutableStateOf(false) }
    val comingSoonFeature = rememberSaveable { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        KotomichiPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                ProfilTabAccountSection(
                    userName = userName,
                    userEmail = userEmail,
                    showNameDialog = showNameDialog,
                    comingSoonFeature = comingSoonFeature
                )

                // ── Section: Umum ──
                ProfilTabGeneralSection(
                    themeMode = themeMode,
                    languageMode = languageMode,
                    showThemeDialog = showThemeDialog,
                    showLanguageDialog = showLanguageDialog
                )

                // ── Logout di paling bawah ──
                ProfilTabLogoutCard(onClick = { showLogoutDialog.value = true })
            }
        }
    }

    ProfilTabDialogs(
        showLogoutDialog = showLogoutDialog,
        showThemeDialog = showThemeDialog,
        showLanguageDialog = showLanguageDialog,
        showNameDialog = showNameDialog,
        comingSoonFeature = comingSoonFeature,
        themeMode = themeMode,
        languageMode = languageMode,
        context = context,
        userName = userName,
        onLogout = onLogout,
        onUpdateName = onUpdateName,
        onDismissLogout = { showLogoutDialog.value = false },
        onDismissTheme = { showThemeDialog.value = false },
        onDismissLanguage = { showLanguageDialog.value = false },
        onDismissName = { showNameDialog.value = false },
        onDismissComingSoon = { comingSoonFeature.value = null }
    )
}

@Composable
internal fun ProfilTabDialogs(
    showLogoutDialog: MutableState<Boolean>,
    showThemeDialog: MutableState<Boolean>,
    showLanguageDialog: MutableState<Boolean>,
    showNameDialog: MutableState<Boolean>,
    comingSoonFeature: MutableState<String?>,
    themeMode: String,
    languageMode: String,
    context: android.content.Context,
    userName: String,
    onLogout: () -> Unit,
    onUpdateName: suspend (String) -> Unit,
    onDismissLogout: () -> Unit,
    onDismissTheme: () -> Unit,
    onDismissLanguage: () -> Unit,
    onDismissName: () -> Unit,
    onDismissComingSoon: () -> Unit
) {
    if (showLogoutDialog.value) {
        KotomichiDialog(
            title = "Keluar",
            text = "Yakin ingin keluar dari akun ini?",
            confirmLabel = "Keluar",
            dismissLabel = "Batal",
            isDestructive = true,
            onConfirm = {
                onDismissLogout()
                onLogout()
            },
            onDismiss = onDismissLogout
        )
    }

    comingSoonFeature.value?.let { feature ->
        KotomichiDialog(
            title = feature,
            text = "Fitur ini akan hadir segera.",
            confirmLabel = "OK",
            dismissLabel = "Tutup",
            onConfirm = onDismissComingSoon,
            onDismiss = onDismissComingSoon
        )
    }

    if (showThemeDialog.value) {
        OptionSelectionDialog(
            title = "Pilih Tema",
            options = listOf(
                ThemePreference.MODE_DARK to "Gelap",
                ThemePreference.MODE_LIGHT to "Terang",
                ThemePreference.MODE_SYSTEM to "Sistem"
            ),
            current = themeMode,
            onSelect = { mode ->
                onDismissTheme()
                ThemePreference.setMode(context, mode)
            },
            onDismiss = onDismissTheme
        )
    }

    if (showLanguageDialog.value) {
        OptionSelectionDialog(
            title = "Pilih Bahasa",
            options = listOf(
                LanguagePreference.LANG_ID to "Indonesia",
                LanguagePreference.LANG_EN to "English",
                LanguagePreference.LANG_SYSTEM to "Sistem"
            ),
            current = languageMode,
            onSelect = { mode ->
                onDismissLanguage()
                LanguagePreference.setMode(context, mode)
            },
            onDismiss = onDismissLanguage
        )
    }

    if (showNameDialog.value) {
        EditNameDialog(
            currentName = userName,
            onSave = onUpdateName,
            onDismiss = onDismissName
        )
    }
}