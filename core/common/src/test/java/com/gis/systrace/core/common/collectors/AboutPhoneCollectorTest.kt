package com.gis.systrace.core.common.collectors

import com.gis.systrace.core.common.DeviceSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AboutPhoneCollectorTest {

    @Test
    fun buildSimStatusSummary_formatsEachSlot() {
        val telephony = TelephonyData(
            hasTelephonyData = true,
            simSlots = listOf(
                DeviceSnapshot.SimSlotDetail(slotIndex = 0, simState = "READY"),
                DeviceSnapshot.SimSlotDetail(slotIndex = 1, simState = "ABSENT"),
            ),
        )

        val summary = AboutPhoneCollector.buildSimStatusSummary(telephony)

        assertEquals("SIM 0: READY | SIM 1: ABSENT", summary)
    }

    @Test
    fun buildSimStatusSummary_returnsNoSimWhenEmpty() {
        val telephony = TelephonyData(hasTelephonyData = false, simSlots = emptyList())

        val summary = AboutPhoneCollector.buildSimStatusSummary(telephony)

        assertEquals("No telephony", summary)
    }

    @Test
    fun readBasebandVersion_doesNotThrowOnJvm() {
        val baseband = AboutPhoneCollector.readBasebandVersion()
        // May be null on JVM; must not throw
        assertTrue(baseband == null || baseband.isNotBlank())
    }
}
