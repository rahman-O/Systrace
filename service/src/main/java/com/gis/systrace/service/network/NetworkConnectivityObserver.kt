package com.gis.systrace.service.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build

/**
 * Observes default-network connectivity and reports whether the device has internet capability.
 */
class NetworkConnectivityObserver(
    context: Context,
    private val onInternetConnectivityChanged: (hasInternet: Boolean) -> Unit,
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var lastHasInternet: Boolean? = null

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            notifyIfChanged()
        }

        override fun onLost(network: Network) {
            notifyIfChanged()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            notifyIfChanged()
        }
    }

    fun register() {
        val initial = hasInternet()
        lastHasInternet = initial
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } else {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, callback)
        }
        // Seed activity timeline with current presence when the collector starts.
        onInternetConnectivityChanged(initial)
    }

    fun unregister() {
        try {
            connectivityManager.unregisterNetworkCallback(callback)
        } catch (_: Throwable) {
        }
    }

    fun currentHasInternet(): Boolean = hasInternet()

    private fun notifyIfChanged() {
        val now = hasInternet()
        if (lastHasInternet == null || lastHasInternet != now) {
            lastHasInternet = now
            onInternetConnectivityChanged(now)
        }
    }

    private fun hasInternet(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
