package com.guard.screen.domain.repository

import com.guard.screen.core.AppResult
import com.guard.screen.data.model.Command
import com.guard.screen.data.model.DeviceInfo
import com.guard.screen.data.model.MediaFile
import java.io.File
import kotlinx.coroutines.flow.Flow

/**
 * Domain-level repository contracts.
 * Ye interfaces use cases ko dependency inversion deta hai.
 */

interface ICommandRepository {
    suspend fun fetchPendingCommands(deviceKey: String): AppResult<List<Command>>
    suspend fun updateCommandStatus(
        commandId: String,
        status: String,
        errorMessage: String? = null
    ): AppResult<Boolean>
    fun observePendingCount(): Flow<Int>
}

interface IDeviceRepository {
    suspend fun registerDevice(device: DeviceInfo): AppResult<Boolean>
    suspend fun sendHeartbeat(
        deviceKey: String,
        batteryLevel: Int,
        isCharging: Boolean
    ): AppResult<Boolean>
    suspend fun getBatteryLevel(): Int
    suspend fun isCharging(): Boolean
}

interface IMediaRepository {
    suspend fun uploadMedia(
        deviceKey: String,
        mediaType: String,
        file: File,
        durationSeconds: Int = 0
    ): AppResult<MediaFile>
    suspend fun uploadLiveFrame(
        deviceKey: String,
        frameBytes: ByteArray,
        frameIndex: Long
    ): AppResult<String>
    suspend fun cleanOldLiveFrames(deviceKey: String): AppResult<Boolean>
    fun observePendingUploads(): Flow<Int>
}

interface IUploadRepository {
    suspend fun getPendingUploads(limit: Int = 10): List<com.guard.screen.data.local.entity.MediaQueueItem>
    suspend fun processQueueItem(item: com.guard.screen.data.local.entity.MediaQueueItem): AppResult<MediaFile>
    suspend fun removeFromQueue(itemId: String): AppResult<Boolean>
    suspend fun cleanFailedItems(): AppResult<Int>
    fun observeQueueCount(): Flow<Int>
}
