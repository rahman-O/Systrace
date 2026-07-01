package com.gis.systrace.data.mdm

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.hmdm.IMdmApi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Binds to Headwind MDM launcher AIDL service and reads per-app settings
 * via [IMdmApi.queryAppPreference].
 */
@Singleton
class HeadwindMdmClient @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    @Volatile
    private var mdmApi: IMdmApi? = null

    @Volatile
    private var binding = false

    private var serviceConnection: ServiceConnection? = null

    fun queryAppPreference(attr: String): String? {
        if (!ensureConnected(CONNECT_TIMEOUT_MS)) {
            return null
        }
        return try {
            mdmApi?.queryAppPreference(SYSTRACE_PKG, attr)?.trim()?.takeIf { it.isNotEmpty() }
        } catch (t: Throwable) {
            Log.w(TAG, "queryAppPreference($attr) failed: ${t.message}")
            disconnect()
            null
        }
    }

    /** Launcher config bundle (device number, server URL, etc.). */
    fun queryConfigValue(key: String): String? {
        if (!ensureConnected(CONNECT_TIMEOUT_MS)) {
            return null
        }
        return try {
            val bundle = mdmApi?.queryConfig() ?: return null
            bundle.getString(key)?.trim()?.takeIf { it.isNotEmpty() }
        } catch (t: Throwable) {
            Log.w(TAG, "queryConfigValue($key) failed: ${t.message}")
            null
        }
    }

    fun resolveDeviceNumber(): String? {
        return listOf("deviceId", "deviceNumber", "DEVICE_NUMBER", "number")
            .firstNotNullOfOrNull { queryConfigValue(it) }
    }

    fun resolveServerBaseUrl(): String? {
        return listOf("baseUrl", "BASE_URL", "serverUrl", "configBaseUrl")
            .firstNotNullOfOrNull { queryConfigValue(it) }
    }

    fun ensureConnected(timeoutMs: Long = CONNECT_TIMEOUT_MS): Boolean {
        if (mdmApi != null) {
            return true
        }
        synchronized(this) {
            if (mdmApi != null) {
                return true
            }
            if (binding) {
                return waitForConnection(timeoutMs)
            }
            binding = true
            val latch = CountDownLatch(1)
            val connected = AtomicReference(false)
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    mdmApi = IMdmApi.Stub.asInterface(service)
                    connected.set(true)
                    latch.countDown()
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    mdmApi = null
                }
            }
            serviceConnection = connection
            val intent = Intent(SERVICE_ACTION).setPackage(LAUNCHER_PKG)
            var bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            if (!bound) {
                intent.setPackage(LEGACY_LAUNCHER_PKG)
                bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
            if (!bound) {
                Log.w(TAG, "Failed to bind Headwind MDM service")
                binding = false
                serviceConnection = null
                return false
            }
            val ok = latch.await(timeoutMs, TimeUnit.MILLISECONDS) && connected.get()
            binding = false
            if (!ok) {
                Log.w(TAG, "Headwind MDM connect timed out after ${timeoutMs}ms")
                disconnect()
            } else {
                Log.d(TAG, "Headwind MDM connected")
            }
            return ok
        }
    }

    private fun waitForConnection(timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (mdmApi != null) {
                return true
            }
            Thread.sleep(50)
        }
        return mdmApi != null
    }

    /** Ask the launcher to re-fetch configuration from the MDM server (HTTP sync). */
    fun forceConfigUpdate(): Boolean {
        if (!ensureConnected(CONNECT_TIMEOUT_MS)) {
            return false
        }
        return try {
            mdmApi?.forceConfigUpdate()
            Log.d(TAG, "Requested launcher forceConfigUpdate")
            true
        } catch (t: Throwable) {
            Log.w(TAG, "forceConfigUpdate failed: ${t.message}")
            disconnect()
            false
        }
    }

    fun disconnect() {
        synchronized(this) {
            val connection = serviceConnection
            if (connection != null) {
                try {
                    context.unbindService(connection)
                } catch (_: Throwable) {
                }
            }
            serviceConnection = null
            mdmApi = null
            binding = false
        }
    }

    companion object {
        private const val TAG = "HeadwindMdmClient"
        private const val SERVICE_ACTION = "com.hmdm.action.Connect"
        private const val LAUNCHER_PKG = "com.hmdm.launcher"
        private const val LEGACY_LAUNCHER_PKG = "ru.headwind.kiosk"
        private const val SYSTRACE_PKG = "com.gis.systrace"
        private const val CONNECT_TIMEOUT_MS = 4_000L
    }
}
