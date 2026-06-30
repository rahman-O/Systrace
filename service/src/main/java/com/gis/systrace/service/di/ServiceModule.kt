package com.gis.systrace.service.di

import com.gis.systrace.core.domain.sync.BackendUploadScheduler
import com.gis.systrace.service.backend.BackendSyncService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {
    @Binds
    @Singleton
    abstract fun bindBackendUploadScheduler(impl: BackendSyncService): BackendUploadScheduler
}
