package com.gis.systrace.data.backend

import android.content.Context
import android.content.pm.PackageManager
import com.gis.systrace.core.domain.repository.BackendConfigRepository
import com.gis.systrace.core.domain.sync.BackendConnectionConfig
import com.gis.systrace.core.domain.sync.BackendSyncStatus
import com.gis.systrace.data.mdm.MdmSettingsReader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackendConfigRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mdmSettingsReader: MdmSettingsReader,
) : BackendConfigRepository {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val configFlow = MutableStateFlow(loadConfig())
    private val syncStatusFlow = MutableStateFlow(loadSyncStatus())

    override fun getConfig(): BackendConnectionConfig {
        val loaded = loadConfig()
        if (loaded != configFlow.value) {
            configFlow.value = loaded
        }
        return loaded
    }

    override suspend fun saveConfig(config: BackendConnectionConfig) {
        prefs.edit()
            .putString(KEY_BASE_URL, config.baseUrl)
            .putString(KEY_API_KEY, config.apiKey)
            .putString(KEY_DEVICE_ID, config.deviceId)
            .putString(KEY_TENANT_ID, config.tenantId)
            .putBoolean(KEY_SYNC_ENABLED, config.syncEnabled)
            .putLong(KEY_SYNC_INTERVAL, config.syncIntervalMs)
            .apply()
        configFlow.value = config
    }

    override fun observeConfig(): Flow<BackendConnectionConfig> = configFlow.asStateFlow()

    override fun getDeviceId(): String? = getConfig().deviceId.takeIf { it.isNotBlank() }

    override suspend fun saveDeviceId(deviceId: String) {
        saveConfig(getConfig().copy(deviceId = deviceId))
    }

    override fun observeSyncStatus(): Flow<BackendSyncStatus> = syncStatusFlow.asStateFlow()

    override suspend fun updateSyncStatus(status: BackendSyncStatus) {
        prefs.edit()
            .putLong(KEY_LAST_SYNC, status.lastSyncAtEpochMs ?: 0L)
            .putLong(KEY_LAST_SUCCESS, status.lastSuccessAtEpochMs ?: 0L)
            .putString(KEY_LAST_ERROR, status.lastError)
            .apply()
        syncStatusFlow.value = status
    }

    override suspend fun applyMdmSettingsIfAvailable(): Boolean {
        val mdm = mdmSettingsReader.read() ?: return false
        val current = getConfig()
        val merged = current.copy(
            baseUrl = mdm.baseUrl,
            deviceId = mdm.deviceId,
            apiKey = mdm.apiKey,
            syncEnabled = mdm.syncEnabled,
            syncIntervalMs = mdm.syncIntervalMs,
        )
        if (merged == current) return false
        saveConfig(merged)
        return true
    }

    private fun loadConfig(): BackendConnectionConfig {
        return BackendConnectionConfig(
            baseUrl = prefs.getString(KEY_BASE_URL, "") ?: "",
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
            deviceId = prefs.getString(KEY_DEVICE_ID, "") ?: "",
            tenantId = prefs.getString(KEY_TENANT_ID, "") ?: "",
            syncEnabled = prefs.getBoolean(KEY_SYNC_ENABLED, true),
            syncIntervalMs = prefs.getLong(
                KEY_SYNC_INTERVAL,
                BackendConnectionConfig.DEFAULT_SYNC_INTERVAL_MS,
            ),
        )
    }

    private fun loadSyncStatus(): BackendSyncStatus {
        val lastSync = prefs.getLong(KEY_LAST_SYNC, 0L).takeIf { it > 0 }
        val lastSuccess = prefs.getLong(KEY_LAST_SUCCESS, 0L).takeIf { it > 0 }
        return BackendSyncStatus(
            lastSyncAtEpochMs = lastSync,
            lastSuccessAtEpochMs = lastSuccess,
            lastError = prefs.getString(KEY_LAST_ERROR, null),
        )
    }

    companion object {
        private const val PREFS = "backend_config"
        private const val KEY_BASE_URL = "baseUrl"
        private const val KEY_API_KEY = "apiKey"
        private const val KEY_DEVICE_ID = "deviceId"
        private const val KEY_TENANT_ID = "tenantId"
        private const val KEY_SYNC_ENABLED = "syncEnabled"
        private const val KEY_SYNC_INTERVAL = "syncIntervalMs"
        private const val KEY_LAST_SYNC = "lastSyncAt"
        private const val KEY_LAST_SUCCESS = "lastSuccessAt"
        private const val KEY_LAST_ERROR = "lastError"
    }
}

object DeviceIdentityProvider {
    fun appVersion(context: Context): String {
        return try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionName ?: "1.0"
        } catch (_: PackageManager.NameNotFoundException) {
            "1.0"
        }
    }
}
