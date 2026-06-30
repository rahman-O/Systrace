package com.gis.systrace.presentation

import android.content.Context
import android.content.SharedPreferences
import com.gis.systrace.core.common.DeviceSnapshot
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SnapshotRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("snapshots", Context.MODE_PRIVATE)
    private val json = Json

    fun saveSnapshot(snapshot: DeviceSnapshot): String {
        val key = "latest_snapshot_${System.currentTimeMillis()}"
        try {
            val encoded = json.encodeToString(snapshot)
            prefs.edit().putString(key, encoded).apply()
            val allKeys = prefs.all.keys.filter { it.startsWith("latest_snapshot_") }.sorted()
            if (allKeys.size > 10) {
                prefs.edit().remove(allKeys.first()).apply()
            }
            return key
        } catch (t: Throwable) {
            return key
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
}
