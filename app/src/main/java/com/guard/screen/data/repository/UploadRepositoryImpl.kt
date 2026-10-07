package com.guard.screen.data.repository

import com.guard.screen.core.AppResult
import com.guard.screen.core.Constants
import com.guard.screen.core.ErrorType
import com.guard.screen.core.Logger
import com.guard.screen.data.local.dao.MediaQueueDao
import com.guard.screen.data.local.entity.MediaQueueItem
import com.guard.screen.data.model.MediaFile
import com.guard.screen.data.model.MediaUploadRequest
import com.guard.screen.data.remote.SupabaseDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadRepositoryImpl @Inject constructor(
    private val supabaseDS: SupabaseDataSource,
    private val mediaQueueDao: MediaQueueDao
) : UploadRepository {

    override suspend fun getPendingUploads(limit: Int): List<MediaQueueItem> =
        withContext(Dispatchers.IO) {
            try {
                mediaQueueDao.getPendingRetry(
                    now = System.currentTimeMillis(),
                    limit = limit
                )
            } catch (e: Exception) {
                Logger.e("UploadRepo", "getPendingUploads failed", e)
                emptyList()
            }
        }

    override suspend fun processQueueItem(item: MediaQueueItem): AppResult<MediaFile> =
        withContext(Dispatchers.IO) {
            try {
                val file = File(item.filePath)

                if (!file.exists()) {
                    Logger.w("UploadRepo", "File not found: ${item.filePath}")
                    mediaQueueDao.deleteById(item.id)
                    return@withContext AppResult.Error(
                        ErrorType.STORAGE,
                        "File not found — removed from queue"
                    )
                }

                // Upload file
                val publicUrl = supabaseDS.uploadFile(
                    bucket = Constants.BUCKET_MEDIA,
                    storagePath = item.storagePath,
                    file = file
                )

                if (publicUrl == null) {
                    val nextRetry = System.currentTimeMillis() + getBackoffDelay(item.retryCount)
                    mediaQueueDao.incrementRetry(item.id, "Upload failed", nextRetry)
                    return@withContext AppResult.Error(ErrorType.NETWORK, "Upload failed")
                }

                // Insert media row
                val request = MediaUploadRequest(
                    deviceKey = item.deviceKey,
                    mediaType = item.mediaType,
                    storagePath = item.storagePath,
                    publicUrl = publicUrl,
                    fileSize = item.fileSize,
                    durationSeconds = item.durationSeconds,
                    createdAt = getCurrentTimestamp()
                )

                // ⭐ Yahan change — publicUrl pass kar
                val mediaFile = supabaseDS.insertMedia(request, publicUrl)

                try { file.delete() } catch (_: Exception) {}

                mediaQueueDao.deleteById(item.id)

                Logger.i("UploadRepo", "Queue item processed: ${item.storagePath}")

                AppResult.Success(
                    mediaFile ?: MediaFile(
                        deviceKey = item.deviceKey,
                        mediaType = item.mediaType,
                        storagePath = item.storagePath,
                        publicUrl = publicUrl,
                        fileSize = item.fileSize,
                        durationSeconds = item.durationSeconds
                    )
                )
            } catch (e: Exception) {
                Logger.e("UploadRepo", "processQueueItem failed", e)
                val nextRetry = System.currentTimeMillis() + getBackoffDelay(item.retryCount)
                mediaQueueDao.incrementRetry(item.id, e.message ?: "Unknown", nextRetry)
                AppResult.Error(ErrorType.UNKNOWN, e.message ?: "Error", e)
            }
        }

    override suspend fun removeFromQueue(itemId: String): AppResult<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                mediaQueueDao.deleteById(itemId)
                AppResult.Success(true)
            } catch (e: Exception) {
                AppResult.Error(ErrorType.UNKNOWN, e.message ?: "Error", e)
            }
        }

    override suspend fun cleanFailedItems(): AppResult<Int> = withContext(Dispatchers.IO) {
        try {
            val count = mediaQueueDao.getCount()
            mediaQueueDao.deleteFailed(Constants.MAX_UPLOAD_RETRY)
            val newCount = mediaQueueDao.getCount()
            val removed = count - newCount
            Logger.d("UploadRepo", "Cleaned $removed failed items")
            AppResult.Success(removed)
        } catch (e: Exception) {
            AppResult.Error(ErrorType.UNKNOWN, e.message ?: "Error", e)
        }
    }

    override fun observeQueueCount(): Flow<Int> = mediaQueueDao.observeCount()

    private fun getBackoffDelay(retryCount: Int): Long {
        return when (retryCount) {
            0 -> 5_000L
            1 -> 30_000L
            2 -> 120_000L
            3 -> 300_000L
            else -> 600_000L
        }
    }

    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())
    }
}
