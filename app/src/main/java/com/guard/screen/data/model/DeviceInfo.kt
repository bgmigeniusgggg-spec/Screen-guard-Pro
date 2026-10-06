package com.guard.screen.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfo(
    val id: String? = null,

    @SerialName("device_key")
    val deviceKey: String,

    @SerialName("brand")
    val brand: String,

    @SerialName("model")
    val model: String,

    @SerialName("manufacturer")
    val manufacturer: String,

    @SerialName("android_version")
    val androidVersion: String,

    @SerialName("sdk_int")
    val sdkInt: Int,

    @SerialName("app_version")
    val appVersion: String,

    @SerialName("last_seen")
    val lastSeen: String? = null,

    @SerialName("battery_level")
    val batteryLevel: Int = -1,

    @SerialName("is_charging")
    val isCharging: Boolean = false,

    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class DeviceHeartbeat(
    @SerialName("device_key")
    val deviceKey: String,

    @SerialName("last_seen")
    val lastSeen: String,

    @SerialName("battery_level")
    val batteryLevel: Int,

    @SerialName("is_charging")
    val isCharging: Boolean
)
