package com.kotomichi.ui.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.kotomichi.di.get
import com.kotomichi.app.BuildConfig
import com.kotomichi.model.UserProfile
import com.kotomichi.repository.AuthRepository
import com.kotomichi.repository.DeckRepository
import com.kotomichi.repository.ProgressRepository
import com.kotomichi.repository.SyncRepository
import com.kotomichi.ui.theme.KotomichiSpacing
import kotlinx.coroutines.flow.firstOrNull

@Composable
fun DebugInfoDialog(info: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Debug Info",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { copyDebugInfo(context, info) }) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Salin semua log"
                    )
                }
            }
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
                        text = "Tekan ikon copy untuk menyalin semua log sekaligus.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        },
        dismissButton = {
            TextButton(onClick = { copyDebugInfo(context, info) }) { Text("Salin Semua") }
        }
    )
}

private fun copyDebugInfo(context: Context, text: String) {
    runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("kotomichi-debug", text))
    }
    Toast.makeText(context, "Semua log tersalin ke clipboard", Toast.LENGTH_SHORT).show()
}

suspend fun buildDebugInfo(user: UserProfile?, dueCount: Int, catalog: DeckCatalog): String {
    val deckRepository: DeckRepository = get()
    val progressRepository: ProgressRepository = get()
    val authRepository: AuthRepository = get()
    val syncRepository: SyncRepository = get()

    val token = runCatching { authRepository.getAccessToken() }.getOrNull()
    val vocabCount = runCatching { deckRepository.getVocabularyCount() }.getOrDefault(-1)

    val logs = user?.let { profile ->
        runCatching { progressRepository.getReviewLogs(profile.id, 10000, 0) }.getOrDefault(emptyList())
    } ?: emptyList()
    val reviewCount = logs.size
    val srsCount = user?.let { profile ->
        runCatching { progressRepository.getAllProgress(profile.id).size }.getOrDefault(-1)
    } ?: 0

    val dayMs = 86_400_000L
    val nowMs = System.currentTimeMillis()
    val review7d = logs.count { it.reviewedAt >= nowMs - 7 * dayMs }
    val review30d = logs.count { it.reviewedAt >= nowMs - 30 * dayMs }
    val activeDays365 = logs.map { it.reviewedAt / dayMs }.toSet().size
    val newestLog = logs.maxOfOrNull { it.reviewedAt }
    val oldestLog = logs.minOfOrNull { it.reviewedAt }

    val lastSyncMs = runCatching { syncRepository.observeLastSyncTime().firstOrNull() }.getOrDefault(0L)
    val watermark = runCatching { syncRepository.masterSyncSince() }.getOrNull()
    val pending = runCatching { syncRepository.pendingReviewLogIds() }.getOrDefault(emptyList())

    return buildString {
        appendLine("KOTOMICHI DEBUG")
        appendLine("Versi: ${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})")
        appendLine("diambil: ${fmtDateTime(nowMs)}")
        appendLine("")
        appendLine("== USER ==")
        appendLine("id: ${user?.id ?: "-"}")
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
        appendLine("== KALENDER (aktivitas) ==")
        appendLine("log 7 hari: $review7d")
        appendLine("log 30 hari: $review30d")
        appendLine("hari aktif 365d: $activeDays365")
        appendLine("log terbaru: ${fmtDate(newestLog)}")
        appendLine("log terlama: ${fmtDate(oldestLog)}")
        appendLine("")
        appendLine("== SINKRON ==")
        appendLine("mode: ${if (catalog.isLoading) "loading/skeleton" else if (catalog.isSyncing) "syncing" else "idle"}")
        appendLine("refresh: ${catalog.refreshTick}x")
        appendLine("error: ${catalog.loadError?.ifBlank { "-" } ?: "-"}")
        appendLine("terakhir: ${fmtDateTime(lastSyncMs)}")
        appendLine("diagnostics: ${syncRepository.lastSyncDiagnostics.value.ifBlank { "-" }}")
        appendLine("watermark master: ${if (watermark != null) fmtDateTime(watermark) else "belum ada (full pull)"}")
        appendLine("pending review (belum terkirim): ${pending.size}")
        appendLine("")
        appendLine("== RUNTIME ==")
        appendLine("crash ditangani oleh Firebase Crashlytics (cek console.crashlytics.google.com)")
    }
}

private fun fmtDateTime(ms: Long?): String =
    if (ms == null || ms <= 0L) "-"
    else java.time.Instant.ofEpochMilli(ms)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDateTime()
        .truncatedTo(java.time.temporal.ChronoUnit.MINUTES)
        .toString()

private fun fmtDate(ms: Long?): String =
    if (ms == null || ms <= 0L) "-"
    else java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()