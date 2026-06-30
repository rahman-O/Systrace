package com.gis.systrace.core.common.collectors

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

data class AboutPhoneData(
    val androidVersionDisplay: String? = null,
    val googlePlaySystemUpdate: String? = null,
    val basebandVersion: String? = null,
    val simStatusSummary: String? = null,
    val socModel: String? = null,
    val socManufacturer: String? = null,
    val sku: String? = null,
    val buildCodename: String? = null,
)

object AboutPhoneCollector {

    fun collect(
        context: Context,
        identifiers: DeviceIdentifiers,
        telephony: TelephonyData,
    ): AboutPhoneData {
        return AboutPhoneData(
            androidVersionDisplay = readAndroidVersionDisplay(),
            googlePlaySystemUpdate = readGooglePlaySystemUpdate(context),
            basebandVersion = readBasebandVersion(),
            simStatusSummary = buildSimStatusSummary(telephony),
            socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL else null,
            socManufacturer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MANUFACTURER else null,
            sku = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SKU else null,
            buildCodename = Build.VERSION.CODENAME,
        )
    }

    private fun readAndroidVersionDisplay(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY
        } else {
            Build.VERSION.RELEASE ?: "${Build.VERSION.SDK_INT}"
        }
    }

    private fun readGooglePlaySystemUpdate(context: Context): String? {
        val packages = listOf(
            "com.google.android.modulemetadata",
            "com.google.android.gms",
        )
        val pm = context.packageManager
        for (packageName in packages) {
            try {
                val versionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).versionName
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(packageName, 0).versionName
                }
                if (!versionName.isNullOrBlank()) {
                    return versionName
                }
            } catch (_: PackageManager.NameNotFoundException) {
                // Try next package
            } catch (_: Throwable) {
                // Try next package
            }
        }
        return null
    }

    fun readBasebandVersion(): String? {
        val radioVersion = try {
            Build.getRadioVersion()
        } catch (_: Throwable) {
            null
        }
        return radioVersion?.takeIf { it.isNotBlank() }
            ?: SystemPropertiesHelper.get("gsm.version.baseband")
    }

    fun buildSimStatusSummary(telephony: TelephonyData): String {
        if (telephony.simSlots.isEmpty()) {
            return if (telephony.hasTelephonyData) "No SIM data" else "No telephony"
        }
        return telephony.simSlots.joinToString(" | ") { slot ->
            val state = slot.simState ?: "UNKNOWN"
            "SIM ${slot.slotIndex}: $state"
        }
    }
}
