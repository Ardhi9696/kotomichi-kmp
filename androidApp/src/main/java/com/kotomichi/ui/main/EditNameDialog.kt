/**
 * File: EditNameDialog.kt
 * Responsibility: Dialog untuk mengubah nama pengguna (input + validasi + loading state).
 */
package com.kotomichi.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kotomichi.ui.theme.KotomichiSpacing
import kotlinx.coroutines.launch

@Composable
internal fun EditNameDialog(
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
