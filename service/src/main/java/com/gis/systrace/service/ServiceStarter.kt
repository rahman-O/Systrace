package com.gis.systrace.service

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.gis.systrace.core.common.AndroidConsoleLogger

object ServiceStarter {
    private const val TAG = "ServiceStarter"

    fun start(context: Context) {
        val intent = Intent(context, DeviceCollectorForegroundService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
            AndroidConsoleLogger.d(TAG, "Foreground service start requested")
        } catch (t: Throwable) {
            AndroidConsoleLogger.e(TAG, "Failed to start foreground service", t)
        }
    }
}
