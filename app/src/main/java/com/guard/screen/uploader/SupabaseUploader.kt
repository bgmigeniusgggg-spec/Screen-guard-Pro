package com.guard.screen.uploader

import android.content.Context
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
class SupabaseUploader @Inject constructor(
    private val context: Context,
    private val supabaseDS: SupabaseDataSource,
    private val mediaQueueDao: MediaQueueDao
) {

    /**
     * File upload karo — agar fail ho toh queue karo.
     */
    suspend fun upload(
        deviceKey: String,
        mediaType: String,
        file: File,
        durationSeconds: Int = 0
    ): AppResult<MediaFile> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || file.length() == 0L) {
                return@withContext AppResult.Error(
                    ErrorType.STORAGE,
                    "File empty or missing"
                )
            }

            val extension = file.extension.ifBlank { getDefaultExtension(mediaType) }
            val fileName = "${UUID.randomUUID()}.$extension"
            val storagePath = "$deviceKey/$mediaType/$fileName"

            Logger.d("SupabaseUploader", "Uploading: $storagePath (${file.length()} bytes)")

            // 1. Upload to Storage
            val publicUrl = supabaseDS.uploadFile(
                bucket = Constants.BUCKET_MEDIA,
                storagePath = storagePath,
                file = file
            )

            if (publicUrl == null) {
                // Failed — queue for retry
                queueForRetry(deviceKey, mediaType, file, storagePath, durationSeconds)
                return@withContext AppResult.Error(
                    ErrorType.NETWORK,
                    "Upload failed — queued for retry"
                )
            }

            // 2. Insert into database
            val request = MediaUploadRequest(
                deviceKey = deviceKey,
                mediaType = mediaType,
                storagePath = storagePath,
                fileSize = file.length(),
                durationSeconds = durationSeconds,
                createdAt = getCurrentTimestamp()
            )

            val mediaFile = supabaseDS.insertMedia(request)

            // 3. Local file delete karo
            try { file.delete() } catch (_: Exception) {}

            Logger.i("SupabaseUploader", "Upload OK: $publicUrl")

            AppResult.Success(
                mediaFile ?: MediaFile(
                    deviceKey = deviceKey,
                    mediaType = mediaType,
                    storagePath = storagePath,
                    publicUrl = publicUrl,
                    fileSize = file.length(),
                    durationSeconds = durationSeconds
                )
            )
        } catch (e: Exception) {
            Logger.e("SupabaseUploader", "Upload failed", e)
            AppResult.Error(ErrorType.UNKNOWN, e.message ?: "Upload failed", e)
        }
    }

    /**
     * Bytes upload karo (live frames).
     */
    suspend fun uploadBytes(
        deviceKey: String,
        frameBytes: ByteArray,
        frameIndex: Long
    ): AppResult<String> = withContext(Dispatchers.IO) {
        try {
            val url = supabaseDS.uploadLiveFrame(deviceKey, frameBytes, frameIndex)
            if (url != null) {
                AppResult.Success(url)
            } else {
                AppResult.Error(ErrorType.NETWORK, "Frame upload failed")
            }
        } catch (e: Exception) {
            AppResult.Error(ErrorType.NETWORK, e.message ?: "Error", e)
        }
    }

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
            Logger.d("SupabaseUploader", "Queued: $storagePath")
        } catch (e: Exception) {
            Logger.e("SupabaseUploader", "Queue failed", e)
        }
    }

    private fun getDefaultExtension(mediaType: String): String {
        return when (mediaType.lowercase()) {
            Constants.MEDIA_TYPE_PHOTO -> "jpg"
            Constants.MEDIA_TYPE_VIDEO -> "mp4"
            Constants.MEDIA_TYPE_AUDIO -> "m4a"
            Constants.MEDIA_TYPE_SCREEN -> "mp4"
            else -> "bin"
        }
    }

    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())
    }
}
