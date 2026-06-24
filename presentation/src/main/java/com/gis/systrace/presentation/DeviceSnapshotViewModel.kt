package com.gis.systrace.presentation

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gis.systrace.core.common.DeviceSnapshot
import com.gis.systrace.data.SnapshotRepository
import com.gis.systrace.service.DeviceCollectorForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeviceSnapshotViewModel(application: Application) : AndroidViewModel(application) {
    private val _snapshot = MutableStateFlow<DeviceSnapshot?>(null)
    val snapshot: StateFlow<DeviceSnapshot?> = _snapshot.asStateFlow()

    private val _serviceRunning = MutableStateFlow(DeviceCollectorForegroundService.isRunning)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning.asStateFlow()

    private val repository = SnapshotRepository(application)
    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refresh()
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { refresh() }
        override fun onLost(network: Network) { refresh() }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            refresh()
        }
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
    }

    init {
        loadCachedSnapshot()
        refresh()
        updateServiceStatus()
        registerListeners()
    }

    fun updateServiceStatus() {
        _serviceRunning.value = DeviceCollectorForegroundService.isRunning
    }

    fun refresh() {
        viewModelScope.launch {
            repository.getLatestSnapshot()?.let { _snapshot.value = it }
            val snapshot = DeviceSnapshotCollector.collect(getApplication())
            _snapshot.value = snapshot
            updateServiceStatus()
        }
    }

    private fun loadCachedSnapshot() {
        repository.getLatestSnapshot()?.let { cached ->
            _snapshot.value = cached
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerListeners() {
        val application = getApplication<Application>()
        application.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), networkCallback)

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 5f, locationListener)
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 5f, locationListener)
            }
        } catch (e: SecurityException) {}
    }

    override fun onCleared() {
        super.onCleared()
        val application = getApplication<Application>()
        try {
            application.unregisterReceiver(batteryReceiver)
            connectivityManager.unregisterNetworkCallback(networkCallback)
            locationManager.removeUpdates(locationListener)
        } catch (e: Exception) {}
    }
}
