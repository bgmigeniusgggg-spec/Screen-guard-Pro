package com.guard.screen.workers

import android.content.Context
import com.guard.screen.core.Logger

/**
 * Saare workers ko ek jagah se schedule/cancel karta hai.
 */
object WorkerScheduler {

    /**
     * Saare workers schedule karo.
     */
    fun scheduleAll(ctx: Context) {
        try {
            Logger.i("WorkerScheduler", "Scheduling all workers")

            KeepAliveWorker.schedule(ctx)
            HeartbeatWorker.schedule(ctx)
            SyncWorker.schedule(ctx)
            RetryUploadWorker.schedule(ctx)
            TokenRefreshWorker.schedule(ctx)

            Logger.i("WorkerScheduler", "All workers scheduled")
        } catch (e: Exception) {
            Logger.e("WorkerScheduler", "Schedule failed", e)
        }
    }

    /**
     * Saare workers cancel karo.
     */
    fun cancelAll(ctx: Context) {
        try {
            Logger.d("WorkerScheduler", "Cancelling all workers")

            KeepAliveWorker.cancel(ctx)
            HeartbeatWorker.cancel(ctx)
            SyncWorker.cancel(ctx)
            RetryUploadWorker.cancel(ctx)
            TokenRefreshWorker.cancel(ctx)

            Logger.d("WorkerScheduler", "All workers cancelled")
        } catch (e: Exception) {
            Logger.e("WorkerScheduler", "Cancel failed", e)
        }
    }
}
