package com.kotomichi.util

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import timber.log.Timber

/**
 * Menangkap error runtime di HP tanpa harus nyambung ADB/logcat ke PC.
 * - Uncaught exception -> ditulis sebagai crash_*.txt (lengkap dengan tail logcat).
 * - Semua log Timber -> ditulis ke runtime.log.
 * Hasilnya bisa dilihat & disalin lewat Debug Info (dialog debug di Home).
 */
object CrashCatcher {

    private const val MAX_CRASH_FILES = 8
    private const val MAX_RUNTIME_LOG_BYTES = 512 * 1024

    @Volatile
    private var logDir: File? = null
    private var previousHandler: Thread.UncaughtExceptionHandler? = null

    fun install(context: Context) {
        if (logDir != null) return
        val dir = File(context.filesDir, "crash_capture")
        if (!dir.exists()) dir.mkdirs()
        logDir = dir

        Timber.plant(object : Timber.DebugTree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                super.log(priority, tag, message, t)
                writeRuntime("[$priority] $tag: $message" + (t?.let { "\n" + it.stackTraceToString() } ?: ""))
            }
        })

        val prev = Thread.getDefaultUncaughtExceptionHandler()
        previousHandler = prev
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sb = StringBuilder()
                sb.appendLine("KOTOMICHI CRASH ${timestamp()}")
                sb.appendLine("Thread: ${thread.name}")
                sb.appendLine(throwable.stackTraceToString())
                val logcat = grabLogcat()
                if (logcat.isNotBlank()) {
                    sb.appendLine()
                    sb.appendLine("=== LOGCAT (tail) ===")
                    sb.appendLine(logcat)
                }
                writeCrash(sb.toString())
            } catch (ignored: Exception) {
                // Jangan pernah memblokir jalur crash.
            }
            // Laporkan ke Firebase Crashlytics (dipanggil sebelum handler default yg mematikan proses).
            runCatching { FirebaseCrashlytics.getInstance().recordException(throwable) }
            previousHandler?.uncaughtException(thread, throwable)
            // Tak ada handler sebelumnya (langka): jangan biarkan app menggantung.
            if (previousHandler == null) {
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }

    /** Isi file crash terbaru, dipotong agar aman di clipboard. */
    fun lastCrashLog(): String? {
        val dir = logDir ?: return null
        val newest = dir.listFiles()
            ?.filter { it.name.endsWith(".crash.txt") }
            ?.maxByOrNull { it.lastModified() } ?: return null
        return runCatching { newest.readText().take(8000) }.getOrNull()
    }

    /** Tail dari log runtime (log Timber). */
    fun recentRuntimeLog(limitLines: Int = 60): String {
        val dir = logDir ?: return "(log belum aktif)"
        val file = File(dir, "runtime.log")
        if (!file.exists()) return "(belum ada log runtime)"
        return runCatching { file.readLines().takeLast(limitLines).joinToString("\n") }
            .getOrElse { "gagal baca runtime.log: $it" }
    }

    private fun writeCrash(content: String) {
        val dir = logDir ?: return
        runCatching {
            File(dir, "crash_${System.currentTimeMillis()}.crash.txt").writeText(content)
            trimOldCrashFiles(dir)
        }
    }

    private fun writeRuntime(line: String) {
        val dir = logDir ?: return
        runCatching {
            val file = File(dir, "runtime.log")
            if (file.length() > MAX_RUNTIME_LOG_BYTES) {
                // pertahankan separuh baris terakhir agar file tidak membengkak
                val keep = file.readLines().takeLast(2000).joinToString("\n")
                file.writeText(keep + "\n")
            }
            FileWriter(file, true).use { it.write("${timestamp()} $line\n") }
        }
    }

    private fun trimOldCrashFiles(dir: File) {
        runCatching {
            val files = dir.listFiles()?.filter { it.name.endsWith(".crash.txt") }
                ?.sortedByDescending { it.lastModified() } ?: return
            files.drop(MAX_CRASH_FILES).forEach { it.delete() }
        }
    }

    private fun grabLogcat(): String {
        return try {
            val lines = ArrayList<String>()
            val process = ProcessBuilder("logcat", "-d", "-v", "time", "-t", "400")
                .redirectErrorStream(true)
                .start()
            process.inputStream.bufferedReader().use { reader ->
                var line = reader.readLine()
                while (line != null && lines.size < 1200) {
                    if (isRelevant(line)) lines.add(line)
                    line = reader.readLine()
                }
            }
            process.waitFor()
            val relevant = lines.takeLast(160)
            if (relevant.isEmpty()) {
                // tak ada baris relevan -> tampilkan tail mentah supaya tetap ada konteks.
                try {
                    val raw = ArrayList<String>()
                    val p2 = ProcessBuilder("logcat", "-d", "-v", "time", "-t", "80")
                        .redirectErrorStream(true)
                        .start()
                    p2.inputStream.bufferedReader().use { r -> var l = r.readLine(); while (l != null) { raw.add(l); l = r.readLine() } }
                    raw.takeLast(80).joinToString("\n")
                } catch (e: Exception) {
                    ""
                }
            } else {
                relevant.joinToString("\n")
            }
        } catch (e: Exception) {
            "logcat tidak dapat dibaca: ${e.message}"
        }
    }

    private fun isRelevant(line: String): Boolean {
        val l = line.lowercase(Locale.US)
        return l.contains("kotomichi") ||
            l.contains("androidruntime") ||
            l.contains("fatal exception") ||
            l.contains("koin") ||
            l.contains("sqldelight") ||
            l.contains("sqlite") ||
            l.contains("nosuchmethod") ||
            l.contains("noclassdeffound") ||
            l.contains("stacktrace")
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
}