package com.gis.systrace.core.common.collectors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager

data class DeviceIdentifiers(
    val serialNumber: String? = null,
    val androidId: String? = null,
    val imeiPrimary: String? = null,
    val imeiSecondary: String? = null,
    val meid: String? = null,
    val iccid: String? = null,
    val imsi: String? = null,
    val phoneNumber: String? = null,
    val identifierAvailability: String? = null,
)

object DeviceIdentifiersCollector {

    @SuppressLint("HardwareIds", "MissingPermission")
    fun collect(context: Context): DeviceIdentifiers {
        val availability = mutableListOf<String>()
        val hasPhoneState = context.checkSelfPermission(
            Manifest.permission.READ_PHONE_STATE,
        ) == PackageManager.PERMISSION_GRANTED
        val hasPhoneNumbers = context.checkSelfPermission(
            Manifest.permission.READ_PHONE_NUMBERS,
        ) == PackageManager.PERMISSION_GRANTED

        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (t: Throwable) {
            availability.add("ANDROID_ID_ERROR")
            null
        }

        val serial = collectSerial(context, hasPhoneState, availability)

        val tele = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        var imeiPrimary: String? = null
        var imeiSecondary: String? = null
        var meid: String? = null
        var iccid: String? = null
        var imsi: String? = null
        var phoneNumber: String? = null

        if (tele != null && hasPhoneState) {
            imeiPrimary = readImei(tele, 0, availability)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                imeiSecondary = readImei(tele, 1, availability)
            }
            meid = readMeid(tele, availability)
            iccid = readSimSerial(tele, availability)
            imsi = readImsi(tele, availability)
        } else if (tele != null) {
            availability.add("PHONE_STATE_DENIED")
        }

        if (tele != null && (hasPhoneState || hasPhoneNumbers)) {
            phoneNumber = readPhoneNumber(tele, availability)
        } else if (tele != null) {
            availability.add("PHONE_NUMBER_DENIED")
        }

        return DeviceIdentifiers(
            serialNumber = serial,
            androidId = androidId,
            imeiPrimary = imeiPrimary,
            imeiSecondary = imeiSecondary,
            meid = meid,
            iccid = iccid,
            imsi = imsi,
            phoneNumber = phoneNumber,
            identifierAvailability = availability.takeIf { it.isNotEmpty() }?.distinct()?.joinToString(","),
        )
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    private fun collectSerial(
        context: Context,
        hasPhoneState: Boolean,
        availability: MutableList<String>,
    ): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!hasPhoneState) {
                availability.add("SERIAL_PHONE_STATE_DENIED")
            } else {
                try {
                    val serial = Build.getSerial()
                    if (!serial.isNullOrBlank() && serial != Build.UNKNOWN) return serial
                    availability.add("SERIAL_UNKNOWN")
                } catch (t: SecurityException) {
                    availability.add("SERIAL_SECURITY_EXCEPTION")
                } catch (t: Throwable) {
                    availability.add("SERIAL_ERROR")
                }
            }
        } else {
            @Suppress("DEPRECATION")
            val legacy = Build.SERIAL
            if (!legacy.isNullOrBlank() && legacy != Build.UNKNOWN) return legacy
            availability.add("SERIAL_LEGACY_UNKNOWN")
        }

        val propSerial = SystemPropertiesHelper.get("ro.serialno")
        if (!propSerial.isNullOrBlank() && propSerial != Build.UNKNOWN) return propSerial

        return null
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    private fun readImei(tele: TelephonyManager, slot: Int, availability: MutableList<String>): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                tele.getImei(slot)
            } else {
                @Suppress("DEPRECATION")
                if (slot == 0) tele.deviceId else null
            }
        } catch (t: SecurityException) {
            availability.add("IMEI_SLOT_${slot}_RESTRICTED")
            null
        } catch (t: Throwable) {
            availability.add("IMEI_SLOT_${slot}_ERROR")
            null
        }
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    private fun readMeid(tele: TelephonyManager, availability: MutableList<String>): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                tele.meid
            } else {
                @Suppress("DEPRECATION")
                tele.deviceId
            }
        } catch (t: SecurityException) {
            availability.add("MEID_RESTRICTED")
            null
        } catch (t: Throwable) {
            null
        }
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    private fun readSimSerial(tele: TelephonyManager, availability: MutableList<String>): String? {
        return try {
            @Suppress("DEPRECATION")
            tele.simSerialNumber
        } catch (t: SecurityException) {
            availability.add("ICCID_RESTRICTED")
            null
        } catch (t: Throwable) {
            null
        }
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    private fun readImsi(tele: TelephonyManager, availability: MutableList<String>): String? {
        return try {
            @Suppress("DEPRECATION")
            tele.subscriberId
        } catch (t: SecurityException) {
            availability.add("IMSI_RESTRICTED")
            null
        } catch (t: Throwable) {
            null
        }
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    private fun readPhoneNumber(tele: TelephonyManager, availability: MutableList<String>): String? {
        return try {
            tele.line1Number?.takeIf { it.isNotBlank() }
        } catch (t: SecurityException) {
            availability.add("PHONE_NUMBER_RESTRICTED")
            null
        } catch (t: Throwable) {
            null
        }
    }
}
