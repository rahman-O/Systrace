package com.gis.systrace.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gis.systrace.core.common.AndroidConsoleLogger

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        AndroidConsoleLogger.d("BootReceiver", "onReceive: action=${intent?.action}")
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            -> ServiceStarter.start(context)
        }
    }
}
