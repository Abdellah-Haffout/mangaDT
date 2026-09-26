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
    val syncSettings: Boolean = true,
    val syncDownloads: Boolean = false
)

data class SyncResult(
    val success: Boolean,
    val message: String,
    val libraryCount: Int = 0,
    val historyCount: Int = 0,
    val statsCount: Int = 0,
    val downloadsCount: Int = 0
)

data class DownloadedChapterSyncMeta(
    val mangaTitle: String,
    val mangaUrl: String,
    val mangaCover: String,
    val sourceId: String,
    val chapterTitle: String,
    val chapterUrl: String,
    val chapterNumber: Float,
    val pageCount: Int,
    val sizeBytes: Long
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

object SimpleBase64 {
    private const val CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    private val INV = IntArray(256) { -1 }.apply {
        for (i in CHARS.indices) this[CHARS[i].code] = i
        this['='.code] = 0
    }

    fun encode(bytes: ByteArray): String {
        val sb = StringBuilder((bytes.size * 4 + 2) / 3)
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i++].toInt() and 0xFF
            val b1 = if (i < bytes.size) bytes[i++].toInt() and 0xFF else -1
            val b2 = if (i < bytes.size) bytes[i++].toInt() and 0xFF else -1

            val c0 = b0 ushr 2
            val c1 = ((b0 and 0x03) shl 4) or (if (b1 >= 0) b1 ushr 4 else 0)
            val c2 = if (b1 >= 0) ((b1 and 0x0F) shl 2) or (if (b2 >= 0) b2 ushr 6 else 0) else 64
            val c3 = if (b2 >= 0) b2 and 0x3F else 64

            sb.append(CHARS[c0])
            sb.append(CHARS[c1])
            sb.append(if (c2 == 64) '=' else CHARS[c2])
            sb.append(if (c3 == 64) '=' else CHARS[c3])
        }
        return sb.toString()
    }

    fun decode(str: String): ByteArray {
        val clean = str.filter { it in CHARS || it == '=' }
        if (clean.isEmpty()) return ByteArray(0)
        val pad = clean.takeLastWhile { it == '=' }.length
        val len = (clean.length * 3) / 4 - pad
        val result = ByteArray(len.coerceAtLeast(0))
        var outIdx = 0
        var i = 0
        while (i < clean.length) {
            val c0 = INV[clean[i++].code]
            val c1 = if (i < clean.length) INV[clean[i++].code] else 0
            val c2 = if (i < clean.length) INV[clean[i++].code] else 0
            val c3 = if (i < clean.length) INV[clean[i++].code] else 0

            if (outIdx < len) result[outIdx++] = ((c0 shl 2) or (c1 ushr 4)).toByte()
            if (outIdx < len) result[outIdx++] = (((c1 and 0x0F) shl 4) or (c2 ushr 2)).toByte()
            if (outIdx < len) result[outIdx++] = (((c2 and 0x03) shl 6) or c3).toByte()
        }
        return result
    }
}

/**
 * Ultra-fast, single-pass streaming JSON reader optimized for large dataset sync.
 */
class FastJsonScanner(private val json: String) {
    private var pos = 0
    private val len = json.length

    private fun skipWhitespace() {
        while (pos < len && json[pos].isWhitespace()) pos++
    }

    fun findKey(targetKey: String): Boolean {
        val search = "\"$targetKey\""
        val idx = json.indexOf(search, pos)
        if (idx == -1) return false
        pos = idx + search.length
        skipWhitespace()
        if (pos < len && json[pos] == ':') {
            pos++
            skipWhitespace()
            return true
        }
        return false
    }

    fun parseStringArray(): List<String> {
        skipWhitespace()
        if (pos >= len || json[pos] != '[') return emptyList()
        pos++ // skip '['
        val list = mutableListOf<String>()
        while (pos < len) {
            skipWhitespace()
            if (pos < len && json[pos] == ']') {
                pos++
                break
            }
            if (pos < len && json[pos] == ',') {
                pos++
                continue
            }
            if (pos < len && json[pos] == '"') {
                list.add(readString())
            } else {
                while (pos < len && json[pos] != ',' && json[pos] != ']') pos++
            }
        }
        return list
    }

    fun readString(): String {
        if (pos >= len || json[pos] != '"') return ""
        pos++ // skip opening quote
        val sb = StringBuilder()
        var escape = false
        while (pos < len) {
            val c = json[pos++]
            if (escape) {
                when (c) {
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    '\\' -> sb.append('\\')
                    '"' -> sb.append('"')
                    '/' -> sb.append('/')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    'u' -> {
                        if (pos + 4 <= len) {
                            val hex = json.substring(pos, pos + 4)
                            pos += 4
                            val code = hex.toIntOrNull(16) ?: 0
                            sb.append(code.toChar())
                        }
                    }
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

    fun parseObject(): Map<String, Any?> {
        skipWhitespace()
        if (pos >= len || json[pos] != '{') return emptyMap()
        pos++ // skip '{'
        val map = HashMap<String, Any?>()
        while (pos < len) {
            skipWhitespace()
            if (pos < len && json[pos] == '}') {
                pos++
                break
            }
            if (pos < len && json[pos] == ',') {
                pos++
                continue
            }
            if (pos < len && json[pos] == '"') {
                val key = readString()
                skipWhitespace()
                if (pos < len && json[pos] == ':') {
                    pos++
                    skipWhitespace()
                    val value = readValue()
                    map[key] = value
                }
            } else {
                pos++
            }
        }
        return map
    }

    private fun readValue(): Any? {
        skipWhitespace()
        if (pos >= len) return null
        return when (val c = json[pos]) {
            '"' -> readString()
            '[' -> parseArray()
            '{' -> parseObject()
            't', 'f' -> {
                if (json.startsWith("true", pos)) { pos += 4; true }
                else if (json.startsWith("false", pos)) { pos += 5; false }
                else { pos++; null }
            }
            'n' -> {
                if (json.startsWith("null", pos)) { pos += 4; null }
                else { pos++; null }
            }
            else -> {
                if (c.isDigit() || c == '-') {
                    val start = pos
                    while (pos < len && (json[pos].isDigit() || json[pos] == '.' || json[pos] == '-' || json[pos] == 'e' || json[pos] == 'E')) {
                        pos++
                    }
                    val numStr = json.substring(start, pos)
                    if (numStr.contains('.')) numStr.toDoubleOrNull()
                    else numStr.toLongOrNull()
                } else {
                    pos++
                    null
                }
            }
        }
    }

    fun parseArray(): List<Any?> {
        skipWhitespace()
        if (pos >= len || json[pos] != '[') return emptyList()
        pos++ // skip '['
        val list = mutableListOf<Any?>()
        while (pos < len) {
            skipWhitespace()
            if (pos < len && json[pos] == ']') {
                pos++
                break
            }
            if (pos < len && json[pos] == ',') {
                pos++
                continue
            }
            val value = readValue()
            list.add(value)
        }
        return list
    }
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
        return try {
            when (action) {
                "PING" -> {
                    "PONG|||$deviceName|||$serverPort"
                }
                "SYNC_PAYLOAD" -> {
                    mergeIncomingJson(body, syncOptions)
                    createLocalPayloadJson(syncOptions)
                }
                "GET_CHAPTER_ZIP" -> {
                    val chapterUrl = body.trim()
                    val result = DownloadManager.packageChapterToZip(chapterUrl)
                    if (result != null) {
                        val (info, zipBytes) = result
                        val b64 = SimpleBase64.encode(zipBytes)
                        "OK|||${escapeJson(info.mangaTitle)}|||${escapeJson(info.mangaUrl)}|||${escapeJson(info.mangaCover)}|||${escapeJson(info.sourceId)}|||${escapeJson(info.chapterTitle)}|||${escapeJson(info.chapterUrl)}|||${info.chapterNumber}|||${info.pageCount}|||$b64"
                    } else {
                        "ERROR|||Chapter not found or not downloaded"
                    }
                }
                "PUSH_CHAPTER_ZIP" -> {
                    val parts = body.split("|||")
                    if (parts.size >= 10 && parts[0] == "PUSH") {
                        val mangaTitle = parts[1]
                        val mangaUrl = parts[2]
                        val mangaCover = parts[3]
                        val sourceId = parts[4]
                        val chapterTitle = parts[5]
                        val chapterUrl = parts[6]
                        val chapterNumber = parts[7].toFloatOrNull() ?: 0f
                        val pageCount = parts[8].toIntOrNull() ?: 0
                        val zipBytes = SimpleBase64.decode(parts[9])

                        if (zipBytes.isNotEmpty()) {
                            DownloadManager.unpackChapterZip(
                                mangaTitle = mangaTitle,
                                mangaUrl = mangaUrl,
                                mangaCover = mangaCover,
                                sourceId = sourceId,
                                chapterTitle = chapterTitle,
                                chapterUrl = chapterUrl,
                                chapterNumber = chapterNumber,
                                pageCount = pageCount,
                                zipBytes = zipBytes
                            )
                            "OK"
                        } else "ERROR|||Empty zip"
                    } else "ERROR|||Invalid format"
                }
                else -> "OK"
            }
        } catch (e: Exception) {
            "ERROR|||${e.message}"
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
        val strings = if (AppSettings.appLanguage == AppLanguage.ARABIC) ArabicStrings else EnglishStrings

        return try {
            val localPayload = createLocalPayloadJson(syncOptions)
            val response = engine.sendRequest(targetIp, targetPort, "SYNC_PAYLOAD", localPayload)

            if (response != null && response.isNotBlank()) {
                val (result, remoteDownloads) = parseAndMergeJson(response, syncOptions)
                var transferredDownloadsCount = 0

                // 2. Transfer Downloaded Chapters if enabled
                if (syncOptions.syncDownloads) {
                    val localMissingChapters = remoteDownloads.filter { !DownloadManager.isChapterDownloaded(it.chapterUrl) }
                    val totalToDownload = localMissingChapters.size

                    // Pull missing chapters from target
                    localMissingChapters.forEachIndexed { idx, chMeta ->
                        lastSyncStatus = strings.syncingDownloadsProgress(idx + 1, totalToDownload, chMeta.chapterTitle)
                        val chResp = engine.sendRequest(targetIp, targetPort, "GET_CHAPTER_ZIP", chMeta.chapterUrl)
                        if (chResp != null && chResp.startsWith("OK|||")) {
                            val parts = chResp.split("|||")
                            if (parts.size >= 10) {
                                val zipBytes = SimpleBase64.decode(parts[9])
                                if (zipBytes.isNotEmpty()) {
                                    val unpacked = DownloadManager.unpackChapterZip(
                                        mangaTitle = chMeta.mangaTitle,
                                        mangaUrl = chMeta.mangaUrl,
                                        mangaCover = chMeta.mangaCover,
                                        sourceId = chMeta.sourceId,
                                        chapterTitle = chMeta.chapterTitle,
                                        chapterUrl = chMeta.chapterUrl,
                                        chapterNumber = chMeta.chapterNumber,
                                        pageCount = chMeta.pageCount,
                                        zipBytes = zipBytes
                                    )
                                    if (unpacked != null) {
                                        transferredDownloadsCount++
                                    }
                                }
                            }
                        }
                    }

                    // Push missing chapters from local to target
                    val remoteUrls = remoteDownloads.map { it.chapterUrl }.toSet()
                    val localToPush = DownloadManager.downloadedChapters.values.filter { it.chapterUrl !in remoteUrls }
                    localToPush.forEach { localCh ->
                        val packaged = DownloadManager.packageChapterToZip(localCh.chapterUrl)
                        if (packaged != null) {
                            val (info, zipBytes) = packaged
                            val b64 = SimpleBase64.encode(zipBytes)
                            val pushBody = "PUSH|||${escapeJson(info.mangaTitle)}|||${escapeJson(info.mangaUrl)}|||${escapeJson(info.mangaCover)}|||${escapeJson(info.sourceId)}|||${escapeJson(info.chapterTitle)}|||${escapeJson(info.chapterUrl)}|||${info.chapterNumber}|||${info.pageCount}|||$b64"
                            engine.sendRequest(targetIp, targetPort, "PUSH_CHAPTER_ZIP", pushBody)
                        }
                    }
                }

                val totalLib = LibraryManager.libraryItems.size
                val totalHist = HistoryManager.historyEntries.size
                val totalStats = StatisticsManager.mangaStatsMap.size

                val isAr = AppSettings.appLanguage == AppLanguage.ARABIC
                val downloadsSummary = if (transferredDownloadsCount > 0) {
                    if (isAr) "، ونقل $transferredDownloadsCount فصلاً محمل" else ", and transferred $transferredDownloadsCount downloaded chapters"
                } else ""

                val summary = if (isAr) {
                    "✓ تمت المزامنة: دمج $totalLib مانجا بالمكتبة، $totalHist سجل قراءة، $totalStats إحصائيات$downloadsSummary"
                } else {
                    "✓ Synced: $totalLib library manga, $totalHist history records, $totalStats reading stats$downloadsSummary"
                }

                lastSyncStatus = summary
                val nowTime = currentTimeMillis().toString()
                lastSyncTimestamp = nowTime
                SettingsStorage.setString("last_sync_status", summary)
                SettingsStorage.setString("last_sync_time", nowTime)
                result.copy(success = true, message = summary, downloadsCount = transferredDownloadsCount)
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
        val sb = StringBuilder(64 * 1024)
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

        // 4. Downloaded Chapters Metadata (if enabled)
        if (options.syncDownloads) {
            val downloads = DownloadManager.downloadedChapters.values.toList()
            sb.append("  \"downloads\": [\n")
            downloads.forEachIndexed { i, d ->
                sb.append("    {")
                sb.append("\"mangaTitle\":\"").append(escapeJson(d.mangaTitle)).append("\",")
                sb.append("\"mangaUrl\":\"").append(escapeJson(d.mangaUrl)).append("\",")
                sb.append("\"mangaCover\":\"").append(escapeJson(d.mangaCover)).append("\",")
                sb.append("\"sourceId\":\"").append(escapeJson(d.sourceId)).append("\",")
                sb.append("\"chapterTitle\":\"").append(escapeJson(d.chapterTitle)).append("\",")
                sb.append("\"chapterUrl\":\"").append(escapeJson(d.chapterUrl)).append("\",")
                sb.append("\"chapterNumber\":").append(d.chapterNumber).append(",")
                sb.append("\"pageCount\":").append(d.pageCount).append(",")
                sb.append("\"sizeBytes\":").append(d.sizeBytes)
                sb.append("}").append(if (i < downloads.size - 1) ",\n" else "\n")
            }
            sb.append("  ],\n")
        }

        // 5. User Profile & Settings
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

    fun mergeIncomingJson(json: String, options: SyncOptions): SyncResult {
        return parseAndMergeJson(json, options).first
    }

    private fun parseAndMergeJson(json: String, options: SyncOptions): Pair<SyncResult, List<DownloadedChapterSyncMeta>> {
        var mergedLibraryCount = 0
        var mergedHistoryCount = 0
        var mergedStatsCount = 0
        val downloadedMetaList = mutableListOf<DownloadedChapterSyncMeta>()

        try {
            val scanner = FastJsonScanner(json)
            val root = scanner.parseObject()

            // 1. Categories & Library
            if (options.syncLibrary) {
                @Suppress("UNCHECKED_CAST")
                val incomingCats = (root["categories"] as? List<String>) ?: emptyList()

                @Suppress("UNCHECKED_CAST")
                val libRaw = (root["library"] as? List<Map<String, Any?>>) ?: (root["favourites"] as? List<Map<String, Any?>>) ?: (root["favorites"] as? List<Map<String, Any?>>)
                if (!libRaw.isNullOrEmpty()) {
                    val items = libRaw.mapNotNull { m ->
                        val title = m["title"] as? String ?: return@mapNotNull null
                        val mangaUrl = m["mangaUrl"] as? String ?: ""
                        val id = m["id"] as? String ?: title
                        val thumb = m["thumbnailUrl"] as? String ?: ""
                        val sourceId = m["sourceId"] as? String ?: ""
                        val cat = (m["category"] as? String)?.ifBlank { "Reading" } ?: "Reading"
                        val added = (m["addedAt"] as? Number)?.toLong() ?: currentTimeMillis()
                        val lastRead = (m["lastReadAt"] as? Number)?.toLong() ?: 0L
                        val lastReadChTitle = m["lastReadChapterTitle"] as? String ?: ""
                        val lastReadChUrl = m["lastReadChapterUrl"] as? String ?: ""
                        val totalCh = (m["totalChapters"] as? Number)?.toInt() ?: 0
                        val unread = (m["unreadCount"] as? Number)?.toInt() ?: 0
                        val author = (m["author"] as? String)?.ifBlank { null }
                        val statusStr = m["status"] as? String ?: ""
                        val status = runCatching { MangaStatus.valueOf(statusStr) }.getOrDefault(MangaStatus.UNKNOWN)

                        LibraryManga(
                            id = id,
                            title = title,
                            thumbnailUrl = thumb,
                            sourceId = sourceId,
                            mangaUrl = mangaUrl,
                            category = cat,
                            addedAt = added,
                            lastReadAt = lastRead,
                            lastReadChapterTitle = lastReadChTitle,
                            lastReadChapterUrl = lastReadChUrl,
                            totalChapters = totalCh,
                            unreadCount = unread,
                            author = author,
                            status = status
                        )
                    }
                    mergedLibraryCount = LibraryManager.batchMergeLibrary(items, incomingCats)
                } else if (incomingCats.isNotEmpty()) {
                    LibraryManager.batchMergeLibrary(emptyList(), incomingCats)
                }
            }

            // 2. History
            if (options.syncHistory) {
                @Suppress("UNCHECKED_CAST")
                val histRaw = (root["history"] as? List<Map<String, Any?>>)
                if (!histRaw.isNullOrEmpty()) {
                    val entries = histRaw.mapNotNull { m ->
                        val title = m["mangaTitle"] as? String ?: return@mapNotNull null
                        val chUrl = m["chapterUrl"] as? String ?: return@mapNotNull null
                        val cover = m["mangaCover"] as? String ?: ""
                        val sourceId = m["sourceId"] as? String ?: ""
                        val chTitle = m["chapterTitle"] as? String ?: ""
                        val mangaUrl = m["mangaUrl"] as? String ?: ""
                        val ts = (m["timestamp"] as? Number)?.toLong() ?: currentTimeMillis()
                        val page = (m["lastPage"] as? Number)?.toInt() ?: 1
                        val totalPages = (m["totalPages"] as? Number)?.toInt() ?: 1
                        val offset = (m["scrollOffset"] as? Number)?.toInt() ?: 0

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
                    }
                    mergedHistoryCount = HistoryManager.batchMergeHistory(entries)
                }
            }

            // 3. Statistics
            if (options.syncStats) {
                @Suppress("UNCHECKED_CAST")
                val statsRaw = (root["stats"] as? List<Map<String, Any?>>)
                if (!statsRaw.isNullOrEmpty()) {
                    val statsList = statsRaw.mapNotNull { m ->
                        val key = m["mangaKey"] as? String ?: ""
                        val title = m["mangaTitle"] as? String ?: ""
                        val cover = m["mangaCover"] as? String ?: ""
                        val sourceId = m["sourceId"] as? String ?: ""
                        val mangaUrl = m["mangaUrl"] as? String ?: ""
                        val time = (m["totalTimeSeconds"] as? Number)?.toLong() ?: 0L
                        val pages = (m["totalPagesRead"] as? Number)?.toInt() ?: 0
                        val lastReadTs = (m["lastReadTimestamp"] as? Number)?.toLong() ?: 0L
                        val sessions = (m["sessionCount"] as? Number)?.toInt() ?: 1

                        @Suppress("UNCHECKED_CAST")
                        val chList = (m["chaptersRead"] as? List<String>)?.toSet() ?: emptySet()
                        @Suppress("UNCHECKED_CAST")
                        val tagsList = (m["tags"] as? List<String>) ?: emptyList()

                        MangaReadingStats(
                            mangaKey = key,
                            mangaTitle = title,
                            mangaCover = cover,
                            sourceId = sourceId,
                            mangaUrl = mangaUrl,
                            totalTimeSeconds = time,
                            chaptersRead = chList,
                            totalPagesRead = pages,
                            lastReadTimestamp = lastReadTs,
                            sessionCount = sessions,
                            tags = tagsList
                        )
                    }
                    mergedStatsCount = StatisticsManager.batchMergeStats(statsList)
                }
            }

            // 4. Downloaded Chapters Metadata
            @Suppress("UNCHECKED_CAST")
            val downloadsRaw = (root["downloads"] as? List<Map<String, Any?>>)
            if (!downloadsRaw.isNullOrEmpty()) {
                downloadsRaw.forEach { d ->
                    val title = d["mangaTitle"] as? String ?: ""
                    val mUrl = d["mangaUrl"] as? String ?: ""
                    val cover = d["mangaCover"] as? String ?: ""
                    val src = d["sourceId"] as? String ?: ""
                    val chTitle = d["chapterTitle"] as? String ?: ""
                    val chUrl = d["chapterUrl"] as? String ?: ""
                    val chNum = (d["chapterNumber"] as? Number)?.toFloat() ?: 0f
                    val pageCnt = (d["pageCount"] as? Number)?.toInt() ?: 0
                    val size = (d["sizeBytes"] as? Number)?.toLong() ?: 0L

                    if (title.isNotBlank() && chUrl.isNotBlank()) {
                        downloadedMetaList.add(
                            DownloadedChapterSyncMeta(
                                mangaTitle = title,
                                mangaUrl = mUrl,
                                mangaCover = cover,
                                sourceId = src,
                                chapterTitle = chTitle,
                                chapterUrl = chUrl,
                                chapterNumber = chNum,
                                pageCount = pageCnt,
                                sizeBytes = size
                            )
                        )
                    }
                }
            }

            // 5. Settings & Profile
            if (options.syncSettings) {
                @Suppress("UNCHECKED_CAST")
                val pinned = (root["pinnedSources"] as? List<String>)
                pinned?.forEach { AppSettings.togglePinnedSource(it) }

                @Suppress("UNCHECKED_CAST")
                val profileMap = root["profile"] as? Map<String, Any?>
                if (profileMap != null) {
                    val username = profileMap["username"] as? String ?: ""
                    val bio = profileMap["bio"] as? String ?: ""
                    val avatar = (profileMap["avatarId"] as? String)?.ifBlank { "avatar_1" } ?: "avatar_1"
                    if (username.isNotBlank()) {
                        StatisticsManager.updateUserProfile(username, bio, avatar)
                    }
                }
            }

            LibraryManager.reload()
            HistoryManager.reload()
            StatisticsManager.reload()
            AppSettings.reload()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val result = SyncResult(
            success = true,
            message = "Sync Completed",
            libraryCount = mergedLibraryCount,
            historyCount = mergedHistoryCount,
            statsCount = mergedStatsCount
        )
        return Pair(result, downloadedMetaList)
    }
}
