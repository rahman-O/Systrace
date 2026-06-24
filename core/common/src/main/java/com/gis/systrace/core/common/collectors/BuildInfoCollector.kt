package com.gis.systrace.core.common.collectors

import android.content.Context
import android.os.Build
import android.provider.Settings

data class BuildInfoData(
    val buildInfo: Map<String, String>,
    val systemProperties: Map<String, String>,
    val userDeviceName: String? = null,
    val buildType: String? = null,
    val buildTags: String? = null,
    val buildIncremental: String? = null,
    val buildTimeEpochMillis: Long? = null,
    val baseOs: String? = null,
    val previewSdkInt: Int? = null,
    val supported32BitAbis: List<String> = emptyList(),
    val supported64BitAbis: List<String> = emptyList(),
)

object BuildInfoCollector {

    fun collect(context: Context): BuildInfoData {
        val buildInfo = linkedMapOf(
            "ID" to Build.ID,
            "SDK" to Build.VERSION.SDK_INT.toString(),
            "Manufacturer" to (Build.MANUFACTURER ?: ""),
            "Brand" to (Build.BRAND ?: ""),
            "Model" to (Build.MODEL ?: ""),
            "Board" to (Build.BOARD ?: ""),
            "Hardware" to (Build.HARDWARE ?: ""),
            "Product" to (Build.PRODUCT ?: ""),
            "Device" to (Build.DEVICE ?: ""),
            "Display" to (Build.DISPLAY ?: ""),
            "Host" to (Build.HOST ?: ""),
            "User" to (Build.USER ?: ""),
            "Type" to (Build.TYPE ?: ""),
            "Tags" to (Build.TAGS ?: ""),
            "Incremental" to (Build.VERSION.INCREMENTAL ?: ""),
            "Codename" to (Build.VERSION.CODENAME ?: ""),
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            buildInfo["SKU"] = Build.SKU ?: ""
            buildInfo["SOC_MANUFACTURER"] = Build.SOC_MANUFACTURER ?: ""
            buildInfo["SOC_MODEL"] = Build.SOC_MODEL ?: ""
        }

        val userDeviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            try {
                Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
            } catch (t: Throwable) {
                null
            }
        } else {
            null
        }

        val baseOs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Build.VERSION.BASE_OS
        } else {
            null
        }

        return BuildInfoData(
            buildInfo = buildInfo,
            systemProperties = SystemPropertiesHelper.collectMdmProperties(),
            userDeviceName = userDeviceName,
            buildType = Build.TYPE,
            buildTags = Build.TAGS,
            buildIncremental = Build.VERSION.INCREMENTAL,
            buildTimeEpochMillis = Build.TIME,
            baseOs = baseOs,
            previewSdkInt = Build.VERSION.PREVIEW_SDK_INT,
            supported32BitAbis = Build.SUPPORTED_32_BIT_ABIS?.toList() ?: emptyList(),
            supported64BitAbis = Build.SUPPORTED_64_BIT_ABIS?.toList() ?: emptyList(),
        )
    }
}
