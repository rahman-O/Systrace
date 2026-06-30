package com.gis.systrace.core.domain.repository

import com.gis.systrace.core.domain.sync.BackendConnectionConfig
import com.gis.systrace.core.domain.sync.BackendSyncStatus
import kotlinx.coroutines.flow.Flow

interface BackendConfigRepository {
    fun getConfig(): BackendConnectionConfig
    suspend fun saveConfig(config: BackendConnectionConfig)
    fun observeConfig(): Flow<BackendConnectionConfig>
    fun getDeviceId(): String?
    suspend fun saveDeviceId(deviceId: String)
    fun observeSyncStatus(): Flow<BackendSyncStatus>
    suspend fun updateSyncStatus(status: BackendSyncStatus)

    /** Merges MDM-provisioned settings from Headwind launcher when available. Returns true if config changed. */
    suspend fun applyMdmSettingsIfAvailable(): Boolean
}
