package com.guard.screen.data.repository

import com.guard.screen.core.AppResult
import com.guard.screen.data.local.entity.MediaQueueItem
import com.guard.screen.data.model.MediaFile
import kotlinx.coroutines.flow.Flow

interface UploadRepository {

    /** Queue mein pending uploads fetch karo */
    suspend fun getPendingUploads(limit: Int = 10): List<MediaQueueItem>

    /** Ek queue item ko upload karo */
    suspend fun processQueueItem(item: MediaQueueItem): AppResult<MediaFile>

    /** Queue item delete karo */
    suspend fun removeFromQueue(itemId: String): AppResult<Boolean>

    /** Failed items clean karo */
    suspend fun cleanFailedItems(): AppResult<Int>

    /** Queue count observe karo */
    fun observeQueueCount(): Flow<Int>
}
