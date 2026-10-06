package com.guard.screen.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.guard.screen.data.local.dao.CommandQueueDao
import com.guard.screen.data.local.dao.MediaQueueDao
import com.guard.screen.data.local.entity.CommandQueueItem
import com.guard.screen.data.local.entity.MediaQueueItem

@Database(
    entities = [
        MediaQueueItem::class,
        CommandQueueItem::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun mediaQueueDao(): MediaQueueDao
    abstract fun commandQueueDao(): CommandQueueDao

    companion object {
        const val DB_NAME = "screenguard.db"
    }
}
