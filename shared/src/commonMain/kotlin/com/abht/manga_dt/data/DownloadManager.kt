package com.abht.manga_dt.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.abht.manga_dt.models.Chapter
import com.abht.manga_dt.models.DownloadStatus
import com.abht.manga_dt.models.DownloadTask
import com.abht.manga_dt.models.Manga
import com.abht.manga_dt.models.ReaderPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class DownloadedChapterInfo(
    val mangaTitle: String,
    val mangaUrl: String,
    val mangaCover: String,
    val sourceId: String,
    val chapterTitle: String,
    val chapterUrl: String,
    val chapterNumber: Float,
    val pageCount: Int,
    val localDirPath: String,
    val pageFilePaths: List<String>,
    val downloadTimestamp: Long,
    val sizeBytes: Long
)

data class DownloadedMangaGroup(
    val mangaTitle: String,
    val mangaUrl: String,
    val mangaCover: String,
    val sourceId: String,
    val chapters: List<DownloadedChapterInfo>,
    val totalSizeBytes: Long
)

object DownloadManager {
    private val engine = PlatformDownloadEngine()
    private val scope = CoroutineScope(Dispatchers.Default)
    private val queueMutex = Mutex()
    private var workerJob: Job? = null
    private val sourceManager = MangaSourceManager()

    // Observable States
    val downloadTasks = mutableStateListOf<DownloadTask>()
    val downloadedChapters = mutableStateMapOf<String, DownloadedChapterInfo>()
    var isWorkerRunning by mutableStateOf(false)
        private set

    init {
        loadDownloadedChapters()
    }

    private fun sanitizeFileName(name: String): String {
        return name
            .replace(Regex("[\\\\/:*?\"<>|\\r\\n\\t]"), "_")
            .trim { it <= ' ' || it == '.' }
            .take(60)
            .ifBlank { "unnamed" }
    }

    // --- Persistence ---

    private fun loadDownloadedChapters() {
        try {
            val raw = SettingsStorage.getString("mangadt_downloads_registry_v1", "")
            if (raw.isBlank()) return
            val entries = raw.split(";;;;;").filter { it.isNotBlank() }
            downloadedChapters.clear()
            for (entry in entries) {
                val parts = entry.split("|||||")
                if (parts.size >= 12) {
                    val pagePaths = if (parts[9].isNotBlank()) parts[9].split("~~~~~") else emptyList()
                    val info = DownloadedChapterInfo(
                        mangaTitle = parts[0],
                        mangaUrl = parts[1],
                        mangaCover = parts[2],
                        sourceId = parts[3],
                        chapterTitle = parts[4],
                        chapterUrl = parts[5],
                        chapterNumber = parts[6].toFloatOrNull() ?: 0f,
                        pageCount = parts[7].toIntOrNull() ?: 0,
                        localDirPath = parts[8],
                        pageFilePaths = pagePaths,
                        downloadTimestamp = parts[10].toLongOrNull() ?: 0L,
                        sizeBytes = parts[11].toLongOrNull() ?: 0L
                    )
                    // Verify that chapter directory exists or has pages
                    if (info.pageFilePaths.any { engine.fileExists(it) }) {
                        downloadedChapters[info.chapterUrl] = info
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveDownloadedChapters() {
        try {
            val serialized = downloadedChapters.values.joinToString(";;;;;") { info ->
                listOf(
                    info.mangaTitle,
                    info.mangaUrl,
                    info.mangaCover,
                    info.sourceId,
                    info.chapterTitle,
                    info.chapterUrl,
                    info.chapterNumber.toString(),
                    info.pageCount.toString(),
                    info.localDirPath,
                    info.pageFilePaths.joinToString("~~~~~"),
                    info.downloadTimestamp.toString(),
                    info.sizeBytes.toString()
                ).joinToString("|||||")
            }
            SettingsStorage.setString("mangadt_downloads_registry_v1", serialized)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Query APIs ---

    fun isChapterDownloaded(chapterUrl: String): Boolean {
        if (chapterUrl.isBlank()) return false
        val info = downloadedChapters[chapterUrl] ?: return false
        return info.pageFilePaths.isNotEmpty() && engine.fileExists(info.pageFilePaths.first())
    }

    fun getDownloadedChapter(chapterUrl: String): DownloadedChapterInfo? {
        return downloadedChapters[chapterUrl]
    }

    fun registerExternalDownloadedChapter(info: DownloadedChapterInfo) {
        downloadedChapters[info.chapterUrl] = info
        saveDownloadedChapters()
    }

    fun getSanitizedChapterDirectory(mangaTitle: String, chapterTitle: String): String {
        val downloadsDir = engine.getDownloadsDirectory()
        val mangaDirName = sanitizeFileName(mangaTitle)
        val chapterDirName = sanitizeFileName(chapterTitle)
        return "$downloadsDir/$mangaDirName/$chapterDirName"
    }

    fun packageChapterToZip(chapterUrl: String): Pair<DownloadedChapterInfo, ByteArray>? {
        val info = downloadedChapters[chapterUrl] ?: return null
        if (info.localDirPath.isBlank()) return null
        val zipBytes = engine.packageDirectoryToZip(info.localDirPath) ?: return null
        return Pair(info, zipBytes)
    }

    fun unpackChapterZip(
        mangaTitle: String,
        mangaUrl: String,
        mangaCover: String,
        sourceId: String,
        chapterTitle: String,
        chapterUrl: String,
        chapterNumber: Float,
        pageCount: Int,
        zipBytes: ByteArray
    ): DownloadedChapterInfo? {
        val targetDir = getSanitizedChapterDirectory(mangaTitle, chapterTitle)
        val extractedPaths = engine.extractZipToDirectory(zipBytes, targetDir)
        if (extractedPaths.isEmpty()) return null

        val totalSize = engine.getDirectorySize(targetDir)
        val info = DownloadedChapterInfo(
            mangaTitle = mangaTitle,
            mangaUrl = mangaUrl,
            mangaCover = mangaCover,
            sourceId = sourceId,
            chapterTitle = chapterTitle,
            chapterUrl = chapterUrl,
            chapterNumber = chapterNumber,
            pageCount = extractedPaths.size,
            localDirPath = targetDir,
            pageFilePaths = extractedPaths,
            downloadTimestamp = currentTimeMillis(),
            sizeBytes = totalSize
        )
        registerExternalDownloadedChapter(info)
        return info
    }

    fun getDownloadedPages(chapterUrl: String): List<ReaderPage>? {
        val info = downloadedChapters[chapterUrl] ?: return null
        if (info.pageFilePaths.isEmpty()) return null
        val existing = info.pageFilePaths.filter { engine.fileExists(it) }
        if (existing.isEmpty()) return null

        return existing.mapIndexed { index, path ->
            ReaderPage(
                url = engine.getFileUri(path),
                pageNumber = index + 1
            )
        }
    }

    fun getDownloadTask(chapterUrl: String): DownloadTask? {
        return downloadTasks.firstOrNull { it.id == chapterUrl }
    }

    fun getDownloadedMangaGroups(): List<DownloadedMangaGroup> {
        val grouped = downloadedChapters.values.groupBy { it.mangaUrl.ifBlank { it.mangaTitle } }
        return grouped.map { (_, chapters) ->
            val first = chapters.first()
            val totalSize = chapters.sumOf { it.sizeBytes }
            DownloadedMangaGroup(
                mangaTitle = first.mangaTitle,
                mangaUrl = first.mangaUrl,
                mangaCover = first.mangaCover,
                sourceId = first.sourceId,
                chapters = chapters.sortedByDescending { it.chapterNumber },
                totalSizeBytes = totalSize
            )
        }.sortedByDescending { it.chapters.maxOfOrNull { ch -> ch.downloadTimestamp } ?: 0L }
    }

    fun getTotalStorageUsed(): Long {
        return downloadedChapters.values.sumOf { it.sizeBytes }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> "${((gb * 10).toInt() / 10.0)} GB"
            mb >= 1.0 -> "${((mb * 10).toInt() / 10.0)} MB"
            kb >= 1.0 -> "${((kb * 10).toInt() / 10.0)} KB"
            else -> "$bytes B"
        }
    }

    // --- Enqueue & Management APIs ---

    fun enqueueDownload(
        manga: Manga,
        chapter: Chapter
    ) {
        if (isChapterDownloaded(chapter.url)) return

        // Persistently save manga metadata and chapters for offline access
        OfflineMangaManager.saveManga(manga)

        scope.launch {
            queueMutex.withLock {
                val existingIndex = downloadTasks.indexOfFirst { it.id == chapter.url }
                if (existingIndex >= 0) {
                    val current = downloadTasks[existingIndex]
                    if (current.status == DownloadStatus.PAUSED || current.status == DownloadStatus.ERROR) {
                        downloadTasks[existingIndex] = current.copy(
                            status = DownloadStatus.QUEUED,
                            progress = 0f,
                            sourceId = manga.source.ifBlank { current.sourceId },
                            mangaUrl = manga.url.ifBlank { current.mangaUrl }
                        )
                    }
                } else {
                    val newTask = DownloadTask(
                        id = chapter.url,
                        title = manga.title,
                        subtitle = if (chapter.title.isNotBlank()) chapter.title else "Chapter ${chapter.chapterNumber}",
                        progress = 0f,
                        status = DownloadStatus.QUEUED,
                        thumbnailUrl = manga.thumbnailUrl,
                        sourceId = manga.source,
                        mangaUrl = manga.url,
                        chapterNumber = chapter.chapterNumber
                    )
                    downloadTasks.add(newTask)
                }
            }
            startWorkerIfNeeded()
        }
    }

    fun enqueueBatchDownload(
        manga: Manga,
        chapters: List<Chapter>
    ) {
        // Persistently save manga metadata and chapters for offline access
        OfflineMangaManager.saveManga(manga)

        scope.launch {
            queueMutex.withLock {
                for (chapter in chapters) {
                    if (isChapterDownloaded(chapter.url)) continue
                    val existingIndex = downloadTasks.indexOfFirst { it.id == chapter.url }
                    if (existingIndex >= 0) {
                        val current = downloadTasks[existingIndex]
                        if (current.status == DownloadStatus.PAUSED || current.status == DownloadStatus.ERROR) {
                            downloadTasks[existingIndex] = current.copy(
                                status = DownloadStatus.QUEUED,
                                progress = 0f,
                                sourceId = manga.source.ifBlank { current.sourceId },
                                mangaUrl = manga.url.ifBlank { current.mangaUrl }
                            )
                        }
                    } else {
                        val newTask = DownloadTask(
                            id = chapter.url,
                            title = manga.title,
                            subtitle = if (chapter.title.isNotBlank()) chapter.title else "Chapter ${chapter.chapterNumber}",
                            progress = 0f,
                            status = DownloadStatus.QUEUED,
                            thumbnailUrl = manga.thumbnailUrl,
                            sourceId = manga.source,
                            mangaUrl = manga.url,
                            chapterNumber = chapter.chapterNumber
                        )
                        downloadTasks.add(newTask)
                    }
                }
            }
            startWorkerIfNeeded()
        }
    }

    fun pauseTask(taskId: String) {
        val index = downloadTasks.indexOfFirst { it.id == taskId }
        if (index >= 0) {
            val current = downloadTasks[index]
            if (current.status == DownloadStatus.DOWNLOADING || current.status == DownloadStatus.QUEUED) {
                downloadTasks[index] = current.copy(status = DownloadStatus.PAUSED)
            }
        }
    }

    fun resumeTask(taskId: String) {
        val index = downloadTasks.indexOfFirst { it.id == taskId }
        if (index >= 0) {
            val current = downloadTasks[index]
            if (current.status == DownloadStatus.PAUSED || current.status == DownloadStatus.ERROR) {
                downloadTasks[index] = current.copy(status = DownloadStatus.QUEUED)
                startWorkerIfNeeded()
            }
        }
    }

    fun cancelTask(taskId: String) {
        val index = downloadTasks.indexOfFirst { it.id == taskId }
        if (index >= 0) {
            downloadTasks.removeAt(index)
        }
    }

    fun pauseAll() {
        for (i in downloadTasks.indices) {
            val task = downloadTasks[i]
            if (task.status == DownloadStatus.DOWNLOADING || task.status == DownloadStatus.QUEUED) {
                downloadTasks[i] = task.copy(status = DownloadStatus.PAUSED)
            }
        }
    }

    fun resumeAll() {
        for (i in downloadTasks.indices) {
            val task = downloadTasks[i]
            if (task.status == DownloadStatus.PAUSED || task.status == DownloadStatus.ERROR) {
                downloadTasks[i] = task.copy(status = DownloadStatus.QUEUED)
            }
        }
        startWorkerIfNeeded()
    }

    fun cancelAll() {
        downloadTasks.clear()
    }

    fun clearCompletedTasks() {
        downloadTasks.removeAll { it.status == DownloadStatus.COMPLETED }
    }

    fun deleteDownloadedChapter(chapterUrl: String) {
        val info = downloadedChapters.remove(chapterUrl) ?: return
        saveDownloadedChapters()
        scope.launch {
            if (info.localDirPath.isNotBlank()) {
                engine.deleteDirectory(info.localDirPath)
            }
        }
    }

    fun deleteMangaDownloads(mangaUrl: String) {
        val matching = downloadedChapters.values.filter { it.mangaUrl == mangaUrl || it.mangaTitle == mangaUrl }
        matching.forEach { ch ->
            downloadedChapters.remove(ch.chapterUrl)
        }
        saveDownloadedChapters()
        scope.launch {
            matching.forEach { ch ->
                if (ch.localDirPath.isNotBlank()) {
                    engine.deleteDirectory(ch.localDirPath)
                }
            }
        }
    }

    /**
     * Checks if a downloaded chapter has been read based on reading statistics and history.
     */
    fun isChapterRead(chapterUrl: String, mangaTitle: String = "", mangaUrl: String = ""): Boolean {
        if (StatisticsManager.mangaStatsMap.values.any { it.chaptersRead.contains(chapterUrl) }) {
            return true
        }
        val hist = HistoryManager.historyEntries.firstOrNull { 
            it.chapterUrl == chapterUrl || 
            (mangaTitle.isNotBlank() && it.mangaTitle.equals(mangaTitle, ignoreCase = true) && it.chapterUrl == chapterUrl)
        }
        if (hist != null && (hist.lastPage >= hist.totalPages || hist.lastPage > 1)) {
            return true
        }
        return false
    }

    /**
     * Calculates the count and total size of downloaded chapters that have been read.
     */
    fun getReadDownloadedChaptersCount(mangaUrl: String = "", mangaTitle: String = ""): Pair<Int, Long> {
        val readList = downloadedChapters.values.filter { ch ->
            val matchesManga = mangaUrl.isBlank() || ch.mangaUrl == mangaUrl || (mangaTitle.isNotBlank() && ch.mangaTitle.equals(mangaTitle, ignoreCase = true))
            matchesManga && isChapterRead(ch.chapterUrl, ch.mangaTitle, ch.mangaUrl)
        }
        val count = readList.size
        val bytes = readList.sumOf { it.sizeBytes }
        return Pair(count, bytes)
    }

    /**
     * Deletes all downloaded chapters that have been read.
     * Returns Pair(deletedCount, freedBytes).
     */
    fun deleteReadChapters(mangaUrl: String = "", mangaTitle: String = ""): Pair<Int, Long> {
        val readList = downloadedChapters.values.filter { ch ->
            val matchesManga = mangaUrl.isBlank() || ch.mangaUrl == mangaUrl || (mangaTitle.isNotBlank() && ch.mangaTitle.equals(mangaTitle, ignoreCase = true))
            matchesManga && isChapterRead(ch.chapterUrl, ch.mangaTitle, ch.mangaUrl)
        }
        if (readList.isEmpty()) return Pair(0, 0L)

        var deletedCount = 0
        var freedBytes = 0L

        readList.forEach { ch ->
            val info = downloadedChapters.remove(ch.chapterUrl)
            if (info != null) {
                deletedCount++
                freedBytes += info.sizeBytes
                scope.launch {
                    if (info.localDirPath.isNotBlank()) {
                        engine.deleteDirectory(info.localDirPath)
                    }
                }
            }
        }

        if (deletedCount > 0) {
            saveDownloadedChapters()
        }
        return Pair(deletedCount, freedBytes)
    }

    // --- Worker Loop ---

    private fun startWorkerIfNeeded() {
        if (isWorkerRunning) return
        val hasQueued = downloadTasks.any { it.status == DownloadStatus.QUEUED }
        if (!hasQueued) return

        isWorkerRunning = true
        workerJob?.cancel()
        workerJob = scope.launch {
            try {
                while (true) {
                    val nextTask = queueMutex.withLock {
                        downloadTasks.firstOrNull { it.status == DownloadStatus.QUEUED }
                    } ?: break

                    processTask(nextTask)
                    delay(300)
                }
            } finally {
                isWorkerRunning = false
                val remaining = downloadTasks.count { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING }
                if (remaining == 0) {
                    DownloadNotificationBridge.cancelDownloadNotification()
                }
            }
        }
    }

    private suspend fun processTask(task: DownloadTask) {
        val taskIndex = downloadTasks.indexOfFirst { it.id == task.id }
        if (taskIndex < 0) return

        // Set status to DOWNLOADING
        val activeTask = task.copy(status = DownloadStatus.DOWNLOADING, progress = 0f)
        downloadTasks[taskIndex] = activeTask

        val remainingInitial = downloadTasks.count { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING }
        DownloadNotificationBridge.updateDownloadNotification(
            task = activeTask,
            progress = 0f,
            currentFileIndex = 0,
            totalFiles = 1,
            remainingTasksCount = remainingInitial
        )

        try {
            // Find cached manga details as fallback if needed
            val cachedManga = MangaDataCache.cachedMangaDetails.values.firstOrNull { m ->
                m.chapters.any { it.url == task.id } || m.title.equals(task.title, ignoreCase = true)
            }
            val sourceId = task.sourceId.ifBlank { cachedManga?.source ?: "" }
            val mangaTitle = task.title.ifBlank { cachedManga?.title ?: "Manga" }
            val mangaUrl = task.mangaUrl.ifBlank { cachedManga?.url ?: "" }
            val mangaCover = task.thumbnailUrl.ifBlank { cachedManga?.thumbnailUrl ?: "" }
            val chapter = cachedManga?.chapters?.firstOrNull { it.url == task.id }
            val chapterTitle = task.subtitle.ifBlank { chapter?.title ?: "Chapter" }
            val chapterNumber = if (task.chapterNumber > 0f) task.chapterNumber else (chapter?.chapterNumber ?: 1f)

            // 1. Fetch pages from source
            val pages = if (sourceId.isNotBlank()) {
                sourceManager.getPages(sourceId, task.id)
            } else emptyList()

            if (pages.isEmpty()) {
                val currentIdx = downloadTasks.indexOfFirst { it.id == task.id }
                if (currentIdx >= 0 && downloadTasks[currentIdx].status == DownloadStatus.DOWNLOADING) {
                    downloadTasks[currentIdx] = downloadTasks[currentIdx].copy(status = DownloadStatus.ERROR)
                    DownloadNotificationBridge.notifyDownloadError(task, "No pages found")
                }
                return
            }

            // 2. Setup directory
            val rootDir = engine.getDownloadsDirectory()
            val safeSource = sanitizeFileName(sourceId.ifBlank { "source" })
            val safeManga = sanitizeFileName(mangaTitle)
            val safeChapter = sanitizeFileName(chapterTitle)
            val chapterDir = "$rootDir/$safeSource/$safeManga/$safeChapter"

            val pageFilePaths = mutableListOf<String>()
            val totalPages = pages.size

            val sourceBaseUrl = if (sourceId.isNotBlank()) sourceManager.getSourceBaseUrl(sourceId) else ""
            val downloadHeaders = buildMap {
                if (sourceBaseUrl.isNotBlank()) {
                    put("Referer", sourceBaseUrl)
                    put("Origin", sourceBaseUrl)
                }
            }

            // 3. Download each page
            for (index in 0 until totalPages) {
                // Check if user paused or cancelled
                val currentTaskState = downloadTasks.firstOrNull { it.id == task.id }
                if (currentTaskState == null || currentTaskState.status == DownloadStatus.PAUSED) {
                    return
                }

                val page = pages[index]
                val pageIndexStr = (index + 1).toString().padStart(3, '0')
                val targetFile = "$chapterDir/page_$pageIndexStr.jpg"

                val success = if (engine.fileExists(targetFile)) {
                    true
                } else {
                    engine.downloadPageToFile(page.url, targetFile, downloadHeaders)
                }

                if (success) {
                    pageFilePaths.add(targetFile)
                }

                // Update progress
                val currentIdx = downloadTasks.indexOfFirst { it.id == task.id }
                if (currentIdx >= 0 && downloadTasks[currentIdx].status == DownloadStatus.DOWNLOADING) {
                    val progressVal = (index + 1).toFloat() / totalPages.toFloat()
                    val updatedTask = downloadTasks[currentIdx].copy(progress = progressVal)
                    downloadTasks[currentIdx] = updatedTask

                    val remaining = downloadTasks.count { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING }
                    DownloadNotificationBridge.updateDownloadNotification(
                        task = updatedTask,
                        progress = progressVal,
                        currentFileIndex = index + 1,
                        totalFiles = totalPages,
                        remainingTasksCount = remaining
                    )
                }
            }

            // 4. Mark Completed & Register
            if (pageFilePaths.size >= (totalPages * 0.75).toInt().coerceAtLeast(1)) {
                val totalSize = engine.getDirectorySize(chapterDir)
                val info = DownloadedChapterInfo(
                    mangaTitle = mangaTitle,
                    mangaUrl = mangaUrl,
                    mangaCover = mangaCover,
                    sourceId = sourceId,
                    chapterTitle = chapterTitle,
                    chapterUrl = task.id,
                    chapterNumber = chapterNumber,
                    pageCount = pageFilePaths.size,
                    localDirPath = chapterDir,
                    pageFilePaths = pageFilePaths,
                    downloadTimestamp = currentTimeMillis(),
                    sizeBytes = totalSize
                )
                downloadedChapters[task.id] = info
                saveDownloadedChapters()

                val currentIdx = downloadTasks.indexOfFirst { it.id == task.id }
                if (currentIdx >= 0) {
                    val completedTask = downloadTasks[currentIdx].copy(
                        status = DownloadStatus.COMPLETED,
                        progress = 1f
                    )
                    downloadTasks[currentIdx] = completedTask
                    DownloadNotificationBridge.notifyDownloadCompleted(completedTask)
                }
            } else {
                val currentIdx = downloadTasks.indexOfFirst { it.id == task.id }
                if (currentIdx >= 0) {
                    val errorTask = downloadTasks[currentIdx].copy(status = DownloadStatus.ERROR)
                    downloadTasks[currentIdx] = errorTask
                    DownloadNotificationBridge.notifyDownloadError(errorTask, "Incomplete download")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val currentIdx = downloadTasks.indexOfFirst { it.id == task.id }
            if (currentIdx >= 0) {
                val errorTask = downloadTasks[currentIdx].copy(status = DownloadStatus.ERROR)
                downloadTasks[currentIdx] = errorTask
                DownloadNotificationBridge.notifyDownloadError(errorTask, e.message)
            }
        }
    }
}
