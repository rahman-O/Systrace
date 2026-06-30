package com.gis.systrace.core.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DeviceSnapshotSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun snapshot_serializesIdentityAndTelephonyFields() {
        val snapshot = sampleSnapshot()
        val encoded = json.encodeToString(snapshot)
        val decoded = json.decodeFromString<DeviceSnapshot>(encoded)

        assertEquals("SN123", decoded.serialNumber)
        assertEquals("android-id-abc", decoded.androidId)
        assertEquals("111111111111111", decoded.imeiPrimary)
        assertEquals(1, decoded.simSlots.size)
        assertEquals("READY", decoded.simSlots.first().simState)
        assertEquals("user-phone", decoded.userDeviceName)
        assertEquals("14", decoded.androidVersionDisplay)
        assertEquals("2024-06-01", decoded.googlePlaySystemUpdate)
        assertEquals("SIM 0: READY", decoded.simStatusSummary)
    }

    @Test
    fun snapshot_backwardCompatible_deserializesLegacyJson() {
        val legacyJson = """
            {
              "deviceManufacturer": "Google",
              "deviceBrand": "google",
              "deviceModel": "Pixel",
              "deviceProduct": "product",
              "deviceName": "device",
              "hardware": "hw",
              "board": "board",
              "bootloader": "boot",
              "fingerprint": "fp",
              "buildInfo": {},
              "androidVersion": "14",
              "sdkVersion": 34,
              "securityPatch": "2024-01-01",
              "buildNumber": "BUILD",
              "kernelVersion": "6.1",
              "systemProperties": {},
              "totalRamBytes": 8000000000,
              "internalStorageBytes": 128000000000,
              "displayWidthPixels": 1080,
              "displayHeightPixels": 2400,
              "densityDpi": 420,
              "refreshRateHz": 120.0,
              "screenSizeInches": 6.1,
              "hasTelephonyData": false,
              "collectedAtEpochMillis": 1000
            }
        """.trimIndent()

        val decoded = json.decodeFromString<DeviceSnapshot>(legacyJson)
        assertNotNull(decoded)
        assertEquals(null, decoded.serialNumber)
        assertEquals(emptyList<DeviceSnapshot.SimSlotDetail>(), decoded.simSlots)
    }

    private fun sampleSnapshot() = DeviceSnapshot(
        deviceManufacturer = "Samsung",
        deviceBrand = "samsung",
        deviceModel = "SM-G991B",
        deviceProduct = "o1s",
        deviceName = "o1s",
        hardware = "exynos",
        board = "exynos2100",
        bootloader = "unknown",
        fingerprint = "samsung/o1s",
        buildInfo = mapOf("Type" to "user"),
        androidVersion = "14",
        sdkVersion = 34,
        securityPatch = "2024-06-01",
        buildNumber = "UP1A",
        kernelVersion = "5.10",
        systemProperties = mapOf("ro.serialno" to "SN123"),
        totalRamBytes = 8_000_000_000,
        internalStorageBytes = 128_000_000_000,
        displayWidthPixels = 1080,
        displayHeightPixels = 2400,
        densityDpi = 420,
        refreshRateHz = 120f,
        screenSizeInches = 6.2f,
        hasTelephonyData = true,
        simSlots = listOf(
            DeviceSnapshot.SimSlotDetail(
                slotIndex = 0,
                simOperator = "STC",
                simState = "READY",
                networkType = "LTE",
            ),
        ),
        serialNumber = "SN123",
        androidId = "android-id-abc",
        imeiPrimary = "111111111111111",
        userDeviceName = "user-phone",
        androidVersionDisplay = "14",
        googlePlaySystemUpdate = "2024-06-01",
        simStatusSummary = "SIM 0: READY",
        socModel = "Exynos 2100",
        socManufacturer = "Samsung",
        sku = "SKU-1",
        buildCodename = "REL",
        collectedAtEpochMillis = System.currentTimeMillis(),
    )
}
