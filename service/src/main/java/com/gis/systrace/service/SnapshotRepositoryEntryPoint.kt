package com.gis.systrace.service

import com.gis.systrace.data.SnapshotRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SnapshotRepositoryEntryPoint {
    fun snapshotRepository(): SnapshotRepository
}
