package com.guard.screen.data.repository

import com.guard.screen.core.AppResult
import com.guard.screen.core.Constants
import com.guard.screen.core.ErrorType
import com.guard.screen.core.Logger
import com.guard.screen.data.local.dao.CommandQueueDao
import com.guard.screen.data.local.entity.CommandQueueItem
import com.guard.screen.data.model.Command
import com.guard.screen.data.remote.SupabaseDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandRepositoryImpl @Inject constructor(
    private val supabaseDS: SupabaseDataSource,
    private val commandQueueDao: CommandQueueDao
) : CommandRepository {

    override suspend fun fetchPendingCommands(deviceKey: String): AppResult<List<Command>> =
        withContext(Dispatchers.IO) {
            try {
                val commands = supabaseDS.getPendingCommands(deviceKey)
                AppResult.Success(commands)
            } catch (e: UnknownHostException) {
                AppResult.Error(ErrorType.NETWORK, "No internet connection", e)
            } catch (e: SocketTimeoutException) {
                AppResult.Error(ErrorType.TIMEOUT, "Request timed out", e)
            } catch (e: Exception) {
                Logger.e("CommandRepo", "fetchPendingCommands failed", e)
                AppResult.Error(ErrorType.UNKNOWN, e.message ?: "Unknown error", e)
            }
        }

    override suspend fun updateCommandStatus(
        commandId: String,
        status: String,
        errorMessage: String?
    ): AppResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val success = supabaseDS.updateCommandStatus(commandId, status, errorMessage)

            if (success) {
                // Successful — queue se remove karo (agar hai)
                commandQueueDao.deleteById(commandId)
                AppResult.Success(true)
            } else {
                // Failed — queue mein add karo for retry
                queueCommandUpdate(commandId, status, errorMessage)
                AppResult.Error(ErrorType.NETWORK, "Status update failed — queued")
            }
        } catch (e: Exception) {
            Logger.e("CommandRepo", "updateCommandStatus failed", e)
            // Queue karo retry ke liye
            queueCommandUpdate(commandId, status, errorMessage)
            AppResult.Error(ErrorType.NETWORK, "Queued for retry", e)
        }
    }

    override fun observePendingCount(): Flow<Int> = commandQueueDao.observeCount()

    override suspend fun getQueuedCommands(): List<Command> = withContext(Dispatchers.IO) {
        try {
            commandQueueDao.getPending().map { item ->
                Command(
                    id = item.id,
                    deviceKey = item.deviceKey,
                    cmd = item.cmd,
                    status = item.status,
                    executedAt = item.executedAt.toString(),
                    errorMessage = item.errorMessage
                )
            }
        } catch (e: Exception) {
            Logger.e("CommandRepo", "getQueuedCommands failed", e)
            emptyList()
        }
    }

    // ============================================
    // HELPERS
    // ============================================

    private suspend fun queueCommandUpdate(
        commandId: String,
        status: String,
        errorMessage: String?
    ) {
        try {
            commandQueueDao.insert(
                CommandQueueItem(
                    commandId = commandId,
                    deviceKey = "", // Not needed for update
                    cmd = "",       // Not needed for update
                    status = status,
                    errorMessage = errorMessage,
                    executedAt = System.currentTimeMillis()
                )
            )
            Logger.d("CommandRepo", "Queued command update: $commandId")
        } catch (e: Exception) {
            Logger.e("CommandRepo", "queueCommandUpdate failed", e)
        }
    }
}
