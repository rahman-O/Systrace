package com.gis.systrace.data.di

import com.gis.systrace.core.network.client.SysTraceHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideHttpClient(): SysTraceHttpClient = SysTraceHttpClient()
}
