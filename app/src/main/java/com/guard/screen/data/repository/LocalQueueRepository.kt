package com.guard.screen.data.repository

import com.guard.screen.core.AppResult
import com.guard.screen.data.local.dao.CommandQueueDao
import com.guard.screen.data.local.dao.MediaQueueDao
import com.guard.screen.data.local.entity.CommandQueueItem
import com.guard.screen.data.local.entity.MediaQueueItem
import com.guard.screen.core.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local queue ka unified interface.
 */
@Singleton
class LocalQueueRepository @Inject constructor(
    private val mediaQueueDao: MediaQueueDao,
    private val commandQueueDao: CommandQueueDao
) {

    // ============================================
    // MEDIA QUEUE
    // ============================================

    suspend fun queueMedia(item: MediaQueueItem): AppResult<Long> =
        withContext(Dispatchers.IO) {
            try {
                val id = mediaQueueDao.insert(item)
                Logger.d("LocalQueue", "Media queued: ${item.storagePath}")
                AppResult.Success(id)
            } catch (e: Exception) {
                AppResult.Error(
                    com.guard.screen.core.ErrorType.STORAGE,
                    e.message ?: "Queue failed",
                    e
                )
            }
        }

    suspend fun getMediaQueue(): List<MediaQueueItem> = withContext(Dispatchers.IO) {
        mediaQueueDao.getAll()
    }

    suspend fun getMediaQueueCount(): Int = withContext(Dispatchers.IO) {
        mediaQueueDao.getCount()
    }

    suspend fun removeMediaFromQueue(id: String): AppResult<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                mediaQueueDao.deleteById(id)
                AppResult.Success(true)
            } catch (e: Exception) {
                AppResult.Error(
                    com.guard.screen.core.ErrorType.UNKNOWN,
                    e.message ?: "Delete failed",
                    e
                )
            }
        }

    // ============================================
    // COMMAND QUEUE
    // ============================================

    suspend fun queueCommand(item: CommandQueueItem): AppResult<Long> =
        withContext(Dispatchers.IO) {
            try {
                val id = commandQueueDao.insert(item)
                AppResult.Success(id)
            } catch (e: Exception) {
                AppResult.Error(
                    com.guard.screen.core.ErrorType.STORAGE,
                    e.message ?: "Queue failed",
                    e
                )
            }
        }

    suspend fun getCommandQueue(): List<CommandQueueItem> = withContext(Dispatchers.IO) {
        commandQueueDao.getAll()
    }

    suspend fun getCommandQueueCount(): Int = withContext(Dispatchers.IO) {
        commandQueueDao.getCount()
    }

    suspend fun removeCommandFromQueue(id: String): AppResult<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                commandQueueDao.deleteById(id)
                AppResult.Success(true)
            } catch (e: Exception) {
                AppResult.Error(
                    com.guard.screen.core.ErrorType.UNKNOWN,
                    e.message ?: "Delete failed",
                    e
                )
            }
        }

    // ============================================
    // CLEANUP
    // ============================================

    suspend fun clearAllQueues(): AppResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            mediaQueueDao.clearAll()
            commandQueueDao.clearAll()
            Logger.d("LocalQueue", "All queues cleared")
            AppResult.Success(true)
        } catch (e: Exception) {
            AppResult.Error(
                com.guard.screen.core.ErrorType.UNKNOWN,
                e.message ?: "Clear failed",
                e
            )
        }
    }

    /**
     * Failed items clean karo (max retry cross).
     */
    suspend fun cleanFailedItems(maxRetry: Int = 3): AppResult<Int> =
        withContext(Dispatchers.IO) {
            try {
                val beforeMedia = mediaQueueDao.getCount()
                val beforeCommands = commandQueueDao.getCount()

                mediaQueueDao.deleteFailed(maxRetry)
                commandQueueDao.deleteFailed(maxRetry)

                val afterMedia = mediaQueueDao.getCount()
                val afterCommands = commandQueueDao.getCount()

                val removed = (beforeMedia - afterMedia) + (beforeCommands - afterCommands)
                Logger.d("LocalQueue", "Cleaned $removed failed items")
                AppResult.Success(removed)
            } catch (e: Exception) {
                AppResult.Error(
                    com.guard.screen.core.ErrorType.UNKNOWN,
                    e.message ?: "Clean failed",
                    e
                )
            }
        }
}
