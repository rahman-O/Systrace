package com.gis.systrace.service

import kotlin.test.Test
import kotlin.test.assertEquals

class ServiceStarterTest {

    @Test
    fun watchdogWorkName_isStable() {
        assertEquals("systrace_service_watchdog", ServiceStarter.WATCHDOG_WORK_NAME)
    }
}
