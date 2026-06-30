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

/**
 * Reacts to Headwind MDM push broadcasts when configuration or app settings change.
 */
class HeadwindPushReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != ACTION_CONFIG_UPDATED && action != ACTION_APP_CONFIG_UPDATED) {
            return
        }
        AndroidConsoleLogger.d(TAG, "Headwind push received: $action")
        ServiceStarter.start(context.applicationContext)
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val entry = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    MdmObserverEntryPoint::class.java,
                )
                entry.mdmSettingsObserver().refreshAndSyncIfNeeded()
            } catch (t: Throwable) {
                AndroidConsoleLogger.e(TAG, "Headwind push sync failed", t)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "HeadwindPushReceiver"
        const val ACTION_CONFIG_UPDATED = "com.hmdm.push.configUpdated"
        const val ACTION_APP_CONFIG_UPDATED = "com.hmdm.push.appConfigUpdated"
    }
}
