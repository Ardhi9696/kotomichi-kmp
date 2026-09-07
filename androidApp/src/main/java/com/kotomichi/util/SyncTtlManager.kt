package com.kotomichi.util

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Pemantau lifecycle aplikasi; pemicu sinkronisasi didelegasikan ke [SyncCoordinator]
 * agar tidak bertabrakan dengan WorkManager/UI (dedup + staleness di satu tempat).
 */
object SyncTtlManager : DefaultLifecycleObserver {

    const val TTL_ON_START_MS = 30 * 60 * 1000L
    const val TTL_ON_RESUME_MS = 5 * 60 * 1000L
    const val TTL_ON_PAUSE_MS = 2 * 60 * 1000L

    private const val PREFS_NAME = "kotomichi_sync_ttl"
    private const val KEY_LAST_SYNC = "last_sync_at"

    private var appContext: Context? = null

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun lastSyncedAt(): Long = prefs().getLong(KEY_LAST_SYNC, 0L)

    fun markSynced(atMillis: Long = System.currentTimeMillis()) {
        prefs().edit().putLong(KEY_LAST_SYNC, atMillis).apply()
    }

    /** Digunakan UI (mis. DeckCatalog) untuk memutuskan perlu-tarik-atau-tidak. */
    fun isStale(ttlMs: Long): Boolean = SyncCoordinator.isStale(ttlMs)

    override fun onStart(owner: LifecycleOwner) {
        SyncCoordinator.request(reason = "onStart", ttlMs = TTL_ON_START_MS, full = true)
    }

    override fun onResume(owner: LifecycleOwner) {
        SyncCoordinator.request(reason = "onResume", ttlMs = TTL_ON_RESUME_MS, full = true)
    }

    override fun onPause(owner: LifecycleOwner) {
        SyncCoordinator.request(reason = "onPause", ttlMs = TTL_ON_PAUSE_MS, full = false)
    }

    private fun prefs() = appContext!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}