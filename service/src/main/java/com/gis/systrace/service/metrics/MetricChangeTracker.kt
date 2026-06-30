package com.gis.systrace.service.metrics

import com.gis.systrace.core.common.DeviceSnapshot
import com.gis.systrace.core.domain.sync.MetricEventUpload
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class PendingMetricEvent(
    val eventType: String,
    val payloadJson: String,
)

@Singleton
class MetricChangeTracker @Inject constructor() {
    private var last: DeviceSnapshot? = null

    fun detectChanges(current: DeviceSnapshot): List<PendingMetricEvent> {
        val prev = last
        last = current
        if (prev == null) return emptyList()

        val events = mutableListOf<PendingMetricEvent>()

        val prevLevel = prev.batteryPercentage
        val curLevel = current.batteryPercentage
        if (prevLevel != null && curLevel != null && kotlin.math.abs(curLevel - prevLevel) >= BATTERY_LEVEL_DELTA) {
            events += metricEvent(
                "BATTERY_LEVEL_CHANGED",
                JSONObject().apply {
                    put("battery", JSONObject().put("level", curLevel))
                },
            )
        }

        val prevCharging = prev.chargingState
        val curCharging = current.chargingState
        if (prevCharging != null && curCharging != null && prevCharging != curCharging) {
            val charging = curCharging.equals("charging", ignoreCase = true) ||
                curCharging.equals("full", ignoreCase = true)
            events += metricEvent(
                "BATTERY_CHARGING_CHANGED",
                JSONObject().apply {
                    put("battery", JSONObject().put("charging", charging))
                },
            )
        }

        val prevScreenOn = prev.screenOn
        val curScreenOn = current.screenOn
        val prevBrightness = prev.screenBrightness
        val curBrightness = current.screenBrightness
        val prevOrientation = prev.screenOrientation
        val curOrientation = current.screenOrientation
        if (prevScreenOn != curScreenOn || prevBrightness != curBrightness || prevOrientation != curOrientation) {
            events += metricEvent(
                "SCREEN_CHANGED",
                JSONObject().apply {
                    curScreenOn?.let { put("screen_on", it) }
                    curBrightness?.let { put("screen_brightness", it) }
                    curOrientation?.let { put("screen_orientation", it) }
                },
            )
        }

        val prevTemp = prev.batteryTemperatureCelsius
        val curTemp = current.batteryTemperatureCelsius
        if (prevTemp != null && curTemp != null && kotlin.math.abs(curTemp - prevTemp) >= BATTERY_TEMP_DELTA_C) {
            events += metricEvent(
                "BATTERY_TEMP_CHANGED",
                JSONObject().apply {
                    put("battery", JSONObject().put("temperature", curTemp.toInt()))
                },
            )
        }

        val prevTransport = prev.networkType
        val curTransport = current.networkType
        val prevWifi = prev.wifiRssiDbm
        val curWifi = current.wifiRssiDbm
        val prevSim = prev.simSignalDbm
        val curSim = current.simSignalDbm

        if (prevTransport != curTransport || prev.connectionStatus != current.connectionStatus) {
            events += metricEvent(
                "NETWORK_CHANGED",
                JSONObject().apply {
                    put("network", JSONObject().apply {
                        curTransport?.let { put("transport", it.lowercase()) }
                        put("wifi_connected", current.connectionStatus == "CONNECTED" && curTransport == "WIFI")
                        curWifi?.let { put("wifi_rssi_dbm", it) }
                        current.simSlots.firstOrNull()?.networkType?.let { put("sim_network_type", it.lowercase()) }
                        curSim?.let { put("sim_signal_dbm", it) }
                    })
                },
            )
        } else {
            if (prevWifi != null && curWifi != null && kotlin.math.abs(curWifi - prevWifi) >= SIGNAL_DELTA_DBM) {
                events += metricEvent(
                    "NETWORK_CHANGED",
                    JSONObject().apply {
                        put("network", JSONObject().apply {
                            put("wifi_rssi_dbm", curWifi)
                            put("wifi_connected", true)
                        })
                    },
                )
            }
            if (prevSim != null && curSim != null && kotlin.math.abs(curSim - prevSim) >= SIGNAL_DELTA_DBM) {
                events += metricEvent(
                    "SIM_SIGNAL_CHANGED",
                    JSONObject().apply {
                        put("network", JSONObject().put("sim_signal_dbm", curSim))
                    },
                )
            }
        }

        return events
    }

    private fun metricEvent(type: String, payload: JSONObject): PendingMetricEvent {
        return PendingMetricEvent(eventType = type, payloadJson = payload.toString())
    }

    companion object {
        private const val BATTERY_LEVEL_DELTA = 2
        private const val BATTERY_TEMP_DELTA_C = 2f
        private const val SIGNAL_DELTA_DBM = 5

        fun toMetricUpload(pending: PendingMetricEvent, now: Long, index: Int): MetricEventUpload =
            MetricEventUpload(
                eventId = "metric-$now-$index",
                eventType = pending.eventType,
                timestamp = now,
                payloadJson = pending.payloadJson,
                title = pending.eventType.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() },
                description = "Live metric change",
            )
    }
}
