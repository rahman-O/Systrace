package com.gis.systrace.core.common

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.StatFs
import android.provider.Settings
import android.util.DisplayMetrics
import com.gis.systrace.core.common.collectors.BuildInfoCollector
import com.gis.systrace.core.common.collectors.DeviceIdentifiersCollector
import com.gis.systrace.core.common.collectors.DevicePresenceCollector
import com.gis.systrace.core.common.collectors.HardwareCollector
import com.gis.systrace.core.common.collectors.MdmSecurityCollector
import com.gis.systrace.core.common.collectors.TelephonyCollector
import java.io.File

object DeviceCollectorShared {
    @SuppressLint("MissingPermission")
    fun collect(
        context: Context,
        options: CollectOptions = CollectOptions(),
    ): DeviceSnapshot {
        val buildData = BuildInfoCollector.collect(context)
        val identifiers = DeviceIdentifiersCollector.collect(context)
        val telephony = TelephonyCollector.collect(context)
        val security = MdmSecurityCollector.collect(context)
        val hardware = HardwareCollector.collect(
            context,
            includeSensors = options.includeSensors,
        )

        val androidVersion = Build.VERSION.RELEASE ?: "${Build.VERSION.SDK_INT}"
        val sdk = Build.VERSION.SDK_INT
        val securityPatch = try { Build.VERSION.SECURITY_PATCH ?: "" } catch (t: Throwable) { "" }
        val buildNumber = Build.DISPLAY ?: Build.ID
        val kernelVersion = System.getProperty("os.version") ?: ""

        // Memory
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val totalRam = try { memInfo.totalMem } catch (t: Throwable) { 0L }
        val availRam = try { memInfo.availMem } catch (t: Throwable) { 0L }

        // Storage (internal)
        val dataDir = context.filesDir ?: File("/")
        val stat = StatFs(dataDir.absolutePath)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availBlocks = stat.availableBlocksLong
        val internalTotal = blockSize * totalBlocks
        val internalFree = blockSize * availBlocks

        // Battery
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val batteryPct = batteryIntent?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) (level * 100) / scale else -1
        } ?: -1
        val voltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val temperatureC = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)?.let { it / 10f } ?: -1f

        // Display
        val metrics: DisplayMetrics = context.resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val densityDpi = metrics.densityDpi
        val refreshRate = try {
            if (Build.VERSION.SDK_INT >= 17) context.display?.refreshRate ?: 60f else 60f
        } catch (e: Throwable) { 60f }
        val screenInches = try {
            val x = width.toDouble() / metrics.xdpi
            val y = height.toDouble() / metrics.ydpi
            Math.hypot(x, y).toFloat()
        } catch (t: Throwable) { 0f }

        // Network
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = try { connectivity?.activeNetwork } catch (t: Throwable) { null }
        val nc = try { activeNetwork?.let { connectivity?.getNetworkCapabilities(it) } } catch (t: Throwable) { null }
        val networkType = when {
            nc == null -> "UNKNOWN"
            nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            nc.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "OTHER"
        }
        val presence = DevicePresenceCollector.collect(context)

        // Applications (optional — slow on background thread)
        val installed = if (options.includeInstalledApps) {
            val pm = context.packageManager
            try {
                pm.getInstalledPackages(0).map { pkg ->
                    val versionCode = if (Build.VERSION.SDK_INT >= 28) {
                        pkg.longVersionCode
                    } else {
                        pkg.versionCode.toLong()
                    }
                    DeviceSnapshot.AppDetail(pkg.packageName, pkg.versionName, versionCode)
                }
            } catch (t: Throwable) {
                emptyList()
            }
        } else {
            emptyList()
        }

        val systemApps = if (options.includeInstalledApps) {
            installed.filter { it.packageName.startsWith("com.android") }
        } else {
            emptyList()
        }

        val fingerprintStr = Build.FINGERPRINT ?: ""
        val emulatorDetected = (Build.FINGERPRINT?.contains("generic") == true) ||
            (Build.MODEL?.contains("Emulator") == true)
        val developerOptions = try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
        } catch (t: Throwable) { false }
        val usbDebug = try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        } catch (t: Throwable) { false }

        return DeviceSnapshot(
            deviceManufacturer = Build.MANUFACTURER ?: "",
            deviceBrand = Build.BRAND ?: "",
            deviceModel = Build.MODEL ?: "",
            deviceProduct = Build.PRODUCT ?: "",
            deviceName = Build.DEVICE ?: "",
            hardware = Build.HARDWARE ?: "",
            board = Build.BOARD ?: "",
            bootloader = Build.BOOTLOADER ?: "",
            fingerprint = fingerprintStr,
            buildInfo = buildData.buildInfo,
            androidVersion = androidVersion,
            sdkVersion = sdk,
            securityPatch = securityPatch,
            buildNumber = buildNumber,
            kernelVersion = kernelVersion,
            systemProperties = buildData.systemProperties,

            cpuAbi = hardware.cpuAbi,
            cpuCores = hardware.cpuCores,
            cpuModel = hardware.cpuModel,
            gpuRenderer = hardware.gpuRenderer,
            gpuVendor = hardware.gpuVendor,
            nfcSupported = hardware.nfcSupported,
            cameraCount = hardware.cameraCount,
            supported32BitAbis = buildData.supported32BitAbis,
            supported64BitAbis = buildData.supported64BitAbis,

            totalRamBytes = totalRam,
            availableRamBytes = availRam,
            internalStorageBytes = internalTotal,
            externalStorageBytes = null,
            freeStorageBytes = internalFree,

            batteryPercentage = batteryPct,
            chargingState = "UNKNOWN",
            batteryHealth = "UNKNOWN",
            batteryTemperatureCelsius = temperatureC,
            batteryVoltageMillivolts = voltage,
            batteryTechnology = "",

            displayWidthPixels = width,
            displayHeightPixels = height,
            densityDpi = densityDpi,
            refreshRateHz = refreshRate,
            screenSizeInches = screenInches,

            wifiSsid = null,
            wifiBssid = null,
            networkType = networkType,
            ipAddresses = emptyList(),
            dnsServers = emptyList(),
            vpnDetected = false,
            connectionStatus = networkType,
            isScreenLocked = presence.isScreenLocked,
            isInternetConnected = presence.isInternetConnected,
            onlineStatus = presence.onlineStatus,
            offlineReason = presence.offlineReason,

            simOperatorName = telephony.simOperatorName,
            carrierName = telephony.carrierName,
            mcc = telephony.mcc,
            mnc = telephony.mnc,
            countryIso = telephony.countryIso,
            hasTelephonyData = telephony.hasTelephonyData,
            simSlots = telephony.simSlots,

            serialNumber = identifiers.serialNumber,
            androidId = identifiers.androidId,
            imeiPrimary = identifiers.imeiPrimary,
            imeiSecondary = identifiers.imeiSecondary,
            meid = identifiers.meid,
            iccid = identifiers.iccid,
            imsi = identifiers.imsi,
            phoneNumber = identifiers.phoneNumber,
            identifierAvailability = identifiers.identifierAvailability,

            userDeviceName = buildData.userDeviceName,
            buildType = buildData.buildType,
            buildTags = buildData.buildTags,
            buildIncremental = buildData.buildIncremental,
            buildTimeEpochMillis = buildData.buildTimeEpochMillis,
            baseOs = buildData.baseOs,
            previewSdkInt = buildData.previewSdkInt,

            availableSensors = hardware.availableSensors,
            installedApplications = installed,
            systemApplications = systemApps,

            rootDetected = false,
            emulatorDetected = emulatorDetected,
            developerOptionsEnabled = developerOptions,
            usbDebuggingEnabled = usbDebug,
            deviceOwnerStatus = security.deviceOwnerStatus,
            isDeviceOwner = security.isDeviceOwner,
            isProfileOwner = security.isProfileOwner,
            deviceOwnerComponent = security.deviceOwnerComponent,
            organizationName = security.organizationName,
            isEncrypted = security.isEncrypted,
            encryptionStatus = security.encryptionStatus,
            screenLockType = security.screenLockType,
            isOemUnlocked = security.isOemUnlocked,
            googlePlayServicesVersion = security.googlePlayServicesVersion,
            isWorkProfile = security.isWorkProfile,

            bluetoothName = hardware.bluetoothName,
            bluetoothEnabled = hardware.bluetoothEnabled,
            uptimeMillis = hardware.uptimeMillis,
            timezone = hardware.timezone,
            locale = hardware.locale,

            collectedAtEpochMillis = System.currentTimeMillis(),
        )
    }
}
