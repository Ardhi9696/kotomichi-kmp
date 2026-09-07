package com.kotomichi.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import java.io.File

object DbFactory {
    private const val DB_NAME = "kotomichi.db"

    // Skema SrsProgress versi sekarang. SQLDelight selalu menandai DB `user_version = 1`
    // selama tidak ada file .sqm, padahal skema lama (sebelum refactor DB) berbeda total
    // (pakai kolom id/state/elapsed_days/due_date, plus tabel-tabel baru belum ada).
    // Karena user_version tidak berubah, AndroidSqliteDriver menganggap DB lama "cocok"
    // dan tidak menjalankan onCreate -> query kolom baru gagal -> crash saat masuk dashboard.
    // Deteksi dengan fingerprint kolom: jika tidak lengkap, DB dibuat ulang (data lama di-backup).
    private val SRS_COLUMNS = listOf(
        "user_id", "vocabulary_id", "direction", "stability", "difficulty",
        "retrievability", "due_at", "last_review_at", "review_count", "lapses",
        "created_at", "updated_at"
    )

    fun create(context: Context): KotomichiDatabase {
        ensureSchemaCompatible(context)
        val driver = AndroidSqliteDriver(
            schema = KotomichiDatabase.Schema,
            context = context,
            name = DB_NAME
        )
        return KotomichiDatabase(driver)
    }

    private fun ensureSchemaCompatible(context: Context) {
        val dbFile = context.getDatabasePath(DB_NAME)
        if (!dbFile.exists()) return

        val isStale = try {
            val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
            if (db == null) {
                true
            } else {
                try {
                    !columnsPresent(db, "SrsProgress", SRS_COLUMNS)
                } finally {
                    db.close()
                }
            }
        } catch (e: Exception) {
            // File rusak / tak bisa dibuka -> build ulang juga.
            true
        }

        if (isStale) {
            rebuildDatabase(dbFile)
        }
    }

    private fun columnsPresent(db: SQLiteDatabase, table: String, columns: List<String>): Boolean {
        val found = try {
            db.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
                val names = HashSet<String>()
                val nameIdx = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) {
                    names.add(cursor.getString(nameIdx))
                }
                names
            }
        } catch (e: Exception) {
            return false
        }
        return found.containsAll(columns)
    }

    private fun rebuildDatabase(dbFile: File) {
        // Backup dulu (file lama disimpan dengan suffix .stale-timestamp), jangan langsung deleted.
        val backup = File(dbFile.parentFile, "kotomichi.db.stale-${System.currentTimeMillis()}")
        // Bersihkan juga journal/WAL sisa proses yang crashing.
        File(dbFile.parentFile, "kotomichi.db-wal").delete()
        File(dbFile.parentFile, "kotomichi.db-shm").delete()
        File(dbFile.parentFile, "kotomichi.db-journal").delete()
        if (!dbFile.renameTo(backup)) {
            dbFile.delete()
        }
    }
}
