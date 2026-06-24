package com.gis.systrace.core.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DeviceSnapshotSerializationTest {

  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun snapshot_serializesNewMdmFields() {
    val snapshot = sampleSnapshot()
    val encoded = json.encodeToString(snapshot)
    val decoded = json.decodeFromString<DeviceSnapshot>(encoded)

    assertEquals("SN123", decoded.serialNumber)
    assertEquals("android-id-abc", decoded.androidId)
    assertEquals("111111111111111", decoded.imeiPrimary)
    assertEquals("DEVICE_OWNER", decoded.deviceOwnerStatus)
    assertEquals(1, decoded.simSlots.size)
    assertEquals("READY", decoded.simSlots.first().simState)
    assertEquals("user-phone", decoded.userDeviceName)
    assertEquals(true, decoded.isEncrypted)
  }

  @Test
  fun snapshot_backwardCompatible_deserializesWithoutNewFields() {
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
        "totalRamBytes": 1,
        "availableRamBytes": 2,
        "internalStorageBytes": 3,
        "freeStorageBytes": 4,
        "batteryPercentage": 80,
        "chargingState": "Charging",
        "batteryHealth": "Good",
        "batteryTemperatureCelsius": 30.0,
        "batteryVoltageMillivolts": 4000,
        "batteryTechnology": "Li-ion",
        "displayWidthPixels": 1080,
        "displayHeightPixels": 2400,
        "densityDpi": 420,
        "refreshRateHz": 120.0,
        "screenSizeInches": 6.1,
        "networkType": "WIFI",
        "ipAddresses": [],
        "dnsServers": [],
        "vpnDetected": false,
        "connectionStatus": "Connected",
        "hasTelephonyData": false,
        "availableSensors": [],
        "installedApplications": [],
        "systemApplications": [],
        "rootDetected": false,
        "emulatorDetected": false,
        "developerOptionsEnabled": false,
        "usbDebuggingEnabled": false,
        "deviceOwnerStatus": "NONE",
        "collectedAtEpochMillis": 1000
      }
    """.trimIndent()

    val decoded = json.decodeFromString<DeviceSnapshot>(legacyJson)
    assertNotNull(decoded)
    assertEquals(null, decoded.serialNumber)
    assertEquals(emptyList<DeviceSnapshot.SimSlotDetail>(), decoded.simSlots)
    assertEquals(false, decoded.isDeviceOwner)
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
    availableRamBytes = 4_000_000_000,
    internalStorageBytes = 128_000_000_000,
    freeStorageBytes = 64_000_000_000,
    batteryPercentage = 75,
    chargingState = "Discharging",
    batteryHealth = "Good",
    batteryTemperatureCelsius = 28f,
    batteryVoltageMillivolts = 3900,
    batteryTechnology = "Li-ion",
    displayWidthPixels = 1080,
    displayHeightPixels = 2400,
    densityDpi = 420,
    refreshRateHz = 120f,
    screenSizeInches = 6.2f,
    networkType = "CELLULAR",
    ipAddresses = listOf("10.0.0.5"),
    dnsServers = listOf("8.8.8.8"),
    vpnDetected = false,
    connectionStatus = "Connected",
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
    availableSensors = emptyList(),
    installedApplications = emptyList(),
    systemApplications = emptyList(),
    rootDetected = false,
    emulatorDetected = false,
    developerOptionsEnabled = false,
    usbDebuggingEnabled = false,
    deviceOwnerStatus = "DEVICE_OWNER",
    isDeviceOwner = true,
    isEncrypted = true,
    encryptionStatus = "ACTIVE",
    collectedAtEpochMillis = System.currentTimeMillis(),
  )
}
