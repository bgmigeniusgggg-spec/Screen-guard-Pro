package com.guard.screen.domain.repository

import com.guard.screen.core.AppResult
import com.guard.screen.data.model.Command
import com.guard.screen.data.model.DeviceInfo
import com.guard.screen.data.model.MediaFile
import java.io.File
import kotlinx.coroutines.flow.Flow

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
    fun observePendingUploads(): Flow<Int>
}

interface IUploadRepository {
    suspend fun getPendingUploads(limit: Int = 10): List<com.guard.screen.data.local.entity.MediaQueueItem>
    suspend fun processQueueItem(item: com.guard.screen.data.local.entity.MediaQueueItem): AppResult<MediaFile>
    suspend fun removeFromQueue(itemId: String): AppResult<Boolean>
    suspend fun cleanFailedItems(): AppResult<Int>
    fun observeQueueCount(): Flow<Int>
}
