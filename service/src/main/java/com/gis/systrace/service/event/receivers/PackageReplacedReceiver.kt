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

/** Fires on first launch after MDM installs or updates SysTrace. */
class PackageReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        AndroidConsoleLogger.d(TAG, "Package replaced — starting collector + sync")
        val appContext = context.applicationContext
        ServiceStarter.start(appContext)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val mdm = EntryPointAccessors.fromApplication(
                    appContext,
                    MdmObserverEntryPoint::class.java,
                ).mdmSettingsObserver()
                mdm.refreshAndSyncIfNeeded()
            } catch (t: Throwable) {
                AndroidConsoleLogger.e(TAG, "Post-install sync failed", t)
            }
        }
    }

    companion object {
        private const val TAG = "PackageReplaced"
    }
}
