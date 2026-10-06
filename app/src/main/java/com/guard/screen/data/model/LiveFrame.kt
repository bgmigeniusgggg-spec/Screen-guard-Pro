package com.guard.screen.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LiveFrame(
    val id: String? = null,

    @SerialName("device_key")
    val deviceKey: String,

    @SerialName("frame_path")
    val framePath: String,

    @SerialName("frame_url")
    val frameUrl: String? = null,

    @SerialName("frame_index")
    val frameIndex: Long,

    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class LiveFrameUpload(
    @SerialName("device_key")
    val deviceKey: String,

    @SerialName("frame_path")
    val framePath: String,

    @SerialName("frame_index")
    val frameIndex: Long,

    @SerialName("created_at")
    val createdAt: String
)
