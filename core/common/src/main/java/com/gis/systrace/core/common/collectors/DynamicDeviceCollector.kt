package com.gis.systrace.core.common.collectors

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.app.KeyguardManager
import android.content.res.Configuration
import android.os.PowerManager
import android.provider.Settings
import android.bluetooth.BluetoothAdapter
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.app.ActivityManager
import android.telephony.TelephonyManager
import com.gis.systrace.core.common.CollectOptions
import com.gis.systrace.core.common.DeviceSnapshot

data class DynamicSnapshotFields(
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
    val onlineStatus: String = "ONLINE",
    val isScreenLocked: Boolean = false,
    val screenOn: Boolean? = null,
    val screenBrightness: Int? = null,
    val screenOrientation: String? = null,
    val isInternetConnected: Boolean = false,
    val offlineReason: String? = null,
    val bluetoothEnabled: Boolean = false,
    val vpnDetected: Boolean = false,
    val uptimeMillis: Long? = null,
)

object DynamicDeviceCollector {

    fun collect(context: Context, @Suppress("UNUSED_PARAMETER") options: CollectOptions): DynamicSnapshotFields {
        val battery = readBattery(context)
        val network = readNetwork(context, readWifiRssi(context), readWifiSsid(context))
        val simSignal = readSimSignalDbm(context)
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)

        val stat = StatFs(Environment.getDataDirectory().absolutePath)
        val freeStorage = stat.availableBlocksLong * stat.blockSizeLong

        val screen = readScreenState(context)
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val internet = connectivity?.activeNetwork?.let { net ->
            connectivity.getNetworkCapabilities(net)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } ?: false

        return DynamicSnapshotFields(
            availableRamBytes = memInfo.availMem,
            freeStorageBytes = freeStorage,
            batteryPercentage = battery.percentage,
            batteryHealth = battery.health,
            chargingState = battery.charging,
            batteryTechnology = battery.technology,
            batteryTemperatureCelsius = battery.temperatureC,
            batteryVoltageMillivolts = battery.voltage,
            networkType = network.type,
            wifiSsid = network.ssid,
            wifiBssid = network.bssid,
            wifiRssiDbm = network.rssiDbm,
            simSignalDbm = simSignal,
            connectionStatus = network.status,
            ipAddresses = network.ips,
            onlineStatus = if (internet) "ONLINE" else "OFFLINE",
            isScreenLocked = screen.locked,
            screenOn = screen.screenOn,
            screenBrightness = screen.brightness,
            screenOrientation = screen.orientation,
            isInternetConnected = internet,
            offlineReason = if (internet) null else "NO_INTERNET",
            bluetoothEnabled = readBluetoothEnabled(context),
            vpnDetected = false,
            uptimeMillis = SystemClock.elapsedRealtime(),
        )
    }

    private data class BatteryTuple(
        val percentage: Int?,
        val health: String?,
        val charging: String?,
        val technology: String?,
        val temperatureC: Float?,
        val voltage: Int?,
    )

    private data class NetworkTuple(
        val type: String?,
        val ssid: String?,
        val bssid: String?,
        val rssiDbm: Int?,
        val status: String?,
        val ips: List<String>,
    )

    private fun readBattery(context: Context): BatteryTuple {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return BatteryTuple(null, null, null, null, null, null)
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else null
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "CHARGING"
            BatteryManager.BATTERY_STATUS_FULL -> "FULL"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "DISCHARGING"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "NOT_CHARGING"
            else -> "UNKNOWN"
        }
        val health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "GOOD"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "OVERHEAT"
            BatteryManager.BATTERY_HEALTH_DEAD -> "DEAD"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "OVER_VOLTAGE"
            BatteryManager.BATTERY_HEALTH_COLD -> "COLD"
            else -> "UNKNOWN"
        }
        val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        val tempC = if (temp > 0) temp / 10f else null
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1).takeIf { it > 0 }
        val tech = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
        return BatteryTuple(pct, health, charging, tech, tempC, voltage)
    }

    private data class ScreenTuple(
        val screenOn: Boolean,
        val locked: Boolean,
        val brightness: Int?,
        val orientation: String?,
    )

    private fun readScreenState(context: Context): ScreenTuple {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val screenOn = pm?.isInteractive == true
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val locked = km?.isKeyguardLocked == true
        val brightness = readBrightness(context)
        val orientation = when (context.resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> "landscape"
            Configuration.ORIENTATION_PORTRAIT -> "portrait"
            else -> "unknown"
        }
        return ScreenTuple(screenOn, locked, brightness, orientation)
    }

    private fun readBrightness(context: Context): Int? {
        return try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, -1)
                .takeIf { it >= 0 }
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun readWifiSsid(context: Context): String? {
        return try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                ?: return null
            val raw = wm.connectionInfo?.ssid?.trim()?.trim('"')
            if (raw.isNullOrBlank() || raw.equals("<unknown ssid>", ignoreCase = true)) null else raw
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun readBluetoothEnabled(context: Context): Boolean {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false
            adapter.isEnabled
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun readWifiRssi(context: Context): Int? {
        return try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                ?: return null
            val info = wm.connectionInfo ?: return null
            val rssi = info.rssi
            if (rssi in -120..0) rssi else null
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun readSimSignalDbm(context: Context): Int? {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: return null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val strength = tm.signalStrength ?: return null
                strength.cellSignalStrengths.firstOrNull()?.dbm?.takeIf { it in -140..0 }
            } else {
                @Suppress("DEPRECATION")
                tm.signalStrength?.let { legacy ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        legacy.cellSignalStrengths.firstOrNull()?.dbm?.takeIf { it in -140..0 }
                    } else null
                }
            }
        } catch (_: SecurityException) {
            null
        }
    }

    private fun readNetwork(context: Context, rssiDbm: Int?, ssid: String?): NetworkTuple {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkTuple(null, null, null, null, "DISCONNECTED", emptyList())
        val active = cm.activeNetwork
        val caps = active?.let { cm.getNetworkCapabilities(it) }
        val type = when {
            caps == null -> "NONE"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            else -> "OTHER"
        }
        val status = if (active != null) "CONNECTED" else "DISCONNECTED"
        val wifiRssi = if (type == "WIFI") rssiDbm else null
        return NetworkTuple(type, ssid, null, wifiRssi, status, emptyList())
    }
}

fun DeviceSnapshot.withDynamicFields(dynamic: DynamicSnapshotFields): DeviceSnapshot = copy(
    availableRamBytes = dynamic.availableRamBytes,
    freeStorageBytes = dynamic.freeStorageBytes,
    batteryPercentage = dynamic.batteryPercentage,
    batteryHealth = dynamic.batteryHealth,
    chargingState = dynamic.chargingState,
    batteryTechnology = dynamic.batteryTechnology,
    batteryTemperatureCelsius = dynamic.batteryTemperatureCelsius,
    batteryVoltageMillivolts = dynamic.batteryVoltageMillivolts,
    networkType = dynamic.networkType ?: simSlots.firstOrNull()?.networkType,
    wifiSsid = dynamic.wifiSsid,
    wifiBssid = dynamic.wifiBssid,
    wifiRssiDbm = dynamic.wifiRssiDbm,
    simSignalDbm = dynamic.simSignalDbm,
    connectionStatus = dynamic.connectionStatus,
    ipAddresses = dynamic.ipAddresses,
    onlineStatus = dynamic.onlineStatus,
    isScreenLocked = dynamic.isScreenLocked,
    screenOn = dynamic.screenOn,
    screenBrightness = dynamic.screenBrightness,
    screenOrientation = dynamic.screenOrientation,
    isInternetConnected = dynamic.isInternetConnected,
    offlineReason = dynamic.offlineReason,
    bluetoothEnabled = dynamic.bluetoothEnabled,
    vpnDetected = dynamic.vpnDetected,
    uptimeMillis = dynamic.uptimeMillis,
)
