package com.kotomichi.util

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.kotomichi.di.get
import com.kotomichi.usecase.AuthUseCase
import com.kotomichi.usecase.SyncDataUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

object SyncTtlManager : DefaultLifecycleObserver {

    const val TTL_ON_START_MS = 30 * 60 * 1000L
    const val TTL_ON_RESUME_MS = 5 * 60 * 1000L
    const val TTL_ON_PAUSE_MS = 2 * 60 * 1000L

    private const val SYNC_COOLDOWN_MS = 15 * 1000L

    private const val PREFS_NAME = "kotomichi_sync_ttl"
    private const val KEY_LAST_SYNC = "last_sync_at"

    private var appContext: Context? = null
    private var lastAttemptAt = 0L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun lastSyncedAt(): Long = prefs().getLong(KEY_LAST_SYNC, 0L)

    fun markSynced(atMillis: Long = System.currentTimeMillis()) {
        prefs().edit().putLong(KEY_LAST_SYNC, atMillis).apply()
    }

    fun isStale(ttlMs: Long): Boolean {
        val now = System.currentTimeMillis()
        return now - lastSyncedAt() >= ttlMs && now - lastAttemptAt >= SYNC_COOLDOWN_MS
    }

    override fun onStart(owner: LifecycleOwner) {
        requestSync(reason = "onStart", ttlMs = TTL_ON_START_MS, full = true)
    }

    override fun onResume(owner: LifecycleOwner) {
        requestSync(reason = "onResume", ttlMs = TTL_ON_RESUME_MS, full = true)
    }

    override fun onPause(owner: LifecycleOwner) {
        requestSync(reason = "onPause", ttlMs = TTL_ON_PAUSE_MS, full = false)
    }

    private fun requestSync(reason: String, ttlMs: Long, full: Boolean) {
        if (!isStale(ttlMs)) return
        lastAttemptAt = System.currentTimeMillis()
        scope.launch {
            try {
                val authUseCase: AuthUseCase = get()
                val syncUseCase: SyncDataUseCase = get()
                if (!authUseCase.isAuthenticated.first()) return@launch
                val result = if (full) syncUseCase.performFullSync() else syncUseCase.pushUpdates()
                if (result.success) {
                    markSynced()
                    Timber.d("TTL sync [$reason] sukses: ${result.message}")
                } else {
                    Timber.w("TTL sync [$reason] gagal: ${result.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "TTL sync [$reason] error")
            }
        }
    }

    private fun prefs() = appContext!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}