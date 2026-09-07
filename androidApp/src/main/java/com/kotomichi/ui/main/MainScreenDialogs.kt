/**
 * File: MainScreenDialogs.kt
 * Responsibility: Merender dialog-dialog (konfirmasi keluar aplikasi, debug info, loading overlay).
 *                 Menangani state visibility untuk setiap dialog.
 */
package com.kotomichi.ui.main

import androidx.compose.runtime.Composable
import com.kotomichi.ui.components.KotomichiDialog
import com.kotomichi.ui.components.KotomichiGlobalLoadingOverlay

@Composable
fun MainScreenDialogs(
    isLoggingOut: Boolean,
    showExitDialog: Boolean,
    showDebug: Boolean,
    debugInfo: String,
    onExitApp: () -> Unit,
    onExitDialogDismiss: () -> Unit,
    onDebugDismiss: () -> Unit
) {
    if (isLoggingOut) {
        KotomichiGlobalLoadingOverlay(isVisible = true)
    }

    if (showExitDialog) {
        KotomichiDialog(
            title = "Keluar Aplikasi",
            text = "Yakin ingin keluar dari aplikasi?",
            confirmLabel = "Keluar",
            dismissLabel = "Batal",
            isDestructive = true,
            onConfirm = {
                onExitDialogDismiss()
                onExitApp()
            },
            onDismiss = onExitDialogDismiss
        )
    }

    if (showDebug) {
        DebugInfoDialog(info = debugInfo, onDismiss = onDebugDismiss)
    }
}
