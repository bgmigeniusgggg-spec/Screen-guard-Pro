package com.guard.screen.data.repository

import com.guard.screen.core.AppResult
import com.guard.screen.data.model.MediaFile
import kotlinx.coroutines.flow.Flow

interface MediaRepository {

    suspend fun uploadMedia(
        deviceKey: String,
        mediaType: String,
        file: java.io.File,
        durationSeconds: Int = 0
    ): AppResult<MediaFile>

    fun observePendingUploads(): Flow<Int>
}
