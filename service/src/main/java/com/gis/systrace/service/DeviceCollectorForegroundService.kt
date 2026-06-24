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
        private const val COLLECTION_INTERVAL_MS = 60_000L

        private val BACKGROUND_COLLECT_OPTIONS = CollectOptions(
            includeInstalledApps = false,
            includeSensors = false,
        )

        @Volatile
        var isRunning: Boolean = false
            private set
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var collectionJob: Job? = null
    private lateinit var repository: SnapshotRepository
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
        repository = SnapshotRepository(applicationContext)
        createNotificationChannel()
        postNotificationUpdate()

        collectionJob = serviceScope.launch {
            while (isActive) {
                val cycleStart = SystemClock.elapsedRealtime()
                collectAndPersist()
                val elapsed = SystemClock.elapsedRealtime() - cycleStart
                val waitMs = (COLLECTION_INTERVAL_MS - elapsed).coerceAtLeast(1_000L)
                AndroidConsoleLogger.d(TAG, "Next cycle in ${waitMs}ms (last collect took ${elapsed}ms)")
                delay(waitMs)
            }
        }

        AndroidConsoleLogger.d(TAG, "Service created and collection loop started (interval=${COLLECTION_INTERVAL_MS}ms)")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AndroidConsoleLogger.d(TAG, "onStartCommand called")
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
        collectionJob?.cancel()
        serviceScope.cancel()
        AndroidConsoleLogger.d(TAG, "onDestroy")
        super.onDestroy()
    }

    private suspend fun collectAndPersist() {
        val cycle = syncCounter.incrementAndGet()
        isCollecting = true
        postNotificationUpdate()
        AndroidConsoleLogger.d(TAG, "Collection cycle #$cycle started")

        val startedAt = System.currentTimeMillis()
        try {
            val snapshot = withContext(Dispatchers.Default) {
                DeviceCollectorShared.collect(applicationContext, BACKGROUND_COLLECT_OPTIONS)
            }
            repository.saveSnapshot(snapshot)

            lastSyncEpochMs = System.currentTimeMillis()
            lastSyncLabel = formatSyncLabel(lastSyncEpochMs)
            lastOnlineStatus = snapshot.onlineStatus
            isCollecting = false

            SnapshotLogFormatter.logSummary(snapshot, lastSyncLabel)
            AndroidConsoleLogger.d(
                TAG,
                "Presence=${snapshot.onlineStatus} locked=${snapshot.isScreenLocked} internet=${snapshot.isInternetConnected} reason=${snapshot.offlineReason ?: "none"}",
            )
            postNotificationUpdate()
            AndroidConsoleLogger.d(
                TAG,
                "Collection cycle #$cycle completed in ${System.currentTimeMillis() - startedAt}ms: $lastSyncLabel",
            )
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
        AndroidConsoleLogger.d(
            TAG,
            "Notification applied on main thread: $lastSyncLabel (collecting=$isCollecting, cycle=${syncCounter.get()})",
        )
    }

    private fun buildNotification(): Notification {
        val activityIntent = Intent().setClassName(packageName, "com.gis.systrace.MainActivity")
        val pending = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val statusLine = if (isCollecting) {
            "Collecting now..."
        } else {
            lastSyncLabel
        }
        val timeText = formatTimeOnly(lastSyncEpochMs)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SysTrace • $timeText")
            .setContentText(statusLine)
            .setSubText("$lastOnlineStatus • Cycle #${syncCounter.get()}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$statusLine\nPresence: $lastOnlineStatus\nNext run every 60 seconds"),
            )
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(lastSyncEpochMs)
            .setSortKey(lastSyncEpochMs.toString())
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.deleteNotificationChannel("device_collector_channel")
            nm.deleteNotificationChannel("device_collector_channel_v2")
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Device Collector",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "SysTrace background device snapshot collection"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun formatSyncLabel(epochMs: Long): String {
        return "Last sync: ${formatTimeOnly(epochMs)}"
    }

    private fun formatTimeOnly(epochMs: Long): String {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return formatter.format(Date(epochMs))
    }
}
