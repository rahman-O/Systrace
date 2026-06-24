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

    // Memory & Storage
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val internalStorageBytes: Long,
    val externalStorageBytes: Long? = null,
    val freeStorageBytes: Long,

    // Battery
    val batteryPercentage: Int,
    val chargingState: String,
    val batteryHealth: String,
    val batteryTemperatureCelsius: Float,
    val batteryVoltageMillivolts: Int,
    val batteryTechnology: String,
    val batteryCycleCount: Int? = null,

    // Display
    val displayWidthPixels: Int,
    val displayHeightPixels: Int,
    val densityDpi: Int,
    val refreshRateHz: Float,
    val screenSizeInches: Float,

    // Network
    val wifiSsid: String? = null,
    val wifiBssid: String? = null,
    val networkType: String,
    val ipAddresses: List<String>,
    val dnsServers: List<String>,
    val vpnDetected: Boolean,
    val connectionStatus: String,
    val isScreenLocked: Boolean = false,
    val isInternetConnected: Boolean = true,
    val onlineStatus: String = "UNKNOWN",
    val offlineReason: String? = null,

    // Telephony
    val simOperatorName: String? = null,
    val carrierName: String? = null,
    val mcc: String? = null,
    val mnc: String? = null,
    val countryIso: String? = null,
    val hasTelephonyData: Boolean,
    val simSlots: List<SimSlotDetail> = emptyList(),

    // Device identity (MDM)
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

    // Capabilities
    val availableSensors: List<SensorDetail>,
    val installedApplications: List<AppDetail>,
    val systemApplications: List<AppDetail>,

    // Security & MDM
    val rootDetected: Boolean,
    val emulatorDetected: Boolean,
    val developerOptionsEnabled: Boolean,
    val usbDebuggingEnabled: Boolean,
    val deviceOwnerStatus: String,
    val widevineLevel: String? = null,
    val isDeviceOwner: Boolean = false,
    val isProfileOwner: Boolean = false,
    val deviceOwnerComponent: String? = null,
    val organizationName: String? = null,
    val isEncrypted: Boolean? = null,
    val encryptionStatus: String? = null,
    val screenLockType: String? = null,
    val isOemUnlocked: Boolean? = null,
    val googlePlayServicesVersion: String? = null,
    val isWorkProfile: Boolean = false,

    // System info
    val bluetoothName: String? = null,
    val bluetoothEnabled: Boolean? = null,
    val uptimeMillis: Long? = null,
    val timezone: String? = null,
    val locale: String? = null,

    // Location
    val lastKnownLatitude: Double? = null,
    val lastKnownLongitude: Double? = null,
    val locationProvider: String? = null,
    val locationAccuracyMeters: Float? = null,
    val locationTimestampEpochMillis: Long? = null,

    val collectedAtEpochMillis: Long,
) {
    @Serializable
    data class SensorDetail(
        val name: String,
        val vendor: String,
        val version: Int,
        val powerMw: Float,
        val type: Int,
    )

    @Serializable
    data class AppDetail(
        val packageName: String,
        val versionName: String? = null,
        val versionCode: Long? = null,
        val grantedPermissions: List<String> = emptyList(),
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
