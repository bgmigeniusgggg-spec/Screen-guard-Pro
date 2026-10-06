package com.guard.screen.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Command(
    val id: String? = null,

    @SerialName("device_key")
    val deviceKey: String,

    @SerialName("cmd")
    val cmd: String,

    @SerialName("status")
    val status: String = "pending",

    @SerialName("params")
    val params: Map<String, String>? = null,

    @SerialName("sent_at")
    val sentAt: String? = null,

    @SerialName("executed_at")
    val executedAt: String? = null,

    @SerialName("error_message")
    val errorMessage: String? = null
) {
    fun parse(): Pair<String, String?> {
        val parts = cmd.split(":", limit = 2)
        val base = parts[0].uppercase().trim()
        val param = if (parts.size > 1) parts[1].trim() else null
        return Pair(base, param)
    }
}

@Serializable
data class CommandRequest(
    @SerialName("device_key")
    val deviceKey: String,

    @SerialName("cmd")
    val cmd: String,

    @SerialName("status")
    val status: String = "pending",

    @SerialName("sent_at")
    val sentAt: String
)

@Serializable
data class CommandUpdate(
    @SerialName("status")
    val status: String,

    @SerialName("executed_at")
    val executedAt: String? = null,

    @SerialName("error_message")
    val errorMessage: String? = null
)
