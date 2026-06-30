package com.gis.systrace.core.common.collectors

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Build
import java.io.BufferedReader
import java.io.FileReader

data class HardwareData(
    val cpuAbi: List<String> = emptyList(),
    val cpuCores: Int = 0,
    val cpuModel: String? = null,
    val gpuRenderer: String? = null,
    val gpuVendor: String? = null,
    val nfcSupported: Boolean = false,
    val cameraCount: Int = 0,
)

object HardwareCollector {

    fun collect(context: Context): HardwareData {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val configInfo = activityManager?.deviceConfigurationInfo

        val cameraCount = try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            cameraManager?.cameraIdList?.size ?: 0
        } catch (t: Throwable) {
            0
        }

        return HardwareData(
            cpuAbi = Build.SUPPORTED_ABIS.toList(),
            cpuCores = Runtime.getRuntime().availableProcessors(),
            cpuModel = readCpuModel(),
            gpuRenderer = configInfo?.glEsVersion?.let { "GLES $it" },
            gpuVendor = null,
            nfcSupported = context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC),
            cameraCount = cameraCount,
        )
    }

    private fun readCpuModel(): String? {
        return try {
            BufferedReader(FileReader("/proc/cpuinfo")).use { reader ->
                reader.lineSequence()
                    .firstOrNull { it.startsWith("Hardware") || it.startsWith("model name") }
                    ?.substringAfter(":", "")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
            }
        } catch (t: Throwable) {
            null
        }
    }
}
