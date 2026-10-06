package com.guard.screen.di

import android.content.Context
import androidx.room.Room
import com.guard.screen.data.local.AppDatabase
import com.guard.screen.data.local.dao.MediaQueueDao
import com.guard.screen.data.local.dao.CommandQueueDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DB_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideMediaQueueDao(db: AppDatabase): MediaQueueDao = db.mediaQueueDao()

    @Provides
    @Singleton
    fun provideCommandQueueDao(db: AppDatabase): CommandQueueDao = db.commandQueueDao()
}
