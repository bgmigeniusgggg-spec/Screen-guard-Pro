package com.guard.screen.workers

import android.content.Context
import com.guard.screen.core.Logger

object WorkerScheduler {

    fun scheduleAll(ctx: Context) {
        try {
            Logger.i("WorkerScheduler", "Scheduling all workers")

            KeepAliveWorker.schedule(ctx)
            HeartbeatWorker.schedule(ctx)
            SyncWorker.schedule(ctx)
            RetryUploadWorker.schedule(ctx)

            Logger.i("WorkerScheduler", "All workers scheduled")
        } catch (e: Exception) {
            Logger.e("WorkerScheduler", "Schedule failed", e)
        }
    }

    fun cancelAll(ctx: Context) {
        try {
            Logger.d("WorkerScheduler", "Cancelling all workers")

            KeepAliveWorker.cancel(ctx)
            HeartbeatWorker.cancel(ctx)
            SyncWorker.cancel(ctx)
            RetryUploadWorker.cancel(ctx)

            Logger.d("WorkerScheduler", "All workers cancelled")
        } catch (e: Exception) {
            Logger.e("WorkerScheduler", "Cancel failed", e)
        }
    }
}
