package com.gis.systrace.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiEnvelope<T>(
    val success: Boolean = true,
    val data: T? = null,
    val message: String? = null,
)

@Serializable
data class HealthResponseDto(
    val status: String? = null,
)

@Serializable
data class HeartbeatRequestDto(
    val deviceId: String,
    val timestamp: Long,
    val onlineStatus: String,
    val appVersion: String? = null,
    val batteryPercentage: Int? = null,
)

@Serializable
data class SnapshotUploadRequestDto(
    val deviceId: String,
    val snapshotId: String,
    val capturedAt: Long,
    val snapshotJson: String,
)

@Serializable
data class EventUploadDto(
    val eventId: String,
    val eventType: String,
    val eventCategory: String = "PRESENCE",
    val severity: String = "INFO",
    val timestamp: Long,
    val source: String = "systrace",
    val title: String,
    val description: String = "",
    val reason: String = "",
    val payloadJson: String = "{}",
    val deviceSnapshotId: String = "",
    val userVisible: Boolean = false,
)

@Serializable
data class EventBatchRequestDto(
    val deviceId: String,
    val events: List<EventUploadDto>,
)
