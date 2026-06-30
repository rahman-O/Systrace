package com.gis.systrace.presentation

import com.gis.systrace.core.common.DeviceSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StaticDeviceSnapshotTest {

    @Test
    fun snapshotHasRequiredIdentityFields() {
        val snapshot = DeviceSnapshot(
            deviceManufacturer = "Test",
            deviceBrand = "Test",
            deviceModel = "Test",
            deviceProduct = "Test",
            deviceName = "Test",
            hardware = "Test",
            board = "Test",
            bootloader = "Test",
            fingerprint = "Test",
            buildInfo = emptyMap(),
            androidVersion = "14",
            sdkVersion = 34,
            securityPatch = "2024-01-01",
            buildNumber = "TEST",
            kernelVersion = "6.1.0",
            systemProperties = emptyMap(),
            totalRamBytes = 1024,
            internalStorageBytes = 10240,
            displayWidthPixels = 1080,
            displayHeightPixels = 2400,
            densityDpi = 420,
            refreshRateHz = 120f,
            screenSizeInches = 6.1f,
            hasTelephonyData = false,
            serialNumber = "SERIAL-1",
            androidId = "android-id",
            imeiPrimary = "123456789012345",
            androidVersionDisplay = "14",
            googlePlaySystemUpdate = "2024-06-01",
            basebandVersion = "g5123b-12345",
            simStatusSummary = "SIM 0: READY",
            lastKnownLatitude = 24.7136,
            lastKnownLongitude = 46.6753,
            locationProvider = "gps",
            locationAccuracyMeters = 10f,
            locationTimestampEpochMillis = System.currentTimeMillis(),
            collectedAtEpochMillis = System.currentTimeMillis(),
        )

        assertNotNull(snapshot)
        assertEquals("SERIAL-1", snapshot.serialNumber)
        assertEquals("android-id", snapshot.androidId)
        assertEquals(34, snapshot.sdkVersion)
        assertEquals("14", snapshot.androidVersionDisplay)
        assertEquals("SIM 0: READY", snapshot.simStatusSummary)
        assertTrue(snapshot.collectedAtEpochMillis > 0)
        assertEquals(24.7136, snapshot.lastKnownLatitude)
    }
}
