package com.gis.systrace.data

import android.content.Context
import android.content.SharedPreferences
import com.gis.systrace.core.common.DeviceSnapshot
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class SnapshotRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("snapshots", Context.MODE_PRIVATE)
    private val json = Json

    fun saveSnapshot(snapshot: DeviceSnapshot) {
        try {
            val encoded = json.encodeToString(snapshot)
            prefs.edit().putString("latest_snapshot_${System.currentTimeMillis()}", encoded).apply()
            // Keep last 10 snapshots
            val allKeys = prefs.all.keys.filter { it.startsWith("latest_snapshot_") }.sorted()
            if (allKeys.size > 10) {
                prefs.edit().remove(allKeys.first()).apply()
            }
        } catch (t: Throwable) {
            // Log error
        }
    }

    fun getLatestSnapshot(): DeviceSnapshot? {
        return try {
            val key = prefs.all.keys.filter { it.startsWith("latest_snapshot_") }.maxOrNull()
            key?.let {
                val encoded = prefs.getString(it, null)
                encoded?.let { json.decodeFromString(it) }
            }
        } catch (t: Throwable) {
            null
        }
    }

    fun getAllSnapshots(): List<DeviceSnapshot> {
        return try {
            prefs.all.keys
                .filter { it.startsWith("latest_snapshot_") }
                .sortedDescending()
                .mapNotNull { key ->
                    prefs.getString(key, null)?.let { json.decodeFromString<DeviceSnapshot>(it) }
                }
        } catch (t: Throwable) {
            emptyList()
        }
    }
}

