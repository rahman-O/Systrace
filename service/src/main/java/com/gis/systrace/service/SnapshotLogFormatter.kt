package com.gis.systrace.service

import com.gis.systrace.core.common.AndroidConsoleLogger
import com.gis.systrace.core.common.DeviceSnapshot

internal object SnapshotLogFormatter {

    private const val TAG = "SysTraceSync"

    fun logSummary(snapshot: DeviceSnapshot, syncLabel: String) {
        AndroidConsoleLogger.d(TAG, "========== SysTrace background sync ==========")
        AndroidConsoleLogger.d(TAG, syncLabel)
        AndroidConsoleLogger.d(TAG, "collectedAtEpochMillis=${snapshot.collectedAtEpochMillis}")
        AndroidConsoleLogger.d(
            TAG,
            "device=${snapshot.deviceManufacturer} ${snapshot.deviceBrand} ${snapshot.deviceModel}",
        )
        AndroidConsoleLogger.d(
            TAG,
            "android=${snapshot.androidVersion} sdk=${snapshot.sdkVersion} patch=${snapshot.securityPatch}",
        )
        AndroidConsoleLogger.d(TAG, "serial=${snapshot.serialNumber ?: "N/A"}")
        AndroidConsoleLogger.d(TAG, "androidId=${snapshot.androidId ?: "N/A"}")
        AndroidConsoleLogger.d(TAG, "imeiPrimary=${snapshot.imeiPrimary ?: "N/A"}")
        AndroidConsoleLogger.d(TAG, "imeiSecondary=${snapshot.imeiSecondary ?: "N/A"}")
        AndroidConsoleLogger.d(TAG, "iccid=${snapshot.iccid ?: "N/A"}")
        AndroidConsoleLogger.d(TAG, "imsi=${snapshot.imsi ?: "N/A"}")
        AndroidConsoleLogger.d(
            TAG,
            "identifierRestrictions=${snapshot.identifierAvailability ?: "none"}",
        )
        AndroidConsoleLogger.d(
            TAG,
            "battery=${snapshot.batteryPercentage}% ${snapshot.chargingState} ${snapshot.batteryHealth}",
        )
        AndroidConsoleLogger.d(
            TAG,
            "network=${snapshot.networkType} vpn=${snapshot.vpnDetected} carrier=${snapshot.carrierName ?: "N/A"}",
        )
        AndroidConsoleLogger.d(
            TAG,
            "presence=${snapshot.onlineStatus} screenLocked=${snapshot.isScreenLocked} internet=${snapshot.isInternetConnected}",
        )
        snapshot.offlineReason?.let {
            AndroidConsoleLogger.d(TAG, "offlineReason=$it")
        }
        AndroidConsoleLogger.d(
            TAG,
            "ramFreeMb=${snapshot.availableRamBytes / (1024 * 1024)} storageFreeMb=${snapshot.freeStorageBytes / (1024 * 1024)}",
        )
        AndroidConsoleLogger.d(
            TAG,
            "mdmOwner=${snapshot.deviceOwnerStatus} encrypted=${snapshot.isEncrypted} screenLock=${snapshot.screenLockType ?: "N/A"}",
        )
        AndroidConsoleLogger.d(
            TAG,
            "location=${snapshot.lastKnownLatitude},${snapshot.lastKnownLongitude} provider=${snapshot.locationProvider ?: "N/A"}",
        )
        AndroidConsoleLogger.d(TAG, "simSlots=${snapshot.simSlots.size} sensors=${snapshot.availableSensors.size}")
        AndroidConsoleLogger.d(TAG, "==============================================")
    }
}
