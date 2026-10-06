package com.guard.screen.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.guard.screen.data.local.entity.CommandQueueItem
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CommandQueueItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CommandQueueItem>)

    @Query("SELECT * FROM command_queue ORDER BY createdAt ASC")
    suspend fun getAll(): List<CommandQueueItem>

    @Query("SELECT * FROM command_queue WHERE retryCount < :maxRetry ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPending(maxRetry: Int = 3, limit: Int = 20): List<CommandQueueItem>

    @Query("SELECT COUNT(*) FROM command_queue")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM command_queue")
    fun observeCount(): Flow<Int>

    @Update
    suspend fun update(item: CommandQueueItem)

    @Query("UPDATE command_queue SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetry(id: String)

    @Delete
    suspend fun delete(item: CommandQueueItem)

    @Query("DELETE FROM command_queue WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM command_queue WHERE retryCount >= :maxRetry")
    suspend fun deleteFailed(maxRetry: Int)

    @Query("DELETE FROM command_queue")
    suspend fun clearAll()
}
