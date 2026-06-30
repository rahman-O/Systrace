package com.gis.systrace.data.di

import com.gis.systrace.core.domain.repository.BackendConfigRepository
import com.gis.systrace.core.domain.repository.BackendSyncRepository
import com.gis.systrace.core.domain.usecase.RunBackendSyncUseCase
import com.gis.systrace.data.backend.BackendConfigRepositoryImpl
import com.gis.systrace.data.backend.BackendSyncRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindBackendConfigRepository(impl: BackendConfigRepositoryImpl): BackendConfigRepository

    @Binds
    @Singleton
    abstract fun bindBackendSyncRepository(impl: BackendSyncRepositoryImpl): BackendSyncRepository
}

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    @Singleton
    fun provideRunBackendSyncUseCase(
        backendSyncRepository: BackendSyncRepository,
    ): RunBackendSyncUseCase = RunBackendSyncUseCase(backendSyncRepository)
}
