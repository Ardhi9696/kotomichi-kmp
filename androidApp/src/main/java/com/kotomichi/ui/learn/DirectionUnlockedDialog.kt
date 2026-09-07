package com.kotomichi.ui.learn

import androidx.compose.runtime.Composable
import com.kotomichi.model.Direction
import com.kotomichi.ui.components.KotomichiDialog

@Composable
fun DirectionUnlockedDialog(
    direction: Direction,
    onDismiss: () -> Unit
) {
    KotomichiDialog(
        title = "Arah Baru Terbuka!",
        text = "Kamu telah menguasai ${direction.label}\nSekarang bisa belajar: ${Direction.values()[direction.ordinal + 1].label}",
        confirmLabel = "Lanjutkan",
        onConfirm = onDismiss,
        onDismiss = onDismiss
    )
}