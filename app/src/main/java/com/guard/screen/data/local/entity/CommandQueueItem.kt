package com.guard.screen.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "command_queue")
data class CommandQueueItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val commandId: String,
    val deviceKey: String,
    val cmd: String,
    val status: String,
    val errorMessage: String? = null,
    val executedAt: Long,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
