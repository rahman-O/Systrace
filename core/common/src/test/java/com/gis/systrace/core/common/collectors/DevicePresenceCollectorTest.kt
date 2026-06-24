package com.gis.systrace.core.common.collectors

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DevicePresenceCollectorTest {

    @Test
    fun resolveOnlineStatus_whenUnlockedAndOnline_returnsOnline() {
        val (status, reason) = DevicePresenceCollector.resolveOnlineStatus(
            isScreenLocked = false,
            isInternetConnected = true,
        )
        assertEquals("ONLINE", status)
        assertNull(reason)
    }

    @Test
    fun resolveOnlineStatus_whenLocked_returnsOfflineLocked() {
        val (status, reason) = DevicePresenceCollector.resolveOnlineStatus(
            isScreenLocked = true,
            isInternetConnected = true,
        )
        assertEquals("OFFLINE", status)
        assertEquals("LOCKED", reason)
    }

    @Test
    fun resolveOnlineStatus_whenNoInternet_returnsOfflineNoInternet() {
        val (status, reason) = DevicePresenceCollector.resolveOnlineStatus(
            isScreenLocked = false,
            isInternetConnected = false,
        )
        assertEquals("OFFLINE", status)
        assertEquals("NO_INTERNET", reason)
    }

    @Test
    fun resolveOnlineStatus_whenLockedAndNoInternet_returnsBothReasons() {
        val (status, reason) = DevicePresenceCollector.resolveOnlineStatus(
            isScreenLocked = true,
            isInternetConnected = false,
        )
        assertEquals("OFFLINE", status)
        assertEquals("LOCKED,NO_INTERNET", reason)
    }
}
