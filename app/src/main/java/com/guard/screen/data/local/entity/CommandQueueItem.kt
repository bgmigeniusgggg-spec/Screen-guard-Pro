package com.guard.screen.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Offline queue for command status updates.
 * Jab command execute ho jaaye, but internet na ho toh status update yahan store hota hai.
 */
@Entity(tableName = "command_queue")
data class CommandQueueItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val commandId: String,          // Supabase command ID

    val deviceKey: String,

    val cmd: String,

    val status: String,             // done, failed

    val errorMessage: String? = null,

    val executedAt: Long,

    val retryCount: Int = 0,

    val createdAt: Long = System.currentTimeMillis()
)
