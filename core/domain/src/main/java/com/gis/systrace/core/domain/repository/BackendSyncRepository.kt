package com.gis.systrace.core.domain.repository

import com.gis.systrace.core.domain.sync.MetricEventUpload
import com.gis.systrace.core.domain.sync.SyncBatchResult

interface BackendSyncRepository {
    suspend fun runFullSync(): SyncBatchResult
    suspend fun sendHeartbeat(): Boolean
    suspend fun uploadLatestSnapshot(): Boolean
    suspend fun sendPresenceEvent(eventType: String): Boolean
    suspend fun sendEventBatch(events: List<MetricEventUpload>): Int
}
