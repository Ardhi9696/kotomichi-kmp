package com.kotomichi.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.kotomichi.di.get
import com.kotomichi.app.BuildConfig
import com.kotomichi.model.UserProfile
import com.kotomichi.repository.AuthRepository
import com.kotomichi.repository.DeckRepository
import com.kotomichi.repository.ProgressRepository
import com.kotomichi.ui.theme.KotomichiSpacing

@Composable
fun DebugInfoDialog(info: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Debug Info", fontWeight = FontWeight.Bold)
        },
        text = {
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(KotomichiSpacing.sm)) {
                    Text(
                        text = info,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Tekan lama pada teks di atas, lalu pilih \"Copy\" untuk menyalin.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

suspend fun buildDebugInfo(user: UserProfile?, dueCount: Int, catalog: DeckCatalog): String {
    val deckRepository: DeckRepository = get()
    val progressRepository: ProgressRepository = get()
    val authRepository: AuthRepository = get()

    val token = runCatching { authRepository.getAccessToken() }.getOrNull()
    val vocabCount = runCatching { deckRepository.getVocabularyCount() }.getOrDefault(-1)
    val reviewCount = user?.let { profile ->
        runCatching { progressRepository.getReviewLogs(profile.id, 1000, 0).size }.getOrDefault(-1)
    } ?: 0
    val srsCount = user?.let { profile ->
        runCatching { progressRepository.getAllProgress(profile.id).size }.getOrDefault(-1)
    } ?: 0

    return buildString {
        appendLine("KOTOMICHI DEBUG")
        appendLine("Versi: ${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})")
        appendLine("")
        appendLine("== USER ==")
        appendLine("id: ${user?.id?.take(12)}${if ((user?.id?.length ?: 0) > 12) "..." else "-"}")
        appendLine("displayName: ${user?.displayName?.ifBlank { "-" }}")
        appendLine("role: ${user?.role?.name}")
        appendLine("level/exp: ${user?.level}/ ${user?.exp}")
        appendLine("streak: ${user?.currentStreak}")
        appendLine("token: ${if (token.isNullOrBlank()) "TIDAK ADA" else "ada (${token.length} karakter)"}")
        appendLine("")
        appendLine("== DB LOKAL ==")
        appendLine("deck: ${catalog.decks.size}")
        appendLine("vocab: $vocabCount")
        appendLine("review_log: $reviewCount")
        appendLine("srs_progress: $srsCount")
        appendLine("due card: $dueCount")
        appendLine("")
        appendLine("== SINKRON ==")
        appendLine("mode: ${if (catalog.isLoading) "loading/skeleton" else if (catalog.isSyncing) "syncing" else "idle"}")
        appendLine("refresh: ${catalog.refreshTick}x")
        appendLine("error: ${catalog.loadError?.ifBlank { "-" } ?: "-"}")
    }
}