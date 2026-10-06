package com.guard.screen.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.guard.screen.core.AppResult
import com.guard.screen.core.DeviceKey
import com.guard.screen.core.Logger
import com.guard.screen.domain.usecase.SyncCommandsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Sync Worker.
 * Backup mechanism — agar GuardService ka polling fail ho jaye toh.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncCommandsUseCase: SyncCommandsUseCase
) : CoroutineWorker(context, params) {

    companion object {
        private const val WORK_NAME = "screenguard_sync"

        fun schedule(ctx: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val request = PeriodicWorkRequestBuilder<SyncWorker>(
                    15, TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
                Logger.d("SyncWorker", "Scheduled every 15 min")
            } catch (e: Exception) {
                Logger.e("SyncWorker", "Schedule failed", e)
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
            Logger.d("SyncWorker", "Syncing commands (backup)")

            val deviceKey = DeviceKey.get(applicationContext)
            val result = syncCommandsUseCase(deviceKey)

            when (result) {
                is AppResult.Success -> {
                    Logger.d("SyncWorker", "Processed ${result.data} commands")
                    Result.success()
                }
                is AppResult.Error -> {
                    Logger.w("SyncWorker", "Sync failed: ${result.message}")
                    Result.retry()
                }
                else -> Result.success()
            }
        } catch (e: Exception) {
            Logger.e("SyncWorker", "Failed", e)
            Result.retry()
        }
    }
}
