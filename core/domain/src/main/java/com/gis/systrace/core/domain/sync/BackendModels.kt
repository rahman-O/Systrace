package com.gis.systrace.core.domain.sync

data class BackendConnectionConfig(
    val baseUrl: String,
    val apiKey: String = "",
    val deviceId: String = "",
    val tenantId: String = "",
    val syncEnabled: Boolean = false,
    val syncIntervalMs: Long = DEFAULT_SYNC_INTERVAL_MS,
) {
    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() &&
            deviceId.isNotBlank() &&
            apiKey.isNotBlank() &&
            syncEnabled

    companion object {
        const val DEFAULT_SYNC_INTERVAL_MS = 60_000L
    }
}

data class BackendSyncStatus(
    val lastSyncAtEpochMs: Long? = null,
    val lastSuccessAtEpochMs: Long? = null,
    val lastError: String? = null,
    val eventsSyncedCount: Int = 0,
    val commandsReceivedCount: Int = 0,
)

data class SyncBatchResult(
    val eventsUploaded: Int = 0,
    val eventsFailed: Int = 0,
    val snapshotUploaded: Boolean = false,
    val commandsReceived: Int = 0,
    val heartbeatSent: Boolean = false,
    val errorMessage: String? = null,
) {
    val isSuccess: Boolean get() = errorMessage == null
}

/** Lightweight metric event for api/v1/events/batch (domain layer). */
data class MetricEventUpload(
    val eventId: String,
    val eventType: String,
    val timestamp: Long,
    val payloadJson: String = "{}",
    val eventCategory: String = "METRICS",
    val title: String = "",
    val description: String = "",
)
