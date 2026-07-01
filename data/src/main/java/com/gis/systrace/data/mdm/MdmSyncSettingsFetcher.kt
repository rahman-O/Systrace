package com.gis.systrace.data.mdm

import android.util.Log
import com.gis.systrace.core.network.client.SysTraceHttpClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fallback when Headwind does not expose per-app preferences via AIDL:
 * pull applicationSettings from the public sync configuration endpoint.
 */
@Singleton
class MdmSyncSettingsFetcher @Inject constructor(
    private val headwindMdmClient: HeadwindMdmClient,
    private val httpClient: SysTraceHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun fetch(): MdmSettingsReader.SysTraceMdmConfig? {
        headwindMdmClient.ensureConnected()
        val deviceId = headwindMdmClient.resolveDeviceNumber()
        val baseUrl = headwindMdmClient.resolveServerBaseUrl()
        if (deviceId.isNullOrBlank() || baseUrl.isNullOrBlank()) {
            Log.d(
                TAG,
                "Sync settings skip deviceId=${!deviceId.isNullOrBlank()} baseUrl=${!baseUrl.isNullOrBlank()}",
            )
            return null
        }
        val normalizedBase = normalizeBaseUrl(baseUrl)
        return try {
            val response = httpClient.getJson(
                baseUrl = normalizedBase,
                path = "rest/public/sync/configuration/$deviceId",
                apiKey = "",
            )
            if (!response.isSuccessful || response.body.isNullOrBlank()) {
                Log.w(TAG, "Sync configuration HTTP ${response.code}")
                return null
            }
            parseSysTraceConfig(normalizedBase, deviceId, response.body!!)
        } catch (t: Throwable) {
            Log.w(TAG, "Sync configuration fetch failed: ${t.message}")
            null
        }
    }

    private fun parseSysTraceConfig(
        baseUrl: String,
        deviceId: String,
        body: String,
    ): MdmSettingsReader.SysTraceMdmConfig? {
        val envelope = json.decodeFromString<SyncEnvelopeDto>(body)
        val settings = envelope.data?.applicationSettings.orEmpty()
            .filter { it.packageId.equals(SYSTRACE_PKG, ignoreCase = true) }
        if (settings.isEmpty()) {
            Log.d(TAG, "No SysTrace applicationSettings in sync response")
            return null
        }
        val byName = settings.associate { it.name to it.value }
        val apiKey = byName["apiKey"]?.trim().orEmpty()
        if (apiKey.isBlank()) {
            Log.d(TAG, "Sync response missing apiKey for SysTrace")
            return null
        }
        val syncEnabled = byName["syncEnabled"]?.toBooleanStrictOrNull() ?: true
        val syncIntervalMs = byName["syncIntervalMs"]?.toLongOrNull() ?: 60_000L
        val resolvedBase = byName["baseUrl"]?.trim()?.takeIf { it.isNotEmpty() } ?: baseUrl
        Log.d(TAG, "Sync API settings loaded for deviceId=$deviceId")
        return MdmSettingsReader.SysTraceMdmConfig(
            baseUrl = normalizeBaseUrl(resolvedBase),
            deviceId = byName["deviceId"]?.trim()?.ifEmpty { deviceId } ?: deviceId,
            apiKey = apiKey,
            syncEnabled = syncEnabled,
            syncIntervalMs = syncIntervalMs.coerceAtLeast(15_000L),
        )
    }

    private fun normalizeBaseUrl(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.endsWith('/')) trimmed else "$trimmed/"
    }

    @Serializable
    private data class SyncEnvelopeDto(
        val status: String? = null,
        val data: SyncDataDto? = null,
    )

    @Serializable
    private data class SyncDataDto(
        @SerialName("applicationSettings")
        val applicationSettings: List<AppSettingDto> = emptyList(),
    )

    @Serializable
    private data class AppSettingDto(
        @SerialName("packageId")
        val packageId: String = "",
        val name: String = "",
        val value: String = "",
    )

    companion object {
        private const val TAG = "MdmSyncSettingsFetcher"
        private const val SYSTRACE_PKG = "com.gis.systrace"
    }
}
