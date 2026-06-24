package com.gis.systrace.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gis.systrace.core.common.AndroidConsoleLogger
import com.gis.systrace.core.common.CollectOptions
import com.gis.systrace.core.common.DeviceCollectorShared
import com.gis.systrace.data.SnapshotRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SnapshotCollectorWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.Default) {
            try {
                AndroidConsoleLogger.d("SnapshotCollectorWorker", "Starting snapshot collection...")
                val snapshot = DeviceCollectorShared.collect(
                    applicationContext,
                    CollectOptions(includeInstalledApps = false, includeSensors = false),
                )
                SnapshotRepository(applicationContext).saveSnapshot(snapshot)
                ServiceStarter.start(applicationContext)
                AndroidConsoleLogger.d("SnapshotCollectorWorker", "Snapshot collected: ${snapshot.deviceModel}")
                Result.success()
            } catch (t: Throwable) {
                AndroidConsoleLogger.e("SnapshotCollectorWorker", "Error collecting snapshot", t)
                Result.retry()
            }
        }
    }
}
