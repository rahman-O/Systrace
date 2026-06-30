package com.gis.systrace.service.backend

import com.gis.systrace.core.common.AndroidConsoleLogger
import com.gis.systrace.core.domain.repository.BackendConfigRepository
import com.gis.systrace.core.domain.repository.BackendSyncRepository
import com.gis.systrace.core.domain.sync.BackendUploadScheduler
import com.gis.systrace.core.domain.usecase.RunBackendSyncUseCase
import com.gis.systrace.core.domain.sync.MetricEventUpload
import com.gis.systrace.service.metrics.MetricChangeTracker
import com.gis.systrace.service.metrics.PendingMetricEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackendSyncService @Inject constructor(
    private val configRepository: BackendConfigRepository,
    private val backendSyncRepository: BackendSyncRepository,
    private val runBackendSync: RunBackendSyncUseCase,
) : BackendUploadScheduler {
    private var syncJob: Job? = null
    private var uploadDebounceJob: Job? = null
    private var metricBatchJob: Job? = null
    private val pendingMetricEvents = mutableListOf<PendingMetricEvent>()
    private var activeScope: CoroutineScope? = null

    fun start(scope: CoroutineScope) {
        activeScope = scope
        if (syncJob != null) return
        syncJob = scope.launch {
            while (isActive) {
                val config = configRepository.getConfig()
                if (config.isConfigured) {
                    val result = runBackendSync()
                    if (result.isSuccess) {
                        AndroidConsoleLogger.d(
                            TAG,
                            "Sync OK events=${result.eventsUploaded} snapshot=${result.snapshotUploaded} commands=${result.commandsReceived}",
                        )
                    } else {
                        AndroidConsoleLogger.e(TAG, "Sync failed: ${result.errorMessage}")
                    }
                }
                val interval = configRepository.getConfig().syncIntervalMs
                    .coerceAtLeast(MIN_INTERVAL_MS)
                delay(interval)
            }
        }
        AndroidConsoleLogger.d(TAG, "BackendSyncService started")
    }

    fun stop() {
        uploadDebounceJob?.cancel()
        uploadDebounceJob = null
        metricBatchJob?.cancel()
        metricBatchJob = null
        synchronized(pendingMetricEvents) { pendingMetricEvents.clear() }
        syncJob?.cancel()
        syncJob = null
        activeScope = null
        AndroidConsoleLogger.d(TAG, "BackendSyncService stopped")
    }

    override fun scheduleUploadAfterSnapshot() {
        val scope = activeScope ?: return
        uploadDebounceJob?.cancel()
        uploadDebounceJob = scope.launch {
            delay(SNAPSHOT_UPLOAD_DEBOUNCE_MS)
            if (!configRepository.getConfig().isConfigured) return@launch
            val result = runBackendSync()
            if (result.isSuccess) {
                AndroidConsoleLogger.d(TAG, "Post-snapshot upload OK")
            } else {
                AndroidConsoleLogger.e(TAG, "Post-snapshot upload failed: ${result.errorMessage}")
            }
        }
    }

    fun syncNow() {
        val scope = activeScope ?: return
        scope.launch {
            if (!configRepository.getConfig().isConfigured) return@launch
            val result = runBackendSync()
            if (result.isSuccess) {
                AndroidConsoleLogger.d(TAG, "syncNow OK")
            } else {
                AndroidConsoleLogger.e(TAG, "syncNow failed: ${result.errorMessage}")
            }
        }
    }

    fun onConnectivityChanged(hasInternet: Boolean) {
        val scope = activeScope ?: return
        scope.launch {
            if (!configRepository.getConfig().isConfigured) return@launch
            val eventType = if (hasInternet) "DEVICE_ONLINE" else "DEVICE_OFFLINE"
            try {
                backendSyncRepository.sendPresenceEvent(eventType)
            } catch (t: Throwable) {
                AndroidConsoleLogger.e(TAG, "Presence event failed: ${t.message}")
            }
            val result = runBackendSync()
            if (result.isSuccess) {
                AndroidConsoleLogger.d(TAG, "Connectivity sync OK internet=$hasInternet")
            } else {
                AndroidConsoleLogger.e(TAG, "Connectivity sync failed: ${result.errorMessage}")
            }
        }
    }

    fun scheduleMetricEventBatch(events: List<PendingMetricEvent>) {
        if (events.isEmpty()) return
        val scope = activeScope ?: return
        synchronized(pendingMetricEvents) {
            pendingMetricEvents.addAll(events)
        }
        metricBatchJob?.cancel()
        metricBatchJob = scope.launch {
            delay(METRIC_BATCH_DEBOUNCE_MS)
            val batch = synchronized(pendingMetricEvents) {
                if (pendingMetricEvents.isEmpty()) return@launch
                pendingMetricEvents.toList().also { pendingMetricEvents.clear() }
            }
            if (!configRepository.getConfig().isConfigured) return@launch
            val now = System.currentTimeMillis()
            val dtos = batch.mapIndexed { index, pending ->
                MetricChangeTracker.toMetricUpload(pending, now, index)
            }
            try {
                val uploaded = backendSyncRepository.sendEventBatch(dtos)
                AndroidConsoleLogger.d(TAG, "Metric batch uploaded=$uploaded/${dtos.size}")
            } catch (t: Throwable) {
                AndroidConsoleLogger.e(TAG, "Metric batch failed: ${t.message}")
            }
        }
    }

    companion object {
        private const val TAG = "BackendSync"
        private const val MIN_INTERVAL_MS = 15_000L
        private const val SNAPSHOT_UPLOAD_DEBOUNCE_MS = 3_000L
        private const val METRIC_BATCH_DEBOUNCE_MS = 3_000L
    }
}
