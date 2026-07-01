package com.gis.systrace.data.mdm

import com.gis.systrace.core.common.AndroidConsoleLogger
import com.gis.systrace.core.domain.repository.BackendConfigRepository
import com.gis.systrace.core.domain.usecase.RunBackendSyncUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MdmSettingsObserver @Inject constructor(
    private val configRepository: BackendConfigRepository,
    private val runBackendSync: RunBackendSyncUseCase,
    private val headwindMdmClient: HeadwindMdmClient,
) {
    private var started = false
    private var lastForceConfigUpdateMs = 0L

    fun start(scope: CoroutineScope) {
        if (started) return
        started = true
        scope.launch {
            headwindMdmClient.ensureConnected()
            // Immediate pull after install/boot — do not wait for the first poll interval.
            refreshAndSyncIfNeeded()
            while (isActive) {
                delay(pollIntervalMs())
                refreshAndSyncIfNeeded()
            }
        }
        AndroidConsoleLogger.d(TAG, "MdmSettingsObserver started")
    }

    private fun pollIntervalMs(): Long {
        val config = configRepository.getConfig()
        return if (config.isConfigured) CONFIGURED_POLL_MS else WAITING_FOR_MDM_POLL_MS
    }

    suspend fun refreshAndSyncIfNeeded() {
        var changed = configRepository.applyMdmSettingsIfAvailable()
        var config = configRepository.getConfig()
        if (!config.isConfigured) {
            val now = System.currentTimeMillis()
            if (now - lastForceConfigUpdateMs >= FORCE_CONFIG_COOLDOWN_MS) {
                lastForceConfigUpdateMs = now
                if (headwindMdmClient.forceConfigUpdate()) {
                    delay(LAUNCHER_SYNC_SETTLE_MS)
                    if (configRepository.applyMdmSettingsIfAvailable()) {
                        changed = true
                    }
                    config = configRepository.getConfig()
                }
            }
        }
        if ((changed || config.isConfigured) && config.isConfigured) {
            val result = runBackendSync()
            if (result.isSuccess) {
                AndroidConsoleLogger.d(TAG, "MDM refresh sync OK")
            } else {
                AndroidConsoleLogger.e(TAG, "MDM refresh sync failed: ${result.errorMessage}")
            }
        }
    }

    companion object {
        private const val TAG = "MdmSettingsObserver"
        /** Poll quickly until launcher has written MDM settings (post-enrollment). */
        private const val WAITING_FOR_MDM_POLL_MS = 15_000L
        /** After configured, re-check MDM settings periodically. */
        private const val CONFIGURED_POLL_MS = 60_000L
        /** Avoid hammering launcher when MQTT push is down but HTTP sync is available. */
        private const val FORCE_CONFIG_COOLDOWN_MS = 60_000L
        private const val LAUNCHER_SYNC_SETTLE_MS = 3_000L
    }
}
