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
import com.guard.screen.data.repository.DeviceRepository
import com.guard.screen.domain.usecase.RegisterDeviceUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Heartbeat Worker.
 * Har 15 min pe Supabase ko batata hai ki device zinda hai.
 */
@HiltWorker
class HeartbeatWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val deviceRepository: DeviceRepository,
    private val registerDeviceUseCase: RegisterDeviceUseCase
) : CoroutineWorker(context, params) {

    companion object {
        private const val WORK_NAME = "screenguard_heartbeat"

        fun schedule(ctx: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val request = PeriodicWorkRequestBuilder<HeartbeatWorker>(
                    15, TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
                Logger.d("HeartbeatWorker", "Scheduled every 15 min")
            } catch (e: Exception) {
                Logger.e("HeartbeatWorker", "Schedule failed", e)
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
            Logger.d("HeartbeatWorker", "Sending heartbeat")

            val deviceKey = DeviceKey.get(applicationContext)
            val batteryLevel = deviceRepository.getBatteryLevel()
            val isCharging = deviceRepository.isCharging()

            val result = deviceRepository.sendHeartbeat(
                deviceKey = deviceKey,
                batteryLevel = batteryLevel,
                isCharging = isCharging
            )

            when (result) {
                is AppResult.Success -> {
                    Logger.d("HeartbeatWorker", "Heartbeat sent: $batteryLevel%")
                    Result.success()
                }
                is AppResult.Error -> {
                    Logger.w("HeartbeatWorker", "Heartbeat failed: ${result.message}")
                    Result.retry()
                }
                else -> Result.retry()
            }
        } catch (e: Exception) {
            Logger.e("HeartbeatWorker", "Failed", e)
            Result.retry()
        }
    }
}
