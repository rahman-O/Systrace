package com.gis.systrace.service

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.gis.systrace.core.common.AndroidConsoleLogger
import java.util.concurrent.TimeUnit

object ServiceStarter {
    internal const val WATCHDOG_WORK_NAME = "systrace_service_watchdog"

    fun start(context: Context) {
        val appContext = context.applicationContext
        try {
            val intent = Intent(appContext, DeviceCollectorForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(intent)
            } else {
                appContext.startService(intent)
            }
            AndroidConsoleLogger.d("ServiceStarter", "Foreground service start requested")
        } catch (t: Throwable) {
            AndroidConsoleLogger.e("ServiceStarter", "Failed to start foreground service", t)
        }
        scheduleWatchdog(appContext)
    }

    fun scheduleWatchdog(context: Context) {
        val appContext = context.applicationContext
        try {
            val request = PeriodicWorkRequestBuilder<ServiceWatchdogWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().build())
                .build()
            WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
                WATCHDOG_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
            AndroidConsoleLogger.d("ServiceStarter", "Watchdog work scheduled")
        } catch (t: Throwable) {
            AndroidConsoleLogger.e("ServiceStarter", "Failed to schedule watchdog", t)
        }
    }
}
