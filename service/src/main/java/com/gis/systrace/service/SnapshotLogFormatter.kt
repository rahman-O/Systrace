package com.gis.systrace.service

import com.gis.systrace.core.common.AndroidConsoleLogger
import com.gis.systrace.core.common.DeviceSnapshot

object SnapshotLogFormatter {
    fun logSummary(snapshot: DeviceSnapshot, syncLabel: String) {
        AndroidConsoleLogger.d(
            TAG,
            "$syncLabel model=${snapshot.deviceModel} battery=${snapshot.batteryPercentage}% " +
                "network=${snapshot.networkType} online=${snapshot.onlineStatus}",
        )
    }

    private const val TAG = "SnapshotLog"
}
