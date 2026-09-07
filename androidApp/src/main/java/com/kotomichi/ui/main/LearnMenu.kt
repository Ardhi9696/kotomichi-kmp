/**
 * File: LearnMenu.kt
 * Responsibility: Mendefinisikan enum LearnMenu yang merepresentasikan 4 aksi cepat
 *                 pada tab Belajar (Cek Kemampuan, Belajar, Review, Pustaka).
 *                 Setiap menu memiliki icon, judul, dan subjudul.
 */
package com.kotomichi.ui.main

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ReplayCircleFilled
import androidx.compose.material.icons.rounded.LocalLibrary

/** Menu aksi cepat pada hub Belajar. Nantinya tiap menu dibangun menjadi layarnya sendiri. */
enum class LearnMenu(
    val icon: ImageVector,
    val title: String,
    val subtitle: String
) {
    CekKemampuan(
        icon = androidx.compose.material.icons.Icons.Filled.AssignmentTurnedIn,
        title = "Cek Kemampuan",
        subtitle = "Periksa kemampuanmu secara subjektif — menilai sendiri seberapa siap kamu."
    ),
    Belajar(
        icon = androidx.compose.material.icons.Icons.Rounded.School,
        title = "Belajar",
        subtitle = "Pelajari kosakata baru berbasis algoritma kami secara objektif."
    ),
    Review(
        icon = androidx.compose.material.icons.Icons.Rounded.ReplayCircleFilled,
        title = "Review",
        subtitle = "Ulangi kosakata jatuh tempo sesuai spaced repetition."
    ),
    Pustaka(
        icon = androidx.compose.material.icons.Icons.Rounded.LocalLibrary,
        title = "Pustaka",
        subtitle = "Jelajahi daftar kosakata dan deck aktifmu."
    )
}
