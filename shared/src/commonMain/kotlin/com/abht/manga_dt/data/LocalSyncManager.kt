package com.abht.manga_dt.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.abht.manga_dt.models.HistoryEntry
import com.abht.manga_dt.models.LibraryManga
import com.abht.manga_dt.models.MangaReadingStats
import com.abht.manga_dt.models.MangaStatus
import com.abht.manga_dt.models.UserProfileData
import kotlinx.coroutines.*

data class DiscoveredPeer(
    val ip: String,
    val port: Int = 45678,
    val deviceName: String,
    val isAndroid: Boolean = false
)

data class SyncOptions(
    val syncLibrary: Boolean = true,
    val syncHistory: Boolean = true,
    val syncStats: Boolean = true,
    val syncSettings: Boolean = true
)

data class SyncResult(
    val success: Boolean,
    val message: String,
    val libraryCount: Int = 0,
    val historyCount: Int = 0,
    val statsCount: Int = 0
)

expect class LocalNetworkEngine() {
    fun getLocalIpAddress(): String
    fun getDeviceName(): String
    fun startSyncServer(port: Int, onMessageReceived: (action: String, body: String) -> String): Boolean
    fun stopSyncServer()
    fun isServerRunning(): Boolean
    suspend fun sendRequest(targetIp: String, port: Int, action: String, payload: String): String?
    suspend fun scanLocalPeers(port: Int, timeoutMs: Long = 1500L): List<DiscoveredPeer>
}

object LocalSyncManager {
    private val engine = LocalNetworkEngine()
    private val syncScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    var isServerEnabled by mutableStateOf(false)
        private set

    var localIpAddress by mutableStateOf(engine.getLocalIpAddress())
        private set

    var deviceName by mutableStateOf(
        SettingsStorage.getString("sync_device_name", engine.getDeviceName())
    )
        private set

    var serverPort by mutableStateOf(45678)
        private set

    var discoveredPeers by mutableStateOf<List<DiscoveredPeer>>(emptyList())
        private set

    var isScanning by mutableStateOf(false)
        private set

    var isSyncing by mutableStateOf(false)
        private set

    var lastSyncStatus by mutableStateOf<String?>(
        SettingsStorage.getString("last_sync_status", "")
    )
        private set

    var lastSyncTimestamp by mutableStateOf(
        SettingsStorage.getString("last_sync_time", "")
    )
        private set

    var syncOptions by mutableStateOf(SyncOptions())

    fun updateDeviceName(name: String) {
        val trimmed = name.trim().ifBlank { engine.getDeviceName() }
        deviceName = trimmed
        SettingsStorage.setString("sync_device_name", trimmed)
    }

    fun refreshLocalIp() {
        localIpAddress = engine.getLocalIpAddress()
    }

    fun toggleServer(enable: Boolean): Boolean {
        return if (enable) {
            refreshLocalIp()
            val started = engine.startSyncServer(serverPort) { action, body ->
                handleIncomingRequest(action, body)
            }
            isServerEnabled = started
            started
        } else {
            engine.stopSyncServer()
            isServerEnabled = false
            true
        }
    }

    private fun handleIncomingRequest(action: String, body: String): String {
        return when (action) {
            "PING" -> {
                "PONG|||$deviceName|||$serverPort"
            }
            "SYNC_PAYLOAD" -> {
                mergeIncomingJson(body, syncOptions)
                createLocalPayloadJson(syncOptions)
            }
            else -> "OK"
        }
    }

    suspend fun scanForPeers() {
        if (isScanning) return
        isScanning = true
        refreshLocalIp()
        try {
            val peers = engine.scanLocalPeers(serverPort)
            discoveredPeers = peers.filterNot { it.ip == localIpAddress }
        } catch (_: Exception) {
        } finally {
            isScanning = false
        }
    }

    suspend fun syncWithTarget(targetIp: String, targetPort: Int = serverPort): SyncResult {
        if (isSyncing) return SyncResult(false, "Sync already in progress")
        isSyncing = true
        refreshLocalIp()

        return try {
            val localPayload = createLocalPayloadJson(syncOptions)
            val response = engine.sendRequest(targetIp, targetPort, "SYNC_PAYLOAD", localPayload)

            if (response != null && response.isNotBlank()) {
                val result = mergeIncomingJson(response, syncOptions)
                val totalLib = LibraryManager.libraryItems.size
                val totalHist = HistoryManager.historyEntries.size
                val totalStats = StatisticsManager.mangaStatsMap.size

                val isAr = AppSettings.appLanguage == AppLanguage.ARABIC
                val summary = if (isAr) {
                    "✓ تمت المزامنة: دمج $totalLib مانجا بالمكتبة، $totalHist سجل قراءة، $totalStats إحصائيات"
                } else {
                    "✓ Synced: $totalLib library manga, $totalHist history records, $totalStats reading stats"
                }

                lastSyncStatus = summary
                val nowTime = currentTimeMillis().toString()
                lastSyncTimestamp = nowTime
                SettingsStorage.setString("last_sync_status", summary)
                SettingsStorage.setString("last_sync_time", nowTime)
                result.copy(success = true, message = summary)
            } else {
                val errorMsg = "Could not connect to $targetIp:$targetPort. Ensure sync server is running on target device."
                lastSyncStatus = errorMsg
                SyncResult(false, errorMsg)
            }
        } catch (e: Exception) {
            val errorMsg = "Sync error: ${e.message ?: "Connection failed"}"
            lastSyncStatus = errorMsg
            SyncResult(false, errorMsg)
        } finally {
            isSyncing = false
        }
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    fun createLocalPayloadJson(options: SyncOptions): String {
        val sb = java.lang.StringBuilder()
        sb.append("{\n")
        sb.append("  \"deviceName\": \"").append(escapeJson(deviceName)).append("\",\n")
        sb.append("  \"timestamp\": ").append(currentTimeMillis()).append(",\n")

        // 1. Library Items
        if (options.syncLibrary) {
            val lib = LibraryManager.libraryItems
            val cats = LibraryManager.categories
            sb.append("  \"categories\": [").append(cats.joinToString(",") { "\"${escapeJson(it)}\"" }).append("],\n")
            sb.append("  \"library\": [\n")
            lib.forEachIndexed { i, m ->
                sb.append("    {")
                sb.append("\"id\":\"").append(escapeJson(m.id)).append("\",")
                sb.append("\"title\":\"").append(escapeJson(m.title)).append("\",")
                sb.append("\"thumbnailUrl\":\"").append(escapeJson(m.thumbnailUrl)).append("\",")
                sb.append("\"sourceId\":\"").append(escapeJson(m.sourceId)).append("\",")
                sb.append("\"mangaUrl\":\"").append(escapeJson(m.mangaUrl)).append("\",")
                sb.append("\"category\":\"").append(escapeJson(m.category)).append("\",")
                sb.append("\"addedAt\":").append(m.addedAt).append(",")
                sb.append("\"lastReadAt\":").append(m.lastReadAt).append(",")
                sb.append("\"lastReadChapterTitle\":\"").append(escapeJson(m.lastReadChapterTitle)).append("\",")
                sb.append("\"lastReadChapterUrl\":\"").append(escapeJson(m.lastReadChapterUrl)).append("\",")
                sb.append("\"totalChapters\":").append(m.totalChapters).append(",")
                sb.append("\"unreadCount\":").append(m.unreadCount).append(",")
                sb.append("\"author\":\"").append(escapeJson(m.author ?: "")).append("\",")
                sb.append("\"status\":\"").append(m.status.name).append("\"")
                sb.append("}").append(if (i < lib.size - 1) ",\n" else "\n")
            }
            sb.append("  ],\n")
        }

        // 2. History Entries
        if (options.syncHistory) {
            val hist = HistoryManager.historyEntries
            sb.append("  \"history\": [\n")
            hist.forEachIndexed { i, h ->
                sb.append("    {")
                sb.append("\"mangaTitle\":\"").append(escapeJson(h.mangaTitle)).append("\",")
                sb.append("\"mangaCover\":\"").append(escapeJson(h.mangaCover)).append("\",")
                sb.append("\"sourceId\":\"").append(escapeJson(h.sourceId)).append("\",")
                sb.append("\"chapterTitle\":\"").append(escapeJson(h.chapterTitle)).append("\",")
                sb.append("\"chapterUrl\":\"").append(escapeJson(h.chapterUrl)).append("\",")
                sb.append("\"mangaUrl\":\"").append(escapeJson(h.mangaUrl)).append("\",")
                sb.append("\"timestamp\":").append(h.timestamp).append(",")
                sb.append("\"lastPage\":").append(h.lastPage).append(",")
                sb.append("\"totalPages\":").append(h.totalPages).append(",")
                sb.append("\"scrollOffset\":").append(h.scrollOffset)
                sb.append("}").append(if (i < hist.size - 1) ",\n" else "\n")
            }
            sb.append("  ],\n")
        }

        // 3. Statistics
        if (options.syncStats) {
            val stats = StatisticsManager.mangaStatsMap.values.toList()
            sb.append("  \"stats\": [\n")
            stats.forEachIndexed { i, s ->
                sb.append("    {")
                sb.append("\"mangaKey\":\"").append(escapeJson(s.mangaKey)).append("\",")
                sb.append("\"mangaTitle\":\"").append(escapeJson(s.mangaTitle)).append("\",")
                sb.append("\"mangaCover\":\"").append(escapeJson(s.mangaCover)).append("\",")
                sb.append("\"sourceId\":\"").append(escapeJson(s.sourceId)).append("\",")
                sb.append("\"mangaUrl\":\"").append(escapeJson(s.mangaUrl)).append("\",")
                sb.append("\"totalTimeSeconds\":").append(s.totalTimeSeconds).append(",")
                sb.append("\"chaptersRead\":[").append(s.chaptersRead.joinToString(",") { "\"${escapeJson(it)}\"" }).append("],")
                sb.append("\"totalPagesRead\":").append(s.totalPagesRead).append(",")
                sb.append("\"lastReadTimestamp\":").append(s.lastReadTimestamp).append(",")
                sb.append("\"sessionCount\":").append(s.sessionCount).append(",")
                sb.append("\"tags\":[").append(s.tags.joinToString(",") { "\"${escapeJson(it)}\"" }).append("]")
                sb.append("}").append(if (i < stats.size - 1) ",\n" else "\n")
            }
            sb.append("  ],\n")
        }

        // 4. User Profile & Settings
        if (options.syncSettings) {
            val profile = StatisticsManager.userProfile
            sb.append("  \"profile\": {\"username\":\"").append(escapeJson(profile.username))
                .append("\",\"bio\":\"").append(escapeJson(profile.bio))
                .append("\",\"avatarId\":\"").append(profile.avatarId).append("\"},\n")
            val pinned = AppSettings.pinnedSourceIds
            sb.append("  \"pinnedSources\": [").append(pinned.joinToString(",") { "\"${escapeJson(it)}\"" }).append("]\n")
        } else {
            sb.append("  \"sync\": true\n")
        }

        sb.append("}")
        return sb.toString()
    }

    private fun extractJsonArrayBlock(json: String, arrayKey: String): String {
        val key = "\"$arrayKey\":"
        val startIdx = json.indexOf(key)
        if (startIdx == -1) return ""
        val bracketStart = json.indexOf('[', startIdx + key.length)
        if (bracketStart == -1) return ""

        var depth = 0
        var inQuotes = false
        var escape = false
        for (i in bracketStart until json.length) {
            val c = json[i]
            if (escape) {
                escape = false
                continue
            }
            if (c == '\\') {
                escape = true
                continue
            }
            if (c == '"') {
                inQuotes = !inQuotes
                continue
            }
            if (!inQuotes) {
                if (c == '[') depth++
                else if (c == ']') {
                    depth--
                    if (depth == 0) {
                        return json.substring(bracketStart + 1, i)
                    }
                }
            }
        }
        return ""
    }

    private fun extractJsonObjects(arrayContent: String): List<String> {
        val objects = mutableListOf<String>()
        var depth = 0
        var inQuotes = false
        var escape = false
        var objStart = -1

        for (i in arrayContent.indices) {
            val c = arrayContent[i]
            if (escape) {
                escape = false
                continue
            }
            if (c == '\\') {
                escape = true
                continue
            }
            if (c == '"') {
                inQuotes = !inQuotes
                continue
            }
            if (!inQuotes) {
                if (c == '{') {
                    if (depth == 0) objStart = i
                    depth++
                } else if (c == '}') {
                    depth--
                    if (depth == 0 && objStart != -1) {
                        objects.add(arrayContent.substring(objStart, i + 1))
                        objStart = -1
                    }
                }
            }
        }
        return objects
    }

    private fun extractJsonString(json: String, key: String): String {
        val search = "\"$key\":"
        val start = json.indexOf(search)
        if (start == -1) return ""
        val quoteStart = json.indexOf('"', start + search.length)
        if (quoteStart == -1) return ""
        val sb = java.lang.StringBuilder()
        var escape = false
        for (i in (quoteStart + 1) until json.length) {
            val c = json[i]
            if (escape) {
                when (c) {
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    '\\' -> sb.append('\\')
                    '"' -> sb.append('"')
                    else -> sb.append(c)
                }
                escape = false
                continue
            }
            if (c == '\\') {
                escape = true
                continue
            }
            if (c == '"') {
                break
            }
            sb.append(c)
        }
        return sb.toString()
    }

    private fun extractJsonLong(json: String, key: String, default: Long = 0L): Long {
        val search = "\"$key\":"
        val start = json.indexOf(search)
        if (start == -1) return default
        var numStart = start + search.length
        while (numStart < json.length && json[numStart].isWhitespace()) numStart++
        var numEnd = numStart
        while (numEnd < json.length && (json[numEnd].isDigit() || json[numEnd] == '-')) numEnd++
        return json.substring(numStart, numEnd).toLongOrNull() ?: default
    }

    private fun extractJsonInt(json: String, key: String, default: Int = 0): Int {
        val search = "\"$key\":"
        val start = json.indexOf(search)
        if (start == -1) return default
        var numStart = start + search.length
        while (numStart < json.length && json[numStart].isWhitespace()) numStart++
        var numEnd = numStart
        while (numEnd < json.length && (json[numEnd].isDigit() || json[numEnd] == '-')) numEnd++
        return json.substring(numStart, numEnd).toIntOrNull() ?: default
    }

    fun mergeIncomingJson(json: String, options: SyncOptions): SyncResult {
        var mergedLibraryCount = 0
        var mergedHistoryCount = 0
        var mergedStatsCount = 0

        // 1. Merge Categories & Library
        if (options.syncLibrary) {
            val categoriesBlock = extractJsonArrayBlock(json, "categories")
            if (categoriesBlock.isNotBlank()) {
                val catMatches = "\"([^\"]+)\"".toRegex().findAll(categoriesBlock)
                catMatches.forEach { match ->
                    val catName = match.groupValues[1]
                    LibraryManager.addCategory(catName)
                }
            }

            val libraryBlock = extractJsonArrayBlock(json, "library")
            if (libraryBlock.isNotBlank()) {
                val itemObjects = extractJsonObjects(libraryBlock)
                itemObjects.forEach { itemStr ->
                    val id = extractJsonString(itemStr, "id")
                    val title = extractJsonString(itemStr, "title")
                    val thumb = extractJsonString(itemStr, "thumbnailUrl")
                    val sourceId = extractJsonString(itemStr, "sourceId")
                    val mangaUrl = extractJsonString(itemStr, "mangaUrl")
                    val category = extractJsonString(itemStr, "category").ifBlank { "Reading" }
                    val addedAt = extractJsonLong(itemStr, "addedAt", currentTimeMillis())
                    val lastReadAt = extractJsonLong(itemStr, "lastReadAt", 0L)
                    val lastReadChTitle = extractJsonString(itemStr, "lastReadChapterTitle")
                    val lastReadChUrl = extractJsonString(itemStr, "lastReadChapterUrl")
                    val totalChapters = extractJsonInt(itemStr, "totalChapters", 0)
                    val unreadCount = extractJsonInt(itemStr, "unreadCount", 0)
                    val author = extractJsonString(itemStr, "author").ifBlank { null }
                    val statusStr = extractJsonString(itemStr, "status")
                    val status = runCatching { MangaStatus.valueOf(statusStr) }.getOrDefault(MangaStatus.UNKNOWN)

                    if (title.isNotBlank()) {
                        val existing = LibraryManager.getLibraryManga(mangaUrl, title)
                        if (existing == null) {
                            LibraryManager.addToLibrary(
                                LibraryManga(
                                    id = id.ifBlank { title },
                                    title = title,
                                    thumbnailUrl = thumb,
                                    sourceId = sourceId,
                                    mangaUrl = mangaUrl,
                                    category = category,
                                    addedAt = addedAt,
                                    lastReadAt = lastReadAt,
                                    lastReadChapterTitle = lastReadChTitle,
                                    lastReadChapterUrl = lastReadChUrl,
                                    totalChapters = totalChapters,
                                    unreadCount = unreadCount,
                                    author = author,
                                    status = status
                                )
                            )
                            mergedLibraryCount++
                        } else {
                            if (lastReadAt > existing.lastReadAt && lastReadChUrl.isNotBlank()) {
                                LibraryManager.updateProgress(mangaUrl, title, lastReadChTitle, lastReadChUrl)
                                mergedLibraryCount++
                            }
                        }
                    }
                }
            }
        }

        // 2. Merge History
        if (options.syncHistory) {
            val historyBlock = extractJsonArrayBlock(json, "history")
            if (historyBlock.isNotBlank()) {
                val entryObjects = extractJsonObjects(historyBlock)
                entryObjects.forEach { entryStr ->
                    val title = extractJsonString(entryStr, "mangaTitle")
                    val cover = extractJsonString(entryStr, "mangaCover")
                    val sourceId = extractJsonString(entryStr, "sourceId")
                    val chTitle = extractJsonString(entryStr, "chapterTitle")
                    val chUrl = extractJsonString(entryStr, "chapterUrl")
                    val mangaUrl = extractJsonString(entryStr, "mangaUrl")
                    val ts = extractJsonLong(entryStr, "timestamp", currentTimeMillis())
                    val page = extractJsonInt(entryStr, "lastPage", 1)
                    val totalPages = extractJsonInt(entryStr, "totalPages", 1)
                    val offset = extractJsonInt(entryStr, "scrollOffset", 0)

                    if (title.isNotBlank() && chUrl.isNotBlank()) {
                        val existing = HistoryManager.historyEntries.firstOrNull { 
                            (mangaUrl.isNotBlank() && it.mangaUrl == mangaUrl) || it.mangaTitle.equals(title, ignoreCase = true) 
                        }
                        if (existing == null || ts > existing.timestamp) {
                            HistoryManager.addOrUpdateHistory(
                                HistoryEntry(
                                    mangaTitle = title,
                                    mangaCover = cover,
                                    sourceId = sourceId,
                                    chapterTitle = chTitle,
                                    chapterUrl = chUrl,
                                    mangaUrl = mangaUrl,
                                    timestamp = ts,
                                    lastPage = page,
                                    totalPages = totalPages,
                                    scrollOffset = offset
                                )
                            )
                            mergedHistoryCount++
                        }
                    }
                }
            }
        }

        // 3. Merge Statistics
        if (options.syncStats) {
            val statsBlock = extractJsonArrayBlock(json, "stats")
            if (statsBlock.isNotBlank()) {
                val statObjects = extractJsonObjects(statsBlock)
                statObjects.forEach { statStr ->
                    val key = extractJsonString(statStr, "mangaKey")
                    val title = extractJsonString(statStr, "mangaTitle")
                    val cover = extractJsonString(statStr, "mangaCover")
                    val sourceId = extractJsonString(statStr, "sourceId")
                    val mangaUrl = extractJsonString(statStr, "mangaUrl")
                    val time = extractJsonLong(statStr, "totalTimeSeconds", 0L)
                    val pages = extractJsonInt(statStr, "totalPagesRead", 0)

                    if (key.isNotBlank()) {
                        val local = StatisticsManager.mangaStatsMap[key]
                        if (local == null) {
                            StatisticsManager.recordReadingSession(
                                mangaTitle = title,
                                mangaCover = cover,
                                sourceId = sourceId,
                                mangaUrl = mangaUrl,
                                durationSeconds = time,
                                pagesTurned = pages
                            )
                            mergedStatsCount++
                        } else if (time > local.totalTimeSeconds) {
                            val deltaSeconds = time - local.totalTimeSeconds
                            val deltaPages = (pages - local.totalPagesRead).coerceAtLeast(0)
                            StatisticsManager.recordReadingSession(
                                mangaTitle = title,
                                mangaCover = cover,
                                sourceId = sourceId,
                                mangaUrl = mangaUrl,
                                durationSeconds = deltaSeconds,
                                pagesTurned = deltaPages
                            )
                            mergedStatsCount++
                        }
                    }
                }
            }
        }

        // 4. Merge Settings & Pinned Sources
        if (options.syncSettings) {
            val pinnedBlock = extractJsonArrayBlock(json, "pinnedSources")
            if (pinnedBlock.isNotBlank()) {
                val sources = "\"([^\"]+)\"".toRegex().findAll(pinnedBlock).map { it.groupValues[1] }.toSet()
                sources.forEach { AppSettings.togglePinnedSource(it) }
            }

            val username = extractJsonString(json, "username")
            val bio = extractJsonString(json, "bio")
            val avatar = extractJsonString(json, "avatarId").ifBlank { "avatar_1" }
            if (username.isNotBlank()) {
                StatisticsManager.updateUserProfile(username, bio, avatar)
            }
        }

        // Reload all managers
        LibraryManager.reload()
        HistoryManager.reload()
        StatisticsManager.reload()
        AppSettings.reload()

        return SyncResult(
            success = true,
            message = "Sync Completed",
            libraryCount = mergedLibraryCount,
            historyCount = mergedHistoryCount,
            statsCount = mergedStatsCount
        )
    }
}
