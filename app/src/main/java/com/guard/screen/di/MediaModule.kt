package com.guard.screen.di

import android.content.Context
import android.media.projection.MediaProjectionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    @Provides
    @Singleton
    fun provideMediaProjectionManager(
        @ApplicationContext context: Context
    ): MediaProjectionManager {
        return context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager
    }
}
