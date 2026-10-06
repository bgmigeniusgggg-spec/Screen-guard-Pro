package com.guard.screen.di

import com.guard.screen.data.repository.CommandRepository
import com.guard.screen.data.repository.CommandRepositoryImpl
import com.guard.screen.data.repository.DeviceRepository
import com.guard.screen.data.repository.DeviceRepositoryImpl
import com.guard.screen.data.repository.MediaRepository
import com.guard.screen.data.repository.MediaRepositoryImpl
import com.guard.screen.data.repository.UploadRepository
import com.guard.screen.data.repository.UploadRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindCommandRepository(
        impl: CommandRepositoryImpl
    ): CommandRepository

    @Binds
    abstract fun bindDeviceRepository(
        impl: DeviceRepositoryImpl
    ): DeviceRepository

    @Binds
    abstract fun bindMediaRepository(
        impl: MediaRepositoryImpl
    ): MediaRepository

    @Binds
    abstract fun bindUploadRepository(
        impl: UploadRepositoryImpl
    ): UploadRepository
}
