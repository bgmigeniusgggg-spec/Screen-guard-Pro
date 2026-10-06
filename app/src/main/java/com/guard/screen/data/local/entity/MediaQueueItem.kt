package com.guard.screen.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "media_queue")
data class MediaQueueItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val deviceKey: String,
    val mediaType: String,
    val filePath: String,
    val storagePath: String,
    val fileSize: Long,
    val durationSeconds: Int = 0,
    val retryCount: Int = 0,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val nextRetryAt: Long = System.currentTimeMillis()
)
