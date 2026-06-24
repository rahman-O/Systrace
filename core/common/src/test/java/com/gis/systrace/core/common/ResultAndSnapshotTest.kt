package com.gis.systrace.core.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResultAndSnapshotTest {
    @Test
    fun snapshot_retainsValues() {
        val snapshot = DeviceSnapshot(
            deviceManufacturer = "Google",
            deviceBrand = "google",
            deviceModel = "Pixel",
            deviceProduct = "sdk_gphone64",
            deviceName = "panther",
            hardware = "ranchu",
            board = "goldfish",
            bootloader = "unknown",
            fingerprint = "fingerprint",
            buildInfo = mapOf("ID" to "UP1A.231005.007"),
            androidVersion = "14",
            sdkVersion = 34,
            securityPatch = "2026-06-01",
            buildNumber = "UP1A.231005.007",
            kernelVersion = "6.1.0",
            systemProperties = mapOf("ro.debuggable" to "0"),
            totalRamBytes = 1,
            availableRamBytes = 2,
            internalStorageBytes = 3,
            externalStorageBytes = 4,
            freeStorageBytes = 5,
            batteryPercentage = 80,
            chargingState = "Charging",
            batteryHealth = "Good",
            batteryTemperatureCelsius = 32.5f,
            batteryVoltageMillivolts = 4100,
            batteryTechnology = "Li-ion",
            displayWidthPixels = 1080,
            displayHeightPixels = 2400,
            densityDpi = 420,
            refreshRateHz = 120f,
            screenSizeInches = 6.4f,
            networkType = "WIFI",
            ipAddresses = listOf("192.168.1.10"),
            dnsServers = listOf("8.8.8.8"),
            vpnDetected = false,
            connectionStatus = "Connected",
            hasTelephonyData = false,
            availableSensors = emptyList(),
            installedApplications = emptyList(),
            systemApplications = emptyList(),
            rootDetected = false,
            emulatorDetected = true,
            developerOptionsEnabled = false,
            usbDebuggingEnabled = false,
            deviceOwnerStatus = "None",
            collectedAtEpochMillis = 123L,
        )

        assertEquals("Google", snapshot.deviceManufacturer)
        assertEquals(34, snapshot.sdkVersion)
        assertTrue(snapshot.ipAddresses.contains("192.168.1.10"))
    }

    @Test
    fun resultWrapper_handlesSuccessAndError() {
        val success = ResultWrapper.Success(42)
        val error = ResultWrapper.Error(Failure.Unknown)

        assertEquals(42, success.value)
        assertEquals(Failure.Unknown, error.failure)
    }
}
