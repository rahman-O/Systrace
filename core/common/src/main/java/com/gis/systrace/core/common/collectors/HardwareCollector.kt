package com.gis.systrace.core.common.collectors

import android.Manifest
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.SystemClock
import com.gis.systrace.core.common.DeviceSnapshot
import java.io.BufferedReader
import java.io.FileReader
import java.util.Locale
import java.util.TimeZone

data class HardwareData(
    val cpuAbi: List<String> = emptyList(),
    val cpuCores: Int = 0,
    val cpuModel: String? = null,
    val gpuRenderer: String? = null,
    val gpuVendor: String? = null,
    val nfcSupported: Boolean = false,
    val cameraCount: Int = 0,
    val availableSensors: List<DeviceSnapshot.SensorDetail> = emptyList(),
    val bluetoothName: String? = null,
    val bluetoothEnabled: Boolean? = null,
    val uptimeMillis: Long? = null,
    val timezone: String? = null,
    val locale: String? = null,
)

object HardwareCollector {

    fun collect(context: Context, includeSensors: Boolean = true): HardwareData {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val configInfo = activityManager?.deviceConfigurationInfo

        val sensors = if (includeSensors) {
            val sensorMgr = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            sensorMgr?.getSensorList(Sensor.TYPE_ALL)?.map { sensor ->
                DeviceSnapshot.SensorDetail(
                    name = sensor.name ?: "",
                    vendor = sensor.vendor ?: "",
                    version = sensor.version,
                    powerMw = sensor.power,
                    type = sensor.type,
                )
            } ?: emptyList()
        } else {
            emptyList()
        }

        val cameraCount = try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            cameraManager?.cameraIdList?.size ?: 0
        } catch (t: Throwable) {
            0
        }

        val bluetooth = readBluetooth(context)

        return HardwareData(
            cpuAbi = Build.SUPPORTED_ABIS.toList(),
            cpuCores = Runtime.getRuntime().availableProcessors(),
            cpuModel = readCpuModel(),
            gpuRenderer = configInfo?.glEsVersion?.let { "GLES $it" },
            gpuVendor = null,
            nfcSupported = context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC),
            cameraCount = cameraCount,
            availableSensors = sensors,
            bluetoothName = bluetooth.first,
            bluetoothEnabled = bluetooth.second,
            uptimeMillis = SystemClock.elapsedRealtime(),
            timezone = TimeZone.getDefault().id,
            locale = Locale.getDefault().toLanguageTag(),
        )
    }

    private fun readBluetooth(context: Context): Pair<String?, Boolean?> {
        return try {
            val adapter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                manager?.adapter
            } else {
                @Suppress("DEPRECATION")
                BluetoothAdapter.getDefaultAdapter()
            } ?: return null to null

            val hasConnect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            val name = if (hasConnect) adapter.name else null
            name to adapter.isEnabled
        } catch (t: Throwable) {
            null to null
        }
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
