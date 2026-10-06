package com.guard.screen.data.repository

import com.guard.screen.core.AppResult
import com.guard.screen.data.model.Command
import kotlinx.coroutines.flow.Flow

interface CommandRepository {

    /** Pending commands fetch karo */
    suspend fun fetchPendingCommands(deviceKey: String): AppResult<List<Command>>

    /** Command status update karo */
    suspend fun updateCommandStatus(
        commandId: String,
        status: String,
        errorMessage: String? = null
    ): AppResult<Boolean>

    /** Command status updates ka Flow */
    fun observePendingCount(): Flow<Int>

    /** Local command queue se commands fetch karo (offline) */
    suspend fun getQueuedCommands(): List<Command>
}
