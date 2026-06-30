package com.gis.systrace.data

import android.content.Context
import android.content.SharedPreferences
import com.gis.systrace.core.common.DeviceSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnapshotRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("snapshots", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var latestKey: String? = null

    fun saveSnapshot(snapshot: DeviceSnapshot): String {
        val key = "latest_snapshot_${System.currentTimeMillis()}"
        val encoded = json.encodeToString(snapshot)
        prefs.edit().putString(key, encoded).apply()
        latestKey = key
        val allKeys = prefs.all.keys.filter { it.startsWith("latest_snapshot_") }.sorted()
        if (allKeys.size > 10) {
            prefs.edit().remove(allKeys.first()).apply()
        }
        return key
    }

    fun getLatestSnapshot(): DeviceSnapshot? {
        val key = latestKey ?: prefs.all.keys.filter { it.startsWith("latest_snapshot_") }.maxOrNull()
            ?: return null
        latestKey = key
        val encoded = prefs.getString(key, null) ?: return null
        return json.decodeFromString(encoded)
    }

    fun getLatestSnapshotKey(): String? {
        return latestKey ?: prefs.all.keys.filter { it.startsWith("latest_snapshot_") }.maxOrNull()
    }
}
