package com.gis.systrace.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.gis.systrace.core.common.AndroidConsoleLogger
import com.gis.systrace.core.common.CollectOptions
import com.gis.systrace.core.common.DeviceCollectorShared
import com.gis.systrace.data.SnapshotRepository
import com.gis.systrace.service.backend.BackendSyncEntryPoint
import com.gis.systrace.service.backend.MdmObserverEntryPoint
import com.gis.systrace.service.network.NetworkConnectivityObserver
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class DeviceCollectorForegroundService : Service() {
    companion object {
        const val CHANNEL_ID = "device_collector_channel_v3"
        const val NOTIF_ID = 1001
        private const val TAG = "DeviceCollectorFS"
        private const val COLLECTION_INTERVAL_MS = 30_000L

        private val BACKGROUND_COLLECT_OPTIONS = CollectOptions(
            includeInstalledApps = false,
            includeSensors = false,
        )

        private val INITIAL_COLLECT_OPTIONS = CollectOptions(
            includeInstalledApps = true,
            includeSensors = true,
        )

        @Volatile
        var isRunning: Boolean = false
            private set
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var collectionJob: Job? = null
    private lateinit var repository: SnapshotRepository
    private var networkObserver: NetworkConnectivityObserver? = null
    private var monitoringStarted = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val syncCounter = AtomicInteger(0)

    @Volatile
    private var lastSyncLabel: String = "Starting..."
    @Volatile
    private var lastSyncEpochMs: Long = System.currentTimeMillis()
    @Volatile
    private var isCollecting: Boolean = false
    @Volatile
    private var lastOnlineStatus: String = "UNKNOWN"

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        repository = EntryPointAccessors.fromApplication(
            applicationContext,
            SnapshotRepositoryEntryPoint::class.java,
        ).snapshotRepository()
        startBackendMonitoring()
        startNetworkObserver()
        createNotificationChannel()
        postNotificationUpdate()

        collectionJob = serviceScope.launch {
            collectAndPersist(INITIAL_COLLECT_OPTIONS)
            while (isActive) {
                val cycleStart = SystemClock.elapsedRealtime()
                collectAndPersist(BACKGROUND_COLLECT_OPTIONS)
                val elapsed = SystemClock.elapsedRealtime() - cycleStart
                val waitMs = (COLLECTION_INTERVAL_MS - elapsed).coerceAtLeast(1_000L)
                delay(waitMs)
            }
        }

        AndroidConsoleLogger.d(TAG, "Service created (interval=${COLLECTION_INTERVAL_MS}ms)")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        postNotificationUpdate()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        ServiceStarter.start(applicationContext)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        isRunning = false
        networkObserver?.unregister()
        networkObserver = null
        collectionJob?.cancel()
        serviceScope.cancel()
        try {
            EntryPointAccessors.fromApplication(
                applicationContext,
                BackendSyncEntryPoint::class.java,
            ).backendSyncService().stop()
        } catch (_: Throwable) {
        }
        super.onDestroy()
    }

    private fun startBackendMonitoring() {
        if (monitoringStarted) return
        monitoringStarted = true
        try {
            val backendEntry = EntryPointAccessors.fromApplication(
                applicationContext,
                BackendSyncEntryPoint::class.java,
            )
            EntryPointAccessors.fromApplication(
                applicationContext,
                MdmObserverEntryPoint::class.java,
            ).mdmSettingsObserver().start(serviceScope)
            backendEntry.backendSyncService().start(serviceScope)
        } catch (t: Throwable) {
            AndroidConsoleLogger.e(TAG, "Failed to start backend monitoring", t)
        }
    }

    private fun startNetworkObserver() {
        if (networkObserver != null) return
        val backendSync = try {
            EntryPointAccessors.fromApplication(
                applicationContext,
                BackendSyncEntryPoint::class.java,
            ).backendSyncService()
        } catch (_: Throwable) {
            null
        }
        networkObserver = NetworkConnectivityObserver(applicationContext) { hasInternet ->
            serviceScope.launch {
                collectAndPersist(BACKGROUND_COLLECT_OPTIONS)
                backendSync?.onConnectivityChanged(hasInternet)
            }
        }.also { it.register() }
    }

    private suspend fun collectAndPersist(options: CollectOptions = BACKGROUND_COLLECT_OPTIONS) {
        val cycle = syncCounter.incrementAndGet()
        isCollecting = true
        postNotificationUpdate()

        try {
            val snapshot = withContext(Dispatchers.Default) {
                DeviceCollectorShared.collect(applicationContext, options)
            }
            repository.saveSnapshot(snapshot)

            try {
                val backendEntry = EntryPointAccessors.fromApplication(
                    applicationContext,
                    BackendSyncEntryPoint::class.java,
                )
                val metricEvents = backendEntry.metricChangeTracker().detectChanges(snapshot)
                if (metricEvents.isNotEmpty()) {
                    backendEntry.backendSyncService().scheduleMetricEventBatch(metricEvents)
                }
                backendEntry.backendSyncService().scheduleUploadAfterSnapshot()
            } catch (t: Throwable) {
                AndroidConsoleLogger.e(TAG, "Failed to schedule post-snapshot upload", t)
            }

            lastSyncEpochMs = System.currentTimeMillis()
            lastSyncLabel = "Last sync: ${formatTimeOnly(lastSyncEpochMs)}"
            lastOnlineStatus = snapshot.onlineStatus
            isCollecting = false

            SnapshotLogFormatter.logSummary(snapshot, lastSyncLabel)
            postNotificationUpdate()
        } catch (t: Throwable) {
            isCollecting = false
            lastSyncLabel = "Sync failed ${formatTimeOnly(System.currentTimeMillis())}"
            AndroidConsoleLogger.e(TAG, "Collection cycle #$cycle failed", t)
            postNotificationUpdate()
        }
    }

    private fun postNotificationUpdate() {
        mainHandler.post { applyForegroundNotification() }
    }

    private fun applyForegroundNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID, notification)
    }

    private fun buildNotification(): Notification {
        val activityIntent = Intent().setClassName(packageName, "com.gis.systrace.MainActivity")
        val pending = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val statusLine = if (isCollecting) "Collecting now..." else lastSyncLabel
        val timeText = formatTimeOnly(lastSyncEpochMs)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SysTrace • $timeText")
            .setContentText(statusLine)
            .setSubText("$lastOnlineStatus • Cycle #${syncCounter.get()}")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(lastSyncEpochMs)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Device Collector",
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            nm.createNotificationChannel(channel)
        }
    }

    private fun formatTimeOnly(epochMs: Long): String {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return formatter.format(Date(epochMs))
    }
}
