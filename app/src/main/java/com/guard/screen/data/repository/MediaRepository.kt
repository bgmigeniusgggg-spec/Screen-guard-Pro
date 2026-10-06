package com.guard.screen.data.repository

import com.guard.screen.core.AppResult
import com.guard.screen.data.model.MediaFile
import kotlinx.coroutines.flow.Flow

interface MediaRepository {

    /** Media upload karo (file + metadata) */
    suspend fun uploadMedia(
        deviceKey: String,
        mediaType: String,
        file: java.io.File,
        durationSeconds: Int = 0
    ): AppResult<MediaFile>

    /** Live frame upload karo */
    suspend fun uploadLiveFrame(
        deviceKey: String,
        frameBytes: ByteArray,
        frameIndex: Long
    ): AppResult<String>

    /** Purane live frames clean karo */
    suspend fun cleanOldLiveFrames(deviceKey: String): AppResult<Boolean>

    /** Pending uploads count */
    fun observePendingUploads(): Flow<Int>
}
