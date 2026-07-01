package com.gis.systrace.data.mdm

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads SysTrace connection settings pushed by Headwind MDM launcher.
 * Primary path: AIDL [HeadwindMdmClient.queryAppPreference].
 * Fallback: launcher shared prefs (legacy / direct writes).
 */
@Singleton
class MdmSettingsReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val headwindMdmClient: HeadwindMdmClient,
    private val syncSettingsFetcher: MdmSyncSettingsFetcher,
) {

    data class SysTraceMdmConfig(
        val baseUrl: String,
        val deviceId: String,
        val apiKey: String,
        val syncEnabled: Boolean,
        val syncIntervalMs: Long = 60_000L,
    )

    fun read(): SysTraceMdmConfig? {
        readFromHeadwind()?.let { return it }
        readFromLauncherPrefs()?.let { return it }
        return syncSettingsFetcher.fetch()
    }

    private fun readFromHeadwind(): SysTraceMdmConfig? {
        val baseUrl = headwindMdmClient.queryAppPreference("baseUrl") ?: return null
        val deviceId = headwindMdmClient.queryAppPreference("deviceId") ?: return null
        val apiKey = headwindMdmClient.queryAppPreference("apiKey") ?: return null
        if (baseUrl.isBlank() || deviceId.isBlank() || apiKey.isBlank()) {
            Log.d(
                TAG,
                "Headwind settings incomplete baseUrl=${baseUrl.isNotBlank()} " +
                    "deviceId=${deviceId.isNotBlank()} apiKey=${apiKey.isNotBlank()}",
            )
            return null
        }
        val syncEnabled = headwindMdmClient.queryAppPreference("syncEnabled")?.toBooleanStrictOrNull() ?: true
        val syncIntervalMs = headwindMdmClient.queryAppPreference("syncIntervalMs")?.toLongOrNull() ?: 60_000L
        Log.d(TAG, "Headwind MDM settings loaded for deviceId=$deviceId")
        return SysTraceMdmConfig(
            baseUrl = normalizeBaseUrl(baseUrl),
            deviceId = deviceId,
            apiKey = apiKey,
            syncEnabled = syncEnabled,
            syncIntervalMs = syncIntervalMs.coerceAtLeast(15_000L),
        )
    }

    private fun readFromLauncherPrefs(): SysTraceMdmConfig? {
        val launcherContext = launcherContext() ?: return null
        val prefs = launcherContext.getSharedPreferences(
            "$SYSTRACE_PKG$PREFS_SUFFIX",
            Context.MODE_PRIVATE,
        )
        val baseUrl = prefs.getString("baseUrl", null)?.trim().orEmpty()
        val deviceId = prefs.getString("deviceId", null)?.trim().orEmpty()
        val apiKey = prefs.getString("apiKey", null)?.trim().orEmpty()
        val syncEnabled = prefs.getBoolean("syncEnabled", true)
        val syncIntervalMs = prefs.getLong("syncIntervalMs", 60_000L)

        if (baseUrl.isBlank() || deviceId.isBlank() || apiKey.isBlank()) {
            Log.d(
                TAG,
                "Launcher prefs incomplete baseUrl=${baseUrl.isNotBlank()} " +
                    "deviceId=${deviceId.isNotBlank()} apiKey=${apiKey.isNotBlank()}",
            )
            return null
        }
        Log.d(TAG, "Launcher prefs settings loaded for deviceId=$deviceId")
        return SysTraceMdmConfig(
            baseUrl = normalizeBaseUrl(baseUrl),
            deviceId = deviceId,
            apiKey = apiKey,
            syncEnabled = syncEnabled,
            syncIntervalMs = syncIntervalMs.coerceAtLeast(15_000L),
        )
    }

    private fun launcherContext(): Context? {
        return try {
            context.createPackageContext(LAUNCHER_PKG, Context.CONTEXT_IGNORE_SECURITY)
        } catch (t: Throwable) {
            Log.w(TAG, "Cannot read launcher prefs: ${t.message}")
            null
        }
    }

    private fun normalizeBaseUrl(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.endsWith('/')) trimmed else "$trimmed/"
    }

    companion object {
        private const val TAG = "MdmSettingsReader"
        private const val LAUNCHER_PKG = "com.hmdm.launcher"
        private const val SYSTRACE_PKG = "com.gis.systrace"
        private const val PREFS_SUFFIX = ".settings"
    }
}
