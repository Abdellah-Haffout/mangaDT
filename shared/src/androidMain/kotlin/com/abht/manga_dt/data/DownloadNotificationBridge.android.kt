package com.abht.manga_dt.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.abht.manga_dt.models.DownloadTask

class DownloadNotificationService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Downloading"
        val text = intent?.getStringExtra(EXTRA_TEXT) ?: "Downloading chapter..."
        val progress = intent?.getIntExtra(EXTRA_PROGRESS, 0) ?: 0
        val max = intent?.getIntExtra(EXTRA_MAX, 100) ?: 100

        val notification = DownloadNotificationBridge.buildOngoingNotification(this, title, text, progress, max)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                DownloadNotificationBridge.ONGOING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(DownloadNotificationBridge.ONGOING_NOTIFICATION_ID, notification)
        }

        return START_NOT_STICKY
    }

    companion object {
        const val ACTION_STOP = "com.abht.manga_dt.STOP_DOWNLOAD_SERVICE"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_PROGRESS = "extra_progress"
        const val EXTRA_MAX = "extra_max"
    }
}

actual object DownloadNotificationBridge {
    const val ONGOING_NOTIFICATION_ID = 40401
    private const val CHANNEL_DOWNLOADING = "manga_downloads_progress"
    private const val CHANNEL_COMPLETED = "manga_downloads_completed"
    private var isChannelsCreated = false

    private fun ensureChannels(context: Context) {
        if (isChannelsCreated) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            
            val downloadingChannel = NotificationChannel(
                CHANNEL_DOWNLOADING,
                "Manga Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active manga download progress"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            
            val completedChannel = NotificationChannel(
                CHANNEL_COMPLETED,
                "Download Completed",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for finished manga downloads"
                setShowBadge(true)
            }

            manager.createNotificationChannel(downloadingChannel)
            manager.createNotificationChannel(completedChannel)
            isChannelsCreated = true
        }
    }

    fun buildOngoingNotification(
        context: Context,
        title: String,
        text: String,
        progress: Int,
        max: Int
    ): android.app.Notification {
        ensureChannels(context)

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = if (launchIntent != null) {
            PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null

        val builder = NotificationCompat.Builder(context, CHANNEL_DOWNLOADING)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)

        if (max > 0) {
            builder.setProgress(max, progress, false)
        } else {
            builder.setProgress(0, 0, true)
        }

        return builder.build()
    }

    actual fun updateDownloadNotification(
        task: DownloadTask,
        progress: Float,
        currentFileIndex: Int,
        totalFiles: Int,
        remainingTasksCount: Int
    ) {
        val context = SettingsStorage.appContext ?: return
        ensureChannels(context)

        val title = "Downloading ${task.title}"
        val percent = (progress * 100).toInt().coerceIn(0, 100)
        val text = "${task.subtitle} • $percent% ($currentFileIndex/$totalFiles)" +
                if (remainingTasksCount > 1) " (+${remainingTasksCount - 1} in queue)" else ""

        try {
            val serviceIntent = Intent(context, DownloadNotificationService::class.java).apply {
                putExtra(DownloadNotificationService.EXTRA_TITLE, title)
                putExtra(DownloadNotificationService.EXTRA_TEXT, text)
                putExtra(DownloadNotificationService.EXTRA_PROGRESS, percent)
                putExtra(DownloadNotificationService.EXTRA_MAX, 100)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            try {
                val notification = buildOngoingNotification(context, title, text, percent, 100)
                NotificationManagerCompat.from(context).notify(ONGOING_NOTIFICATION_ID, notification)
            } catch (ignored: Exception) {}
        }
    }

    actual fun notifyDownloadCompleted(task: DownloadTask) {
        val context = SettingsStorage.appContext ?: return
        ensureChannels(context)

        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = if (launchIntent != null) {
                PendingIntent.getActivity(
                    context,
                    task.id.hashCode(),
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else null

            val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETED)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Download Complete")
                .setContentText("${task.title} - ${task.subtitle}")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(task.id.hashCode(), notification)
        } catch (ignored: Exception) {}
    }

    actual fun notifyDownloadError(task: DownloadTask, message: String?) {
        val context = SettingsStorage.appContext ?: return
        ensureChannels(context)

        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETED)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("Download Failed")
                .setContentText("${task.title} - ${task.subtitle}: ${message ?: "Network error"}")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(task.id.hashCode(), notification)
        } catch (ignored: Exception) {}
    }

    actual fun cancelDownloadNotification() {
        val context = SettingsStorage.appContext ?: return
        try {
            val stopIntent = Intent(context, DownloadNotificationService::class.java).apply {
                action = DownloadNotificationService.ACTION_STOP
            }
            context.startService(stopIntent)
        } catch (ignored: Exception) {}

        try {
            NotificationManagerCompat.from(context).cancel(ONGOING_NOTIFICATION_ID)
        } catch (ignored: Exception) {}
    }
}
