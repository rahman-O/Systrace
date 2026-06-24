package com.gis.systrace.core.common.collectors

import android.app.KeyguardManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

data class DevicePresenceData(
    val isScreenLocked: Boolean,
    val isInternetConnected: Boolean,
    val onlineStatus: String,
    val offlineReason: String? = null,
)

object DevicePresenceCollector {

    fun collect(context: Context): DevicePresenceData {
        val isScreenLocked = isScreenLocked(context)
        val isInternetConnected = isInternetConnected(context)
        val (onlineStatus, offlineReason) = resolveOnlineStatus(isScreenLocked, isInternetConnected)

        return DevicePresenceData(
            isScreenLocked = isScreenLocked,
            isInternetConnected = isInternetConnected,
            onlineStatus = onlineStatus,
            offlineReason = offlineReason,
        )
    }

    internal fun resolveOnlineStatus(
        isScreenLocked: Boolean,
        isInternetConnected: Boolean,
    ): Pair<String, String?> {
        val onlineStatus = if (isScreenLocked || !isInternetConnected) "OFFLINE" else "ONLINE"
        val offlineReason = when {
            isScreenLocked && !isInternetConnected -> "LOCKED,NO_INTERNET"
            isScreenLocked -> "LOCKED"
            !isInternetConnected -> "NO_INTERNET"
            else -> null
        }
        return onlineStatus to offlineReason
    }

    fun isScreenLocked(context: Context): Boolean {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            keyguard.isDeviceLocked || keyguard.isKeyguardLocked
        } else {
            @Suppress("DEPRECATION")
            keyguard.isKeyguardLocked
        }
    }

    fun isInternetConnected(context: Context): Boolean {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val activeNetwork = connectivity.activeNetwork ?: return false
        val capabilities = connectivity.getNetworkCapabilities(activeNetwork) ?: return false

        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            true
        }

        return hasInternet && isValidated
    }
}
