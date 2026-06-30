package com.gis.systrace.core.common

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import com.gis.systrace.core.common.collectors.AboutPhoneCollector
import com.gis.systrace.core.common.collectors.BuildInfoCollector
import com.gis.systrace.core.common.collectors.DeviceIdentifiersCollector
import com.gis.systrace.core.common.collectors.HardwareCollector
import com.gis.systrace.core.common.collectors.SensorsCollector
import com.gis.systrace.core.common.collectors.TelephonyCollector

object StaticDeviceCollector {
    @SuppressLint("MissingPermission")
    fun collect(context: Context): DeviceSnapshot {
        val buildData = BuildInfoCollector.collect(context)
        val identifiers = DeviceIdentifiersCollector.collect(context)
        val telephony = TelephonyCollector.collect(context)
        val hardware = HardwareCollector.collect(context)
        val aboutPhone = AboutPhoneCollector.collect(context, identifiers, telephony)

        val androidVersion = Build.VERSION.RELEASE ?: "${Build.VERSION.SDK_INT}"
        val sdk = Build.VERSION.SDK_INT
        val securityPatch = if (Build.VERSION.SDK_INT >= 23) Build.VERSION.SECURITY_PATCH ?: "" else ""
        val buildNumber = Build.DISPLAY ?: Build.ID
        val kernelVersion = System.getProperty("os.version") ?: ""

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val totalRam = memInfo.totalMem

        val statInternal = StatFs(context.filesDir.absolutePath)
        val internalTotal = statInternal.blockSizeLong * statInternal.blockCountLong

        var externalTotal: Long? = null
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            val path = context.getExternalFilesDir(null)
            if (path != null) {
                val statExternal = StatFs(path.absolutePath)
                externalTotal = statExternal.blockSizeLong * statExternal.blockCountLong
            }
        }

        val metrics: DisplayMetrics = context.resources.displayMetrics
        val refreshRate = try {
            if (Build.VERSION.SDK_INT >= 30) context.display?.refreshRate ?: 60f
            else 60f
        } catch (e: Throwable) {
            60f
        }
        val screenInches = Math.hypot(
            metrics.widthPixels.toDouble() / metrics.xdpi,
            metrics.heightPixels.toDouble() / metrics.ydpi,
        ).toFloat()

        val locManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        var bestLoc: Location? = null
        try {
            locManager?.getProviders(true)?.forEach { provider ->
                val location = locManager.getLastKnownLocation(provider)
                if (location != null &&
                    (bestLoc == null || location.accuracy < (bestLoc?.accuracy ?: Float.MAX_VALUE))
                ) {
                    bestLoc = location
                }
            }
        } catch (e: SecurityException) {
            // Location permission not granted
        }

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
            internalStorageBytes = internalTotal,
            externalStorageBytes = externalTotal,

            displayWidthPixels = metrics.widthPixels,
            displayHeightPixels = metrics.heightPixels,
            densityDpi = metrics.densityDpi,
            refreshRateHz = refreshRate,
            screenSizeInches = screenInches,

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

            androidVersionDisplay = aboutPhone.androidVersionDisplay,
            googlePlaySystemUpdate = aboutPhone.googlePlaySystemUpdate,
            basebandVersion = aboutPhone.basebandVersion,
            simStatusSummary = aboutPhone.simStatusSummary,
            socModel = aboutPhone.socModel,
            socManufacturer = aboutPhone.socManufacturer,
            sku = aboutPhone.sku,
            buildCodename = aboutPhone.buildCodename,

            lastKnownLatitude = bestLoc?.latitude,
            lastKnownLongitude = bestLoc?.longitude,
            locationProvider = bestLoc?.provider,
            locationAccuracyMeters = bestLoc?.accuracy,
            locationTimestampEpochMillis = bestLoc?.time,

            collectedAtEpochMillis = System.currentTimeMillis(),
            availableSensors = SensorsCollector.collect(context).map {
                DeviceSnapshot.SensorDetail(it.name, it.type, it.vendor)
            },
        )
    }
}
