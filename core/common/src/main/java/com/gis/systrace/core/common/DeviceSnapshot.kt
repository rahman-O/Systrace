package com.gis.systrace.core.common

import kotlinx.serialization.Serializable

@Serializable
data class DeviceSnapshot(
    val deviceManufacturer: String,
    val deviceBrand: String,
    val deviceModel: String,
    val deviceProduct: String,
    val deviceName: String,
    val hardware: String,
    val board: String,
    val bootloader: String,
    val fingerprint: String,
    val buildInfo: Map<String, String>,
    val androidVersion: String,
    val sdkVersion: Int,
    val securityPatch: String,
    val buildNumber: String,
    val kernelVersion: String,
    val radioVersion: String? = null,
    val systemProperties: Map<String, String>,

    // CPU & Hardware
    val cpuAbi: List<String> = emptyList(),
    val cpuCores: Int = 0,
    val cpuModel: String? = null,
    val gpuRenderer: String? = null,
    val gpuVendor: String? = null,
    val nfcSupported: Boolean = false,
    val cameraCount: Int = 0,
    val supported32BitAbis: List<String> = emptyList(),
    val supported64BitAbis: List<String> = emptyList(),

    // Memory & Storage (totals only)
    val totalRamBytes: Long,
    val internalStorageBytes: Long,
    val externalStorageBytes: Long? = null,

    // Display
    val displayWidthPixels: Int,
    val displayHeightPixels: Int,
    val densityDpi: Int,
    val refreshRateHz: Float,
    val screenSizeInches: Float,

    // Telephony
    val simOperatorName: String? = null,
    val carrierName: String? = null,
    val mcc: String? = null,
    val mnc: String? = null,
    val countryIso: String? = null,
    val hasTelephonyData: Boolean,
    val simSlots: List<SimSlotDetail> = emptyList(),

    // Device identity
    val serialNumber: String? = null,
    val androidId: String? = null,
    val imeiPrimary: String? = null,
    val imeiSecondary: String? = null,
    val meid: String? = null,
    val iccid: String? = null,
    val imsi: String? = null,
    val phoneNumber: String? = null,
    val identifierAvailability: String? = null,

    // About phone / Build
    val userDeviceName: String? = null,
    val buildType: String? = null,
    val buildTags: String? = null,
    val buildIncremental: String? = null,
    val buildTimeEpochMillis: Long? = null,
    val baseOs: String? = null,
    val previewSdkInt: Int? = null,

    // About phone (Settings-aligned)
    val androidVersionDisplay: String? = null,
    val googlePlaySystemUpdate: String? = null,
    val basebandVersion: String? = null,
    val simStatusSummary: String? = null,
    val socModel: String? = null,
    val socManufacturer: String? = null,
    val sku: String? = null,
    val buildCodename: String? = null,

    // Location
    val lastKnownLatitude: Double? = null,
    val lastKnownLongitude: Double? = null,
    val locationProvider: String? = null,
    val locationAccuracyMeters: Float? = null,
    val locationTimestampEpochMillis: Long? = null,

    // Runtime / dynamic (aligned with snapshot_mapper.go)
    val availableRamBytes: Long? = null,
    val freeStorageBytes: Long? = null,
    val batteryPercentage: Int? = null,
    val batteryHealth: String? = null,
    val chargingState: String? = null,
    val batteryTechnology: String? = null,
    val batteryTemperatureCelsius: Float? = null,
    val batteryVoltageMillivolts: Int? = null,
    val networkType: String? = null,
    val wifiSsid: String? = null,
    val wifiBssid: String? = null,
    val wifiRssiDbm: Int? = null,
    val simSignalDbm: Int? = null,
    val connectionStatus: String? = null,
    val ipAddresses: List<String> = emptyList(),
    val onlineStatus: String = "UNKNOWN",
    val isScreenLocked: Boolean = false,
    val screenOn: Boolean? = null,
    val screenBrightness: Int? = null,
    val screenOrientation: String? = null,
    val isInternetConnected: Boolean = false,
    val offlineReason: String? = null,
    val bluetoothEnabled: Boolean = false,
    val vpnDetected: Boolean = false,
    val uptimeMillis: Long? = null,

    val availableSensors: List<SensorDetail> = emptyList(),

    val collectedAtEpochMillis: Long,
) {
    @Serializable
    data class SensorDetail(
        val name: String = "",
        val type: String = "",
        val vendor: String = "",
    )

    @Serializable
    data class SimSlotDetail(
        val slotIndex: Int,
        val carrierName: String? = null,
        val simOperator: String? = null,
        val mcc: String? = null,
        val mnc: String? = null,
        val countryIso: String? = null,
        val simState: String? = null,
        val networkType: String? = null,
        val isRoaming: Boolean = false,
        val isEmbedded: Boolean = false,
        val subscriptionId: Int? = null,
        val iccid: String? = null,
        val imsi: String? = null,
        val phoneNumber: String? = null,
    )
}
