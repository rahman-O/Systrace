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
            totalRamBytes = 8_000_000_000,
            internalStorageBytes = 128_000_000_000,
            displayWidthPixels = 1080,
            displayHeightPixels = 2400,
            densityDpi = 420,
            refreshRateHz = 120f,
            screenSizeInches = 6.4f,
            hasTelephonyData = false,
            collectedAtEpochMillis = 123L,
        )

        assertEquals("Google", snapshot.deviceManufacturer)
        assertEquals(34, snapshot.sdkVersion)
        assertTrue(snapshot.totalRamBytes > 0)
    }

    @Test
    fun resultWrapper_handlesSuccessAndError() {
        val success = ResultWrapper.Success(42)
        val error = ResultWrapper.Error(Failure.Unknown)

        assertEquals(42, success.value)
        assertEquals(Failure.Unknown, error.failure)
    }
}
