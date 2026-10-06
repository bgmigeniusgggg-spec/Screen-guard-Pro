package com.guard.screen.workers

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.guard.screen.core.Constants
import com.guard.screen.core.Logger
import com.guard.screen.services.GuardService
import com.guard.screen.services.KeepAliveService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Keep-Alive Worker.
 * Har 15 min pe GuardService aur KeepAliveService ko check karta hai.
 */
@HiltWorker
class KeepAliveWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val WORK_NAME = "screenguard_keepalive"

        fun schedule(ctx: Context) {
            try {
                val constraints = Constraints.Builder().build()

                val request = PeriodicWorkRequestBuilder<KeepAliveWorker>(
                    15, TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
                Logger.d("KeepAliveWorker", "Scheduled every 15 min")
            } catch (e: Exception) {
                Logger.e("KeepAliveWorker", "Schedule failed", e)
            }
        }

        fun cancel(ctx: Context) {
            try {
                WorkManager.getInstance(ctx).cancelUniqueWork(WORK_NAME)
            } catch (_: Exception) {}
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Logger.d("KeepAliveWorker", "Running keep-alive check")

            // Setup done check
            val prefs = applicationContext.getSharedPreferences(
                Constants.PREFS_NAME, Context.MODE_PRIVATE
            )
            val setupDone = prefs.getBoolean(Constants.KEY_SETUP_DONE, false)

            if (!setupDone) {
                Logger.d("KeepAliveWorker", "Setup not done — skip")
                return Result.success()
            }

            // Services ensure karo
            ensureServiceRunning(GuardService::class.java)
            ensureServiceRunning(KeepAliveService::class.java)

            Result.success()
        } catch (e: Exception) {
            Logger.e("KeepAliveWorker", "Failed", e)
            Result.retry()
        }
    }

    private fun ensureServiceRunning(serviceClass: Class<*>) {
        try {
            val intent = Intent(applicationContext, serviceClass)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(intent)
            } else {
                applicationContext.startService(intent)
            }
        } catch (e: Exception) {
            Logger.e("KeepAliveWorker", "Service start failed", e)
        }
    }
}
