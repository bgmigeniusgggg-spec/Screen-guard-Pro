package com.guard.screen.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Media file model — Supabase ke `media` table se match karta hai.
 */
@Serializable
data class MediaFile(
    val id: String? = null,

    @SerialName("device_key")
    val deviceKey: String,

    @SerialName("media_type")
    val mediaType: String,          // photo, video, audio, screen

    @SerialName("storage_path")
    val storagePath: String,        // Supabase Storage mein path

    @SerialName("public_url")
    val publicUrl: String? = null,

    @SerialName("file_size")
    val fileSize: Long = 0,

    @SerialName("duration_seconds")
    val durationSeconds: Int = 0,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("metadata")
    val metadata: Map<String, String>? = null
)

/**
 * Media upload request — Supabase mein insert ke liye.
 */
@Serializable
data class MediaUploadRequest(
    @SerialName("device_key")
    val deviceKey: String,

    @SerialName("media_type")
    val mediaType: String,

    @SerialName("storage_path")
    val storagePath: String,

    @SerialName("file_size")
    val fileSize: Long,

    @SerialName("duration_seconds")
    val durationSeconds: Int = 0,

    @SerialName("created_at")
    val createdAt: String
)
