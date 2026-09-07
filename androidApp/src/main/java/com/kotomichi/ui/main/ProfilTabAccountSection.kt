/**
 * File: ProfilTabAccountSection.kt
 * Responsibility: Menampilkan section "Akun" di halaman profil (foto, nama, email).
 */
package com.kotomichi.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.kotomichi.ui.components.KotomichiCard
import com.kotomichi.ui.components.KotomichiCardVariant

@Composable
internal fun ProfilTabAccountSection(
    userName: String,
    userEmail: String,
    showNameDialog: MutableState<Boolean>,
    comingSoonFeature: MutableState<String?>
) {
    SectionTitle("Akun")
    KotomichiCard(variant = KotomichiCardVariant.Outlined) {
        Column {
            SettingsRow(
                icon = Icons.Filled.CameraAlt,
                title = "Foto Profil",
                value = userName.ifBlank { "Pengguna" },
                onClick = { comingSoonFeature.value = "Ubah Foto Profil" }
            )
            HorizontalDivider()
            SettingsRow(
                icon = Icons.Filled.Person,
                title = "Nama",
                value = userName.ifBlank { "Pengguna" },
                onClick = { showNameDialog.value = true }
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
}
