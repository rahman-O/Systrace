package com.gis.systrace.data.backend

import android.content.Context
import com.gis.systrace.core.common.AndroidConsoleLogger
import com.gis.systrace.core.domain.repository.BackendConfigRepository
import com.gis.systrace.core.domain.repository.BackendSyncRepository
import com.gis.systrace.core.domain.sync.BackendSyncStatus
import com.gis.systrace.core.domain.sync.MetricEventUpload
import com.gis.systrace.core.domain.sync.SyncBatchResult
import com.gis.systrace.core.network.api.BackendApiPaths
import com.gis.systrace.core.network.client.SysTraceHttpClient
import com.gis.systrace.core.network.dto.ApiEnvelope
import com.gis.systrace.core.network.dto.EventBatchRequestDto
import com.gis.systrace.core.network.dto.EventUploadDto
import com.gis.systrace.core.network.dto.HealthResponseDto
import com.gis.systrace.core.network.dto.HeartbeatRequestDto
import com.gis.systrace.core.network.dto.SnapshotUploadRequestDto
import com.gis.systrace.data.SnapshotRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackendSyncRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configRepository: BackendConfigRepository,
    private val snapshotRepository: SnapshotRepository,
    private val httpClient: SysTraceHttpClient,
) : BackendSyncRepository {

    private val json = httpClient.json

    override suspend fun runFullSync(): SyncBatchResult {
        val config = configRepository.getConfig()
        if (!config.isConfigured) {
            return SyncBatchResult(
                errorMessage = when {
                    config.baseUrl.isBlank() -> "Base URL missing"
                    config.deviceId.isBlank() -> "Device ID not configured"
                    config.apiKey.isBlank() -> "API Key missing"
                    !config.syncEnabled -> "Backend sync disabled"
                    else -> "Backend configuration incomplete"
                },
            )
        }

        val now = System.currentTimeMillis()
        var result = SyncBatchResult()

        return try {
            val heartbeat = sendHeartbeat()
            result = result.copy(heartbeatSent = heartbeat)

            val snapshotOk = uploadLatestSnapshot()
            result = result.copy(snapshotUploaded = snapshotOk)

            configRepository.updateSyncStatus(
                BackendSyncStatus(
                    lastSyncAtEpochMs = now,
                    lastSuccessAtEpochMs = now,
                    lastError = null,
                ),
            )
            AndroidConsoleLogger.d(TAG, "Backend sync OK snapshot=$snapshotOk heartbeat=$heartbeat")
            result
        } catch (t: Throwable) {
            val message = t.message ?: "Unknown sync error"
            configRepository.updateSyncStatus(
                BackendSyncStatus(
                    lastSyncAtEpochMs = now,
                    lastSuccessAtEpochMs = configRepository.observeSyncStatus().first().lastSuccessAtEpochMs,
                    lastError = message,
                ),
            )
            AndroidConsoleLogger.e(TAG, "Backend sync failed", t)
            result.copy(errorMessage = message)
        }
    }

    override suspend fun sendHeartbeat(): Boolean {
        val config = configRepository.getConfig()
        val deviceId = configRepository.getDeviceId() ?: return false
        val snapshot = snapshotRepository.getLatestSnapshot()

        val request = HeartbeatRequestDto(
            deviceId = deviceId,
            timestamp = System.currentTimeMillis(),
            onlineStatus = snapshot?.onlineStatus ?: "UNKNOWN",
            appVersion = DeviceIdentityProvider.appVersion(context),
            batteryPercentage = snapshot?.batteryPercentage,
        )

        val response = httpClient.postJson(
            baseUrl = config.baseUrl,
            path = BackendApiPaths.HEARTBEAT,
            jsonBody = json.encodeToString(request),
            apiKey = config.apiKey,
        )
        return response.isSuccessful
    }

    override suspend fun uploadLatestSnapshot(): Boolean {
        val config = configRepository.getConfig()
        val deviceId = configRepository.getDeviceId() ?: return false
        val snapshot = snapshotRepository.getLatestSnapshot() ?: return false
        val snapshotId = snapshotRepository.getLatestSnapshotKey() ?: return false

        val request = SnapshotUploadRequestDto(
            deviceId = deviceId,
            snapshotId = snapshotId,
            capturedAt = snapshot.collectedAtEpochMillis,
            snapshotJson = json.encodeToString(snapshot),
        )

        val response = httpClient.postJson(
            baseUrl = config.baseUrl,
            path = BackendApiPaths.UPLOAD_SNAPSHOT,
            jsonBody = json.encodeToString(request),
            apiKey = config.apiKey,
        )
        return response.isSuccessful
    }

    override suspend fun sendEventBatch(events: List<MetricEventUpload>): Int {
        if (events.isEmpty()) return 0
        val config = configRepository.getConfig()
        val deviceId = configRepository.getDeviceId() ?: return 0
        val dtos = events.map { ev ->
            EventUploadDto(
                eventId = ev.eventId,
                eventType = ev.eventType,
                eventCategory = ev.eventCategory,
                severity = "INFO",
                timestamp = ev.timestamp,
                source = "systrace",
                title = ev.title.ifBlank { ev.eventType.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() } },
                description = ev.description.ifBlank { "Live metric change" },
                payloadJson = ev.payloadJson,
            )
        }
        val request = EventBatchRequestDto(deviceId = deviceId, events = dtos)
        val response = httpClient.postJson(
            baseUrl = config.baseUrl,
            path = BackendApiPaths.EVENTS_BATCH,
            jsonBody = json.encodeToString(request),
            apiKey = config.apiKey,
        )
        return if (response.isSuccessful) events.size else 0
    }

    override suspend fun sendPresenceEvent(eventType: String): Boolean {
        val config = configRepository.getConfig()
        val deviceId = configRepository.getDeviceId() ?: return false
        val now = System.currentTimeMillis()
        val event = EventUploadDto(
            eventId = "presence-$now",
            eventType = eventType,
            eventCategory = "PRESENCE",
            severity = "INFO",
            timestamp = now,
            source = "systrace",
            title = eventType.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() },
            description = "Network connectivity changed",
        )
        val request = EventBatchRequestDto(
            deviceId = deviceId,
            events = listOf(event),
        )
        val response = httpClient.postJson(
            baseUrl = config.baseUrl,
            path = BackendApiPaths.EVENTS_BATCH,
            jsonBody = json.encodeToString(request),
            apiKey = config.apiKey,
        )
        return response.isSuccessful
    }

    suspend fun testConnection(): Result<String> {
        val config = configRepository.getConfig()
        if (config.baseUrl.isBlank()) {
            return Result.failure(IllegalStateException("Base URL is empty"))
        }
        val response = httpClient.getJson(
            baseUrl = config.baseUrl,
            path = BackendApiPaths.HEALTH,
            apiKey = config.apiKey,
        )
        return if (response.isSuccessful) {
            val status = response.body?.let {
                json.decodeFromString<ApiEnvelope<HealthResponseDto>>(it).data?.status
            } ?: "ok"
            Result.success("Connected: $status (HTTP ${response.code})")
        } else {
            Result.failure(Exception("HTTP ${response.code}: ${response.rawBody}"))
        }
    }

    companion object {
        private const val TAG = "BackendSync"
    }
}
