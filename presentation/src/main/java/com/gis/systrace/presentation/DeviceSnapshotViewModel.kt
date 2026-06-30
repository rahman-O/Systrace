package com.gis.systrace.presentation

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gis.systrace.core.common.DeviceSnapshot
import com.gis.systrace.core.common.StaticDeviceCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeviceSnapshotViewModel(application: Application) : AndroidViewModel(application) {
    private val _snapshot = MutableStateFlow<DeviceSnapshot?>(null)
    val snapshot: StateFlow<DeviceSnapshot?> = _snapshot.asStateFlow()

    private val repository = SnapshotRepository(application)
    private val locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager

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
        registerLocationListener()
    }

    fun refresh() {
        viewModelScope.launch {
            repository.getLatestSnapshot()?.let { _snapshot.value = it }
            val snapshot = StaticDeviceCollector.collect(getApplication())
            _snapshot.value = snapshot
            repository.saveSnapshot(snapshot)
        }
    }

    private fun loadCachedSnapshot() {
        repository.getLatestSnapshot()?.let { cached ->
            _snapshot.value = cached
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerLocationListener() {
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 5f, locationListener)
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 5f, locationListener)
            }
        } catch (e: SecurityException) {
            // Location permission not granted
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            locationManager.removeUpdates(locationListener)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
