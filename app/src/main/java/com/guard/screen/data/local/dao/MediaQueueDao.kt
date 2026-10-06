package com.guard.screen.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.guard.screen.data.local.entity.MediaQueueItem
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaQueueDao {

    // ============================================
    // INSERT
    // ============================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MediaQueueItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaQueueItem>)

    // ============================================
    // QUERY
    // ============================================

    @Query("SELECT * FROM media_queue ORDER BY createdAt ASC")
    suspend fun getAll(): List<MediaQueueItem>

    @Query("SELECT * FROM media_queue WHERE deviceKey = :deviceKey ORDER BY createdAt ASC")
    suspend fun getByDevice(deviceKey: String): List<MediaQueueItem>

    @Query("SELECT * FROM media_queue WHERE nextRetryAt <= :now ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingRetry(now: Long, limit: Int = 10): List<MediaQueueItem>

    @Query("SELECT * FROM media_queue WHERE id = :id")
    suspend fun getById(id: String): MediaQueueItem?

    @Query("SELECT COUNT(*) FROM media_queue")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM media_queue")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM media_queue ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MediaQueueItem>>

    // ============================================
    // UPDATE
    // ============================================

    @Update
    suspend fun update(item: MediaQueueItem)

    @Query("UPDATE media_queue SET retryCount = retryCount + 1, lastError = :error, nextRetryAt = :nextRetryAt WHERE id = :id")
    suspend fun incrementRetry(id: String, error: String, nextRetryAt: Long)

    // ============================================
    // DELETE
    // ============================================

    @Delete
    suspend fun delete(item: MediaQueueItem)

    @Query("DELETE FROM media_queue WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM media_queue WHERE retryCount >= :maxRetry")
    suspend fun deleteFailed(maxRetry: Int)

    @Query("DELETE FROM media_queue")
    suspend fun clearAll()
}
