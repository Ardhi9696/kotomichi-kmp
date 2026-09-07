/**
 * File: ProfilTabGeneralSection.kt
 * Responsibility: Menampilkan section "Umum" di halaman profil (tema, bahasa).
 */
package com.kotomichi.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.kotomichi.ui.components.KotomichiCard
import com.kotomichi.ui.components.KotomichiCardVariant
import com.kotomichi.ui.theme.LanguagePreference
import com.kotomichi.ui.theme.ThemePreference

@Composable
internal fun ProfilTabGeneralSection(
    themeMode: String,
    languageMode: String,
    showThemeDialog: MutableState<Boolean>,
    showLanguageDialog: MutableState<Boolean>
) {
    SectionTitle("Umum")
    KotomichiCard(variant = KotomichiCardVariant.Outlined) {
        Column {
            SettingsRow(
                icon = Icons.Filled.Palette,
                title = "Tema",
                value = themeLabel(themeMode),
                onClick = { showThemeDialog.value = true }
            )
            HorizontalDivider()
            SettingsRow(
                icon = Icons.Filled.Language,
                title = "Bahasa",
                value = languageLabel(languageMode),
                onClick = { showLanguageDialog.value = true }
            )
        }
    }
}

internal fun themeLabel(mode: String): String = when (mode) {
    ThemePreference.MODE_DARK -> "Gelap"
    ThemePreference.MODE_LIGHT -> "Terang"
    else -> "Sistem"
}

internal fun languageLabel(mode: String): String = when (mode) {
    LanguagePreference.LANG_ID -> "Indonesia"
    LanguagePreference.LANG_EN -> "English"
    else -> "Sistem"
}
