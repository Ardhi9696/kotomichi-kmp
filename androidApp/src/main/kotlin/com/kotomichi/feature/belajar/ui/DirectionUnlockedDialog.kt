package com.kotomichi.feature.belajar.ui

import androidx.compose.runtime.Composable
import com.kotomichi.shared.core.model.Direction
import com.kotomichi.core.designsystem.common.KotomichiDialog

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