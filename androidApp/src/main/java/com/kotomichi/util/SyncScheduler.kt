package com.kotomichi.util

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncScheduler {
    
    fun schedulePeriodicSync(context: Context) {
        val workRequest = PeriodicWorkRequest.Builder(
            SyncWorker::class.java,
            SyncWorker.SYNC_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.MINUTES
            )
            .addTag("sync")
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
    
    fun cancelPeriodicSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(SyncWorker.WORK_NAME)
    }
    
    fun triggerImmediateSync(context: Context) {
        val workRequest = androidx.work.OneTimeWorkRequest.Builder(SyncWorker::class.java)
            .addTag("sync")
            .build()
        
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}