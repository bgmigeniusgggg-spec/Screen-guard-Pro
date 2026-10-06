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
import com.guard.screen.core.Logger
import com.guard.screen.data.repository.UploadRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Retry Upload Worker.
 * Queue mein pending media files ko retry karta hai.
 */
@HiltWorker
class RetryUploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val uploadRepository: UploadRepository
) : CoroutineWorker(context, params) {

    companion object {
        private const val WORK_NAME = "screenguard_retry_upload"
        private const val BATCH_SIZE = 5

        fun schedule(ctx: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val request = PeriodicWorkRequestBuilder<RetryUploadWorker>(
                    15, TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
                Logger.d("RetryUploadWorker", "Scheduled every 15 min")
            } catch (e: Exception) {
                Logger.e("RetryUploadWorker", "Schedule failed", e)
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
            Logger.d("RetryUploadWorker", "Checking pending uploads")

            val pendingItems = uploadRepository.getPendingUploads(limit = BATCH_SIZE)

            if (pendingItems.isEmpty()) {
                Logger.d("RetryUploadWorker", "No pending uploads")
                return Result.success()
            }

            Logger.i("RetryUploadWorker", "Found ${pendingItems.size} pending uploads")

            var successCount = 0
            var failCount = 0

            pendingItems.forEach { item ->
                try {
                    val result = uploadRepository.processQueueItem(item)
                    when (result) {
                        is AppResult.Success -> successCount++
                        is AppResult.Error -> failCount++
                        else -> {}
                    }
                } catch (e: Exception) {
                    Logger.e("RetryUploadWorker", "Item failed", e)
                    failCount++
                }
            }

            Logger.i("RetryUploadWorker", "Success: $successCount, Failed: $failCount")

            // Failed items clean karo (agar max retry cross)
            try {
                uploadRepository.cleanFailedItems()
            } catch (_: Exception) {}

            Result.success()
        } catch (e: Exception) {
            Logger.e("RetryUploadWorker", "Failed", e)
            Result.retry()
        }
    }
}
