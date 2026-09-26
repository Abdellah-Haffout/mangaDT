package com.abht.manga_dt.data

import com.abht.manga_dt.models.DownloadTask

expect object DownloadNotificationBridge {
    fun updateDownloadNotification(
        task: DownloadTask,
        progress: Float,
        currentFileIndex: Int,
        totalFiles: Int,
        remainingTasksCount: Int
    )
    fun notifyDownloadCompleted(task: DownloadTask)
    fun notifyDownloadError(task: DownloadTask, message: String?)
    fun cancelDownloadNotification()
}
