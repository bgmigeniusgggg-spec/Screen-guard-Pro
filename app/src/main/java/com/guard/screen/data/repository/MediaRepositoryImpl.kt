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
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val supabaseDS: SupabaseDataSource,
    private val mediaQueueDao: MediaQueueDao
) : MediaRepository {

    override suspend fun uploadMedia(
        deviceKey: String,
        mediaType: String,
        file: File,
        durationSeconds: Int
    ): AppResult<MediaFile> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || file.length() == 0L) {
                return@withContext AppResult.Error(
                    ErrorType.STORAGE,
                    "File empty or missing"
                )
            }

            val extension = file.extension.ifBlank { getExtensionForType(mediaType) }
            val fileName = "${UUID.randomUUID()}.$extension"
            val storagePath = "$deviceKey/$mediaType/$fileName"

            Logger.d("MediaRepo", "Uploading: $storagePath")

            val publicUrl = supabaseDS.uploadFile(
                bucket = Constants.BUCKET_MEDIA,
                storagePath = storagePath,
                file = file
            )

            if (publicUrl == null) {
                queueForRetry(deviceKey, mediaType, file, storagePath, durationSeconds)
                return@withContext AppResult.Error(
                    ErrorType.NETWORK,
                    "Upload failed — queued for retry"
                )
            }

            val request = MediaUploadRequest(
                deviceKey = deviceKey,
                mediaType = mediaType,
                storagePath = storagePath,
                publicUrl = publicUrl,
                fileSize = file.length(),
                durationSeconds = durationSeconds,
                createdAt = getCurrentTimestamp()
            )

            val mediaFile = supabaseDS.insertMedia(request, publicUrl)

            try { file.delete() } catch (_: Exception) {}

            if (mediaFile != null) {
                Logger.i("MediaRepo", "Media uploaded: $publicUrl")
                AppResult.Success(mediaFile)
            } else {
                AppResult.Success(
                    MediaFile(
                        deviceKey = deviceKey,
                        mediaType = mediaType,
                        storagePath = storagePath,
                        publicUrl = publicUrl,
                        fileSize = file.length(),
                        durationSeconds = durationSeconds
                    )
                )
            }
        } catch (e: Exception) {
            Logger.e("MediaRepo", "uploadMedia failed", e)
            AppResult.Error(ErrorType.UNKNOWN, e.message ?: "Upload failed", e)
        }
    }

    override fun observePendingUploads(): Flow<Int> = mediaQueueDao.observeCount()

    private suspend fun queueForRetry(
        deviceKey: String,
        mediaType: String,
        file: File,
        storagePath: String,
        durationSeconds: Int
    ) {
        try {
            mediaQueueDao.insert(
                MediaQueueItem(
                    deviceKey = deviceKey,
                    mediaType = mediaType,
                    filePath = file.absolutePath,
                    storagePath = storagePath,
                    fileSize = file.length(),
                    durationSeconds = durationSeconds
                )
            )
            Logger.d("MediaRepo", "Queued for retry: $storagePath")
        } catch (e: Exception) {
            Logger.e("MediaRepo", "queueForRetry failed", e)
        }
    }

    private fun getExtensionForType(mediaType: String): String {
        return when (mediaType.lowercase()) {
            Constants.MEDIA_TYPE_PHOTO -> "jpg"
            Constants.MEDIA_TYPE_AUDIO -> "m4a"
            else -> "bin"
        }
    }

    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())
    }
}
