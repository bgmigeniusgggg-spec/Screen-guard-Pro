package com.guard.screen.workers

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.guard.screen.R
import com.guard.screen.core.Constants
import com.guard.screen.core.Logger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Token Refresh Worker.
 * MediaProjection token 24 ghante mein expire hota hai (Android 14+).
 * Ye worker har 12 ghante check karta hai — agar token purana hai toh notification bhejta hai.
 */
@HiltWorker
class TokenRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val WORK_NAME = "screenguard_token_refresh"

        fun schedule(ctx: Context) {
            try {
                val constraints = Constraints.Builder().build()

                val request = PeriodicWorkRequestBuilder<TokenRefreshWorker>(
                    12, TimeUnit.HOURS
                )
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
                Logger.d("TokenRefreshWorker", "Scheduled every 12 hours")
            } catch (e: Exception) {
                Logger.e("TokenRefreshWorker", "Schedule failed", e)
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
            Logger.d("TokenRefreshWorker", "Checking MediaProjection token")

            val prefs = applicationContext.getSharedPreferences(
                Constants.PREFS_NAME, Context.MODE_PRIVATE
            )

            val tokenTimestamp = prefs.getLong(Constants.KEY_TOKEN_TIMESTAMP, 0L)
            val now = System.currentTimeMillis()

            // Agar token 23 ghante se purana hai ya kabhi nahi liya
            val needsRefresh = tokenTimestamp == 0L ||
                    (now - tokenTimestamp) > Constants.TOKEN_VALIDITY_MS

            if (needsRefresh) {
                Logger.i("TokenRefreshWorker", "Token needs refresh")
                showRefreshNotification()
            } else {
                val ageHours = (now - tokenTimestamp) / (60 * 60 * 1000)
                Logger.d("TokenRefreshWorker", "Token age: ${ageHours}h — OK")
            }

            Result.success()
        } catch (e: Exception) {
            Logger.e("TokenRefreshWorker", "Failed", e)
            Result.retry()
        }
    }

    private fun showRefreshNotification() {
        try {
            val manager = applicationContext.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            val notification = NotificationCompat.Builder(
                applicationContext,
                Constants.CHANNEL_SERVICE
            )
                .setContentTitle("Screen Access Renewal")
                .setContentText("Tap to renew screen recording access")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            manager.notify(Constants.NOTIF_ID_TOKEN_REFRESH, notification)
            Logger.d("TokenRefreshWorker", "Refresh notification shown")
        } catch (e: Exception) {
            Logger.e("TokenRefreshWorker", "Notification failed", e)
        }
    }
}
