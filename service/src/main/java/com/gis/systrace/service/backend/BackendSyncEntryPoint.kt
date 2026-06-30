package com.gis.systrace.service.backend

import com.gis.systrace.core.domain.sync.BackendUploadScheduler
import com.gis.systrace.data.mdm.MdmSettingsObserver
import com.gis.systrace.service.metrics.MetricChangeTracker
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BackendSyncEntryPoint {
    fun backendSyncService(): BackendSyncService
    fun metricChangeTracker(): MetricChangeTracker
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MdmObserverEntryPoint {
    fun mdmSettingsObserver(): MdmSettingsObserver
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BackendUploadSchedulerEntryPoint {
    fun backendUploadScheduler(): BackendUploadScheduler
}
