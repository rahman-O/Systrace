package com.gis.systrace.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gis.systrace.core.common.AndroidConsoleLogger

class ServiceWatchdogWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            if (!DeviceCollectorForegroundService.isRunning) {
                AndroidConsoleLogger.d("ServiceWatchdogWorker", "Service not running, restarting")
                ServiceStarter.start(applicationContext)
            } else {
                AndroidConsoleLogger.d("ServiceWatchdogWorker", "Service is running")
            }
            Result.success()
        } catch (t: Throwable) {
            AndroidConsoleLogger.e("ServiceWatchdogWorker", "Watchdog failed", t)
            Result.retry()
        }
    }
}
