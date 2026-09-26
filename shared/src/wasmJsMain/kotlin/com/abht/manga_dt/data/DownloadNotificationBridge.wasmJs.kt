package com.abht.manga_dt.data

import com.abht.manga_dt.models.DownloadTask

actual object DownloadNotificationBridge {
    actual fun updateDownloadNotification(
        task: DownloadTask,
        progress: Float,
        currentFileIndex: Int,
        totalFiles: Int,
        remainingTasksCount: Int
    ) {}

    actual fun notifyDownloadCompleted(task: DownloadTask) {}
    actual fun notifyDownloadError(task: DownloadTask, message: String?) {}
    actual fun cancelDownloadNotification() {}
}
