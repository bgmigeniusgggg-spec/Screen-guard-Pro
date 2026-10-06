package com.guard.screen.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Offline queue for media uploads.
 * Internet na ho toh yahan save hote hain, jab internet aaye toh upload hote hain.
 */
@Entity(tableName = "media_queue")
data class MediaQueueItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val deviceKey: String,

    val mediaType: String,          // photo, video, audio, screen

    val filePath: String,           // Local file path

    val storagePath: String,        // Target path in Supabase Storage

    val fileSize: Long,

    val durationSeconds: Int = 0,

    val retryCount: Int = 0,

    val lastError: String? = null,

    val createdAt: Long = System.currentTimeMillis(),

    val nextRetryAt: Long = System.currentTimeMillis()
)
