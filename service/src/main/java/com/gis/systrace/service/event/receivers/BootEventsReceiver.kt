package com.gis.systrace.service.event.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gis.systrace.core.common.AndroidConsoleLogger
import com.gis.systrace.service.ServiceStarter
import com.gis.systrace.service.backend.MdmObserverEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_PACKAGE_REPLACED
        ) {
            return
        }
        AndroidConsoleLogger.d(TAG, "Boot/package event: $action")
        ServiceStarter.start(context.applicationContext)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val entry = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    MdmObserverEntryPoint::class.java,
                )
                entry.mdmSettingsObserver().refreshAndSyncIfNeeded()
            } catch (t: Throwable) {
                AndroidConsoleLogger.e(TAG, "Post-boot MDM sync failed", t)
            }
        }
    }

    companion object {
        private const val TAG = "BootEventsReceiver"
    }
}
