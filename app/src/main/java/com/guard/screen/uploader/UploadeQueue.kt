package com.guard.screen.uploader

import android.content.Context
import com.guard.screen.core.AppResult
import com.guard.screen.core.Constants
import com.guard.screen.core.Logger
import com.guard.screen.data.local.dao.MediaQueueDao
import com.guard.screen.data.local.entity.MediaQueueItem
import com.guard.screen.data.repository.UploadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadQueue @Inject constructor(
    private val context: Context,
    private val uploadRepository: UploadRepository,
    private val mediaQueueDao: MediaQueueDao
) {

    /**
     * Queue mein pending uploads process karo.
     * @return (successCount, failCount)
     */
    suspend fun processPending(limit: Int = 5): Pair<Int, Int> = withContext(Dispatchers.IO) {
        try {
            val pending = uploadRepository.getPendingUploads(limit)

            if (pending.isEmpty()) {
                Logger.d("UploadQueue", "No pending items")
                return@withContext Pair(0, 0)
            }

            Logger.i("UploadQueue", "Processing ${pending.size} items")

            var success = 0
            var fail = 0

            pending.forEach { item ->
                try {
                    val result = uploadRepository.processQueueItem(item)
                    when (result) {
                        is AppResult.Success -> success++
                        is AppResult.Error -> fail++
                        else -> {}
                    }
                } catch (e: Exception) {
                    Logger.e("UploadQueue", "Item failed", e)
                    fail++
                }
            }

            Logger.i("UploadQueue", "Success: $success, Failed: $fail")
            Pair(success, fail)
        } catch (e: Exception) {
            Logger.e("UploadQueue", "Process failed", e)
            Pair(0, 0)
        }
    }

    /**
     * Failed items clean karo.
     */
    suspend fun cleanFailed(maxRetry: Int = Constants.MAX_UPLOAD_RETRY): Int =
        withContext(Dispatchers.IO) {
            try {
                val before = mediaQueueDao.getCount()
                mediaQueueDao.deleteFailed(maxRetry)
                val after = mediaQueueDao.getCount()
                val removed = before - after
                Logger.d("UploadQueue", "Cleaned $removed failed items")
                removed
            } catch (e: Exception) {
                Logger.e("UploadQueue", "Clean failed", e)
                0
            }
        }

    /**
     * Queue count lo.
     */
    suspend fun getPendingCount(): Int = withContext(Dispatchers.IO) {
        try {
            mediaQueueDao.getCount()
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Saari queue clear karo.
     */
    suspend fun clearAll(): Boolean = withContext(Dispatchers.IO) {
        try {
            mediaQueueDao.clearAll()
            true
        } catch (e: Exception) {
            Logger.e("UploadQueue", "Clear failed", e)
            false
        }
    }
}
