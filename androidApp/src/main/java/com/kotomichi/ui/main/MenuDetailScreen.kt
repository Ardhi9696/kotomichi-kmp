/**
 * File: MenuDetailScreen.kt
 * Responsibility: Layar placeholder saat menu belajar diklik.
 *                 Hanya menampilkan top bar (LearnMenuTopBar), konten kosong.
 */
package com.kotomichi.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Layar detail menu (placeholder - hanya top bar).
 * @param menu Menu yang sedang aktif
 * @param onBack Callback saat tombol kembali diklik
 */
@Composable
fun MenuDetailScreen(menu: LearnMenu, onBack: () -> Unit) {
    // Content kosong - top bar ditangani oleh LearnMenuTopBar di StudyHubTab
    Box(modifier = Modifier.fillMaxSize()) {
        // Placeholder kosong, LearnMenuTopBar sudah handle navigasi
    }
}