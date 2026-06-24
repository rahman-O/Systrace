package com.gis.systrace.presentation

import com.gis.systrace.core.common.DeviceSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DeviceSnapshotCollectorTest {

    @Test
    fun snapshotHasRequiredMdmFields() {
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
            availableRamBytes = 512,
            internalStorageBytes = 10240,
            externalStorageBytes = null,
            freeStorageBytes = 5120,
            batteryPercentage = 50,
            chargingState = "Idle",
            batteryHealth = "Good",
            batteryTemperatureCelsius = 30f,
            batteryVoltageMillivolts = 4000,
            batteryTechnology = "Li-ion",
            displayWidthPixels = 1080,
            displayHeightPixels = 2400,
            densityDpi = 420,
            refreshRateHz = 120f,
            screenSizeInches = 6.1f,
            wifiSsid = null,
            wifiBssid = null,
            networkType = "WIFI",
            ipAddresses = emptyList(),
            dnsServers = emptyList(),
            vpnDetected = false,
            connectionStatus = "Connected",
            simOperatorName = null,
            carrierName = null,
            mcc = null,
            mnc = null,
            countryIso = null,
            hasTelephonyData = false,
            serialNumber = "SERIAL-1",
            androidId = "android-id",
            imeiPrimary = "123456789012345",
            deviceOwnerStatus = "NONE",
            availableSensors = emptyList(),
            installedApplications = emptyList(),
            systemApplications = emptyList(),
            rootDetected = false,
            emulatorDetected = false,
            developerOptionsEnabled = false,
            usbDebuggingEnabled = false,
            lastKnownLatitude = null,
            lastKnownLongitude = null,
            locationProvider = null,
            locationAccuracyMeters = null,
            locationTimestampEpochMillis = null,
            collectedAtEpochMillis = System.currentTimeMillis(),
        )

        assertNotNull(snapshot)
        assertEquals("SERIAL-1", snapshot.serialNumber)
        assertEquals("android-id", snapshot.androidId)
        assertEquals(34, snapshot.sdkVersion)
        assertTrue(snapshot.collectedAtEpochMillis > 0)
    }
}
