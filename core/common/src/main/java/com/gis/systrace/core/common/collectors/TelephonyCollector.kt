package com.gis.systrace.core.common.collectors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import com.gis.systrace.core.common.DeviceSnapshot

data class TelephonyData(
    val simOperatorName: String? = null,
    val carrierName: String? = null,
    val mcc: String? = null,
    val mnc: String? = null,
    val countryIso: String? = null,
    val hasTelephonyData: Boolean = false,
    val simSlots: List<DeviceSnapshot.SimSlotDetail> = emptyList(),
)

object TelephonyCollector {

    @SuppressLint("MissingPermission")
    fun collect(context: Context): TelephonyData {
        val tele = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: return TelephonyData()

        val hasTelephony = tele.phoneType != TelephonyManager.PHONE_TYPE_NONE
        val hasPhoneState = context.checkSelfPermission(
            Manifest.permission.READ_PHONE_STATE,
        ) == PackageManager.PERMISSION_GRANTED

        val simOperatorName = tele.simOperatorName
        val carrierName = tele.networkOperatorName
        val mcc = try {
            tele.networkOperator?.takeIf { it.length >= 3 }?.take(3)
        } catch (t: Throwable) {
            null
        }
        val mnc = try {
            tele.networkOperator?.takeIf { it.length > 3 }?.drop(3)
        } catch (t: Throwable) {
            null
        }
        val countryIso = tele.networkCountryIso

        val simSlots = if (hasPhoneState) {
            collectSimSlots(context, tele)
        } else {
            emptyList()
        }

        return TelephonyData(
            simOperatorName = simOperatorName,
            carrierName = carrierName,
            mcc = mcc,
            mnc = mnc,
            countryIso = countryIso,
            hasTelephonyData = hasTelephony,
            simSlots = simSlots,
        )
    }

    @SuppressLint("MissingPermission", "HardwareIds")
    private fun collectSimSlots(context: Context, tele: TelephonyManager): List<DeviceSnapshot.SimSlotDetail> {
        val slots = mutableListOf<DeviceSnapshot.SimSlotDetail>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val activeSubs: List<SubscriptionInfo> = try {
                subManager?.activeSubscriptionInfoList ?: emptyList()
            } catch (t: SecurityException) {
                emptyList()
            }

            if (activeSubs.isNotEmpty()) {
                for (info in activeSubs) {
                    val slotIndex = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        info.simSlotIndex
                    } else {
                        @Suppress("DEPRECATION")
                        info.simSlotIndex
                    }
                    val teleForSub = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        tele.createForSubscriptionId(info.subscriptionId)
                    } else {
                        tele
                    }
                    slots.add(buildSlotDetail(info, teleForSub, slotIndex))
                }
                return slots
            }
        }

        slots.add(
            DeviceSnapshot.SimSlotDetail(
                slotIndex = 0,
                carrierName = tele.networkOperatorName,
                simOperator = tele.simOperatorName,
                mcc = tele.simOperator?.takeIf { it.length >= 3 }?.take(3),
                mnc = tele.simOperator?.takeIf { it.length > 3 }?.drop(3),
                countryIso = tele.simCountryIso,
                simState = simStateName(tele.simState),
                networkType = networkTypeName(tele),
                isRoaming = tele.isNetworkRoaming,
                iccid = readIccid(tele),
                imsi = readImsi(tele),
                phoneNumber = readPhoneNumber(tele),
            ),
        )
        return slots
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    private fun buildSlotDetail(
        info: SubscriptionInfo,
        tele: TelephonyManager,
        slotIndex: Int,
    ): DeviceSnapshot.SimSlotDetail {
        val mccMnc = info.mccString to info.mncString
        return DeviceSnapshot.SimSlotDetail(
            slotIndex = slotIndex,
            carrierName = tele.networkOperatorName,
            simOperator = info.carrierName?.toString() ?: tele.simOperatorName,
            mcc = mccMnc.first ?: tele.simOperator?.takeIf { it.length >= 3 }?.take(3),
            mnc = mccMnc.second ?: tele.simOperator?.takeIf { it.length > 3 }?.drop(3),
            countryIso = info.countryIso ?: tele.simCountryIso,
            simState = simStateName(tele.simState),
            networkType = networkTypeName(tele),
            isRoaming = tele.isNetworkRoaming,
            isEmbedded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.isEmbedded else false,
            subscriptionId = info.subscriptionId,
            iccid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                info.iccId
            } else {
                readIccid(tele)
            },
            imsi = readImsi(tele),
            phoneNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                info.number?.takeIf { it.isNotBlank() }
            } else {
                readPhoneNumber(tele)
            },
        )
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    private fun readIccid(tele: TelephonyManager): String? {
        return try {
            @Suppress("DEPRECATION")
            tele.simSerialNumber
        } catch (t: Throwable) {
            null
        }
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    private fun readImsi(tele: TelephonyManager): String? {
        return try {
            @Suppress("DEPRECATION")
            tele.subscriberId
        } catch (t: Throwable) {
            null
        }
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    private fun readPhoneNumber(tele: TelephonyManager): String? {
        return try {
            tele.line1Number?.takeIf { it.isNotBlank() }
        } catch (t: Throwable) {
            null
        }
    }

    private fun simStateName(state: Int): String = when (state) {
        TelephonyManager.SIM_STATE_ABSENT -> "ABSENT"
        TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "NETWORK_LOCKED"
        TelephonyManager.SIM_STATE_PIN_REQUIRED -> "PIN_REQUIRED"
        TelephonyManager.SIM_STATE_PUK_REQUIRED -> "PUK_REQUIRED"
        TelephonyManager.SIM_STATE_READY -> "READY"
        TelephonyManager.SIM_STATE_UNKNOWN -> "UNKNOWN"
        TelephonyManager.SIM_STATE_CARD_IO_ERROR -> "CARD_IO_ERROR"
        TelephonyManager.SIM_STATE_CARD_RESTRICTED -> "CARD_RESTRICTED"
        TelephonyManager.SIM_STATE_NOT_READY -> "NOT_READY"
        TelephonyManager.SIM_STATE_PERM_DISABLED -> "PERM_DISABLED"
        else -> "OTHER"
    }

    @SuppressLint("MissingPermission")
    private fun networkTypeName(tele: TelephonyManager): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                when (tele.dataNetworkType) {
                    TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
                    20 -> "5G_NR"
                    TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPA+"
                    TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
                    TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
                    TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
                    TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
                    TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
                    TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT"
                    TelephonyManager.NETWORK_TYPE_EHRPD -> "EHRPD"
                    TelephonyManager.NETWORK_TYPE_EVDO_0 -> "EVDO_0"
                    TelephonyManager.NETWORK_TYPE_EVDO_A -> "EVDO_A"
                    TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO_B"
                    TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA"
                    TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA"
                    TelephonyManager.NETWORK_TYPE_IDEN -> "IDEN"
                    TelephonyManager.NETWORK_TYPE_GSM -> "GSM"
                    TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "TD_SCDMA"
                    TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN"
                    else -> "UNKNOWN_${tele.dataNetworkType}"
                }
            } else {
                @Suppress("DEPRECATION")
                "TYPE_${tele.networkType}"
            }
        } catch (t: Throwable) {
            "UNKNOWN"
        }
    }
}
