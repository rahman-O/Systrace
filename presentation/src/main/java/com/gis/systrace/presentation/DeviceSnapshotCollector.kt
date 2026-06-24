package com.gis.systrace.presentation

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.MediaDrm
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.util.DisplayMetrics
import com.gis.systrace.core.common.DeviceSnapshot
import com.gis.systrace.core.common.collectors.BuildInfoCollector
import com.gis.systrace.core.common.collectors.DeviceIdentifiersCollector
import com.gis.systrace.core.common.collectors.DevicePresenceCollector
import com.gis.systrace.core.common.collectors.HardwareCollector
import com.gis.systrace.core.common.collectors.MdmSecurityCollector
import com.gis.systrace.core.common.collectors.TelephonyCollector
import java.io.File
import java.net.NetworkInterface
import java.util.Collections
import java.util.UUID

object DeviceSnapshotCollector {
    @SuppressLint("MissingPermission")
    fun collect(context: Context): DeviceSnapshot {
        val buildData = BuildInfoCollector.collect(context)
        val identifiers = DeviceIdentifiersCollector.collect(context)
        val telephony = TelephonyCollector.collect(context)
        val security = MdmSecurityCollector.collect(context)
        val hardware = HardwareCollector.collect(context)

        val androidVersion = Build.VERSION.RELEASE ?: "${Build.VERSION.SDK_INT}"
        val sdk = Build.VERSION.SDK_INT
        val securityPatch = if (Build.VERSION.SDK_INT >= 23) Build.VERSION.SECURITY_PATCH ?: "" else ""
        val buildNumber = Build.DISPLAY ?: Build.ID
        val kernelVersion = System.getProperty("os.version") ?: ""

        // Memory
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val totalRam = memInfo.totalMem
        val availRam = memInfo.availMem

        // Storage
        val statInternal = StatFs(context.filesDir.absolutePath)
        val internalTotal = statInternal.blockSizeLong * statInternal.blockCountLong
        val internalFree = statInternal.blockSizeLong * statInternal.availableBlocksLong

        var externalTotal: Long? = null
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            val path = context.getExternalFilesDir(null)
            if (path != null) {
                val statExternal = StatFs(path.absolutePath)
                externalTotal = statExternal.blockSizeLong * statExternal.blockCountLong
            }
        }

        // Battery
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val batteryPct = batteryIntent?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) (level * 100) / scale else -1
        } ?: -1
        val voltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val temperatureC = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)?.let { it / 10f } ?: -1f
        val health = when (batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }
        val status = when (batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }
        val technology = batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: ""
        val batteryCycleCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val cycles = bm?.getIntProperty(5) ?: -1 // BatteryManager.BATTERY_PROPERTY_CYCLE_COUNT
                cycles.takeIf { it >= 0 }
            } catch (t: Throwable) {
                null
            }
        } else {
            null
        }

        // Display
        val metrics: DisplayMetrics = context.resources.displayMetrics
        val refreshRate = try {
            if (Build.VERSION.SDK_INT >= 30) context.display?.refreshRate ?: 60f
            else 60f
        } catch (e: Throwable) { 60f }
        val screenInches = Math.hypot(metrics.widthPixels.toDouble() / metrics.xdpi, metrics.heightPixels.toDouble() / metrics.ydpi).toFloat()

        // Network
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connectivity?.activeNetwork
        val nc = connectivity?.getNetworkCapabilities(activeNetwork)
        val linkProps = connectivity?.getLinkProperties(activeNetwork)

        val networkType = when {
            nc == null -> "OFFLINE"
            nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            nc.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "OTHER"
        }

        var wifiSsid: String? = null
        var wifiBssid: String? = null
        if (networkType == "WIFI") {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val wifiInfo = wifiManager?.connectionInfo
            wifiSsid = wifiInfo?.ssid?.removeSurrounding("\"")
            wifiBssid = wifiInfo?.bssid
        }

        val ipAddresses = mutableListOf<String>()
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) ipAddresses.add(addr.hostAddress ?: "")
                }
            }
        } catch (t: Throwable) {}
        val dnsServers = linkProps?.dnsServers?.map { it.hostAddress ?: "" } ?: emptyList<String>()
        val presence = DevicePresenceCollector.collect(context)

        // Applications
        val pm = context.packageManager
        val installed = try {
            pm.getInstalledPackages(0).map { pkg ->
                val vCode = if (Build.VERSION.SDK_INT >= 28) pkg.longVersionCode else pkg.versionCode.toLong()
                DeviceSnapshot.AppDetail(pkg.packageName, pkg.versionName, vCode)
            }
        } catch (t: Throwable) {
            emptyList<DeviceSnapshot.AppDetail>()
        }

        // Location
        val locManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        var bestLoc: Location? = null
        try {
            locManager?.getProviders(true)?.forEach { provider ->
                val l = locManager.getLastKnownLocation(provider)
                if (l != null && (bestLoc == null || l.accuracy < (bestLoc?.accuracy ?: Float.MAX_VALUE))) {
                    bestLoc = l
                }
            }
        } catch (e: SecurityException) {}

        // DRM / Widevine
        val widevineUuid = UUID(-0x121074568629b532L, -0x35b455dbb0a34232L)
        val widevineLevel = try {
            val mediaDrm = MediaDrm(widevineUuid)
            val level = mediaDrm.getPropertyString("securityLevel")
            mediaDrm.release()
            level
        } catch (e: Exception) { null }

        val rootDetected = checkRoot()

        return DeviceSnapshot(
            deviceManufacturer = Build.MANUFACTURER ?: "",
            deviceBrand = Build.BRAND ?: "",
            deviceModel = Build.MODEL ?: "",
            deviceProduct = Build.PRODUCT ?: "",
            deviceName = Build.DEVICE ?: "",
            hardware = Build.HARDWARE ?: "",
            board = Build.BOARD ?: "",
            bootloader = Build.BOOTLOADER ?: "",
            fingerprint = Build.FINGERPRINT ?: "",
            buildInfo = buildData.buildInfo,
            androidVersion = androidVersion,
            sdkVersion = sdk,
            securityPatch = securityPatch,
            buildNumber = buildNumber,
            kernelVersion = kernelVersion,
            radioVersion = Build.getRadioVersion(),
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
            externalStorageBytes = externalTotal,
            freeStorageBytes = internalFree,

            batteryPercentage = batteryPct,
            chargingState = status,
            batteryHealth = health,
            batteryTemperatureCelsius = temperatureC,
            batteryVoltageMillivolts = voltage,
            batteryTechnology = technology,
            batteryCycleCount = batteryCycleCount,

            displayWidthPixels = metrics.widthPixels,
            displayHeightPixels = metrics.heightPixels,
            densityDpi = metrics.densityDpi,
            refreshRateHz = refreshRate,
            screenSizeInches = screenInches,

            wifiSsid = wifiSsid,
            wifiBssid = wifiBssid,
            networkType = networkType,
            ipAddresses = ipAddresses,
            dnsServers = dnsServers,
            vpnDetected = nc?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ?: false,
            connectionStatus = if (nc != null) "Connected" else "Disconnected",
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
            systemApplications = installed.filter { pm.getLaunchIntentForPackage(it.packageName) == null },

            rootDetected = rootDetected,
            emulatorDetected = (Build.FINGERPRINT?.contains("generic") == true) || (Build.MODEL?.contains("Emulator") == true),
            developerOptionsEnabled = try {
                Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
            } catch (t: Throwable) { false },
            usbDebuggingEnabled = try {
                Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
            } catch (t: Throwable) { false },
            deviceOwnerStatus = security.deviceOwnerStatus,
            widevineLevel = widevineLevel,
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

            lastKnownLatitude = bestLoc?.latitude,
            lastKnownLongitude = bestLoc?.longitude,
            locationProvider = bestLoc?.provider,
            locationAccuracyMeters = bestLoc?.accuracy,
            locationTimestampEpochMillis = bestLoc?.time,

            collectedAtEpochMillis = System.currentTimeMillis(),
        )
    }

    private fun checkRoot(): Boolean {
        val paths = arrayOf(
            "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
        )
        return paths.any { File(it).exists() }
    }
}
