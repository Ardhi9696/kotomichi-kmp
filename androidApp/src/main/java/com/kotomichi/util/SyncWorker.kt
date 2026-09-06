package com.kotomichi.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kotomichi.usecase.SyncDataUseCase
import com.kotomichi.di.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val syncUseCase: SyncDataUseCase = get()
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Timber.d("Starting background sync")
            val result = syncUseCase.performFullSync()
            
            if (result.success) {
                Timber.d("Background sync completed: ${result.message}")
                Result.success()
            } else {
                Timber.w("Background sync failed: ${result.message}")
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "Background sync error")
            Result.retry()
        }
    }
    
    companion object {
        const val WORK_NAME = "kotomichi_sync_work"
        const val SYNC_INTERVAL_HOURS = 6
    }
}