package com.abht.manga_dt.data

import com.abht.manga_dt.models.HistoryEntry
import com.abht.manga_dt.models.LibraryManga
import com.abht.manga_dt.models.MangaReadingStats
import com.abht.manga_dt.models.MangaStatus

enum class RestoreMode {
    MERGE,
    OVERWRITE
}

data class BackupFileInfo(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val lastModified: Long
)

data class BackupPreview(
    val deviceName: String,
    val timestamp: Long,
    val libraryCount: Int,
    val historyCount: Int,
    val statsCount: Int,
    val pinnedCount: Int,
    val hasProfile: Boolean
)

expect class FileBackupEngine() {
    fun saveBackupToFile(filename: String, content: String): Result<String>
    fun readBackupFromFile(path: String): Result<String>
    fun listLocalBackups(): List<BackupFileInfo>
    fun deleteBackupFile(path: String): Boolean
    fun getDefaultBackupDirectory(): String
}

object BackupManager {
    private val engine = FileBackupEngine()

    fun getDefaultBackupDirectory(): String = engine.getDefaultBackupDirectory()

    fun listBackups(): List<BackupFileInfo> = engine.listLocalBackups()

    fun deleteBackup(path: String): Boolean = engine.deleteBackupFile(path)

    fun readBackup(path: String): Result<String> = engine.readBackupFromFile(path)

    fun createBackup(options: SyncOptions = SyncOptions()): Result<String> {
        return try {
            val json = LocalSyncManager.createLocalPayloadJson(options)
            val timestamp = currentTimeMillis()
            val filename = "mangadt_backup_${timestamp}.json"
            engine.saveBackupToFile(filename, json)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun inspectBackup(json: String): BackupPreview? {
        val hasMangaContent = json.contains("\"library\"") || json.contains("\"history\"") || 
                              json.contains("\"favourites\"") || json.contains("\"favorites\"") || 
                              json.contains("\"categories\"") || json.contains("\"stats\"") ||
                              json.contains("\"deviceName\"")

        if (!hasMangaContent) {
            return null
        }

        val deviceName = extractString(json, "deviceName").ifBlank { "Manga DT Backup" }
        val timestamp = extractLong(json, "timestamp", currentTimeMillis())

        val libBlock = extractArray(json, "library").ifBlank { extractArray(json, "favourites") }.ifBlank { extractArray(json, "favorites") }
        val libCount = if (libBlock.isNotBlank()) extractObjects(libBlock).size else 0

        val histBlock = extractArray(json, "history")
        val histCount = if (histBlock.isNotBlank()) extractObjects(histBlock).size else 0

        val statsBlock = extractArray(json, "stats")
        val statsCount = if (statsBlock.isNotBlank()) extractObjects(statsBlock).size else 0

        val pinnedBlock = extractArray(json, "pinnedSources")
        val pinnedCount = if (pinnedBlock.isNotBlank()) "\"([^\"]+)\"".toRegex().findAll(pinnedBlock).count() else 0

        val hasProfile = json.contains("\"profile\":")

        return BackupPreview(
            deviceName = deviceName,
            timestamp = timestamp,
            libraryCount = libCount,
            historyCount = histCount,
            statsCount = statsCount,
            pinnedCount = pinnedCount,
            hasProfile = hasProfile
        )
    }

    fun restoreBackup(json: String, mode: RestoreMode, options: SyncOptions = SyncOptions()): SyncResult {
        if (mode == RestoreMode.MERGE) {
            return LocalSyncManager.mergeIncomingJson(json, options)
        }

        // FULL OVERWRITE MODE
        var restoredLib = 0
        var restoredHist = 0
        var restoredStats = 0

        // 1. Library Overwrite
        if (options.syncLibrary) {
            val categoriesBlock = extractArray(json, "categories")
            if (categoriesBlock.isNotBlank()) {
                val catNames = "\"([^\"]+)\"".toRegex().findAll(categoriesBlock).map { it.groupValues[1] }.toList()
                if (catNames.isNotEmpty()) {
                    SettingsStorage.setString("library_categories", catNames.joinToString("|||"))
                }
            }

            val libraryBlock = extractArray(json, "library")
            val newLibrary = mutableListOf<LibraryManga>()
            if (libraryBlock.isNotBlank()) {
                extractObjects(libraryBlock).forEach { itemStr ->
                    val id = extractString(itemStr, "id")
                    val title = extractString(itemStr, "title")
                    val thumb = extractString(itemStr, "thumbnailUrl")
                    val sourceId = extractString(itemStr, "sourceId")
                    val mangaUrl = extractString(itemStr, "mangaUrl")
                    val category = extractString(itemStr, "category").ifBlank { "Reading" }
                    val addedAt = extractLong(itemStr, "addedAt", currentTimeMillis())
                    val lastReadAt = extractLong(itemStr, "lastReadAt", 0L)
                    val lastReadChTitle = extractString(itemStr, "lastReadChapterTitle")
                    val lastReadChUrl = extractString(itemStr, "lastReadChapterUrl")
                    val totalChapters = extractInt(itemStr, "totalChapters", 0)
                    val unreadCount = extractInt(itemStr, "unreadCount", 0)
                    val author = extractString(itemStr, "author").ifBlank { null }
                    val status = runCatching { MangaStatus.valueOf(extractString(itemStr, "status")) }.getOrDefault(MangaStatus.UNKNOWN)

                    if (title.isNotBlank()) {
                        newLibrary.add(
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
                        restoredLib++
                    }
                }
            }

            val rawLib = newLibrary.joinToString(";;;") {
                "${it.id}|||${it.title}|||${it.thumbnailUrl}|||${it.sourceId}|||${it.mangaUrl}|||${it.category}|||${it.addedAt}|||${it.lastReadAt}|||${it.lastReadChapterTitle}|||${it.lastReadChapterUrl}|||${it.totalChapters}|||${it.unreadCount}|||${it.author ?: ""}|||${it.status.name}"
            }
            SettingsStorage.setString("library_items_json", rawLib)
        }

        // 2. History Overwrite
        if (options.syncHistory) {
            val historyBlock = extractArray(json, "history")
            val newHistory = mutableListOf<HistoryEntry>()
            if (historyBlock.isNotBlank()) {
                extractObjects(historyBlock).forEach { entryStr ->
                    val title = extractString(entryStr, "mangaTitle")
                    val cover = extractString(entryStr, "mangaCover")
                    val sourceId = extractString(entryStr, "sourceId")
                    val chTitle = extractString(entryStr, "chapterTitle")
                    val chUrl = extractString(entryStr, "chapterUrl")
                    val mangaUrl = extractString(entryStr, "mangaUrl")
                    val ts = extractLong(entryStr, "timestamp", currentTimeMillis())
                    val page = extractInt(entryStr, "lastPage", 1)
                    val totalPages = extractInt(entryStr, "totalPages", 1)
                    val offset = extractInt(entryStr, "scrollOffset", 0)

                    if (title.isNotBlank() && chUrl.isNotBlank()) {
                        newHistory.add(
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
                        restoredHist++
                    }
                }
            }

            val rawHist = newHistory.joinToString(";;;") {
                "${it.mangaTitle}|||${it.mangaCover}|||${it.sourceId}|||${it.chapterTitle}|||${it.chapterUrl}|||${it.mangaUrl}|||${it.timestamp}|||${it.lastPage}|||${it.totalPages}|||${it.scrollOffset}"
            }
            SettingsStorage.setString("reading_history_json", rawHist)
        }

        // 3. Statistics Overwrite
        if (options.syncStats) {
            val statsBlock = extractArray(json, "stats")
            if (statsBlock.isNotBlank()) {
                val statsList = mutableListOf<MangaReadingStats>()
                extractObjects(statsBlock).forEach { statStr ->
                    val key = extractString(statStr, "mangaKey")
                    val title = extractString(statStr, "mangaTitle")
                    val cover = extractString(statStr, "mangaCover")
                    val sourceId = extractString(statStr, "sourceId")
                    val mangaUrl = extractString(statStr, "mangaUrl")
                    val time = extractLong(statStr, "totalTimeSeconds", 0L)
                    val pages = extractInt(statStr, "totalPagesRead", 0)
                    val lastRead = extractLong(statStr, "lastReadTimestamp", 0L)
                    val sessionCount = extractInt(statStr, "sessionCount", 1)

                    if (key.isNotBlank()) {
                        statsList.add(
                            MangaReadingStats(
                                mangaKey = key,
                                mangaTitle = title,
                                mangaCover = cover,
                                sourceId = sourceId,
                                mangaUrl = mangaUrl,
                                totalTimeSeconds = time,
                                chaptersRead = emptySet(),
                                totalPagesRead = pages,
                                lastReadTimestamp = lastRead,
                                sessionCount = sessionCount,
                                tags = emptyList()
                            )
                        )
                        restoredStats++
                    }
                }

                val rawStats = statsList.joinToString("§§§") {
                    "${it.mangaKey}|||${it.mangaTitle}|||${it.mangaCover}|||${it.sourceId}|||${it.mangaUrl}|||${it.totalTimeSeconds}|||${it.chaptersRead.joinToString(",,,")}|||${it.totalPagesRead}|||${it.lastReadTimestamp}|||${it.sessionCount}|||${it.tags.joinToString(",")}"
                }
                SettingsStorage.setString("mangadt_stats_manga_data", rawStats)
            }
        }

        // 4. Settings Overwrite
        if (options.syncSettings) {
            val pinnedBlock = extractArray(json, "pinnedSources")
            if (pinnedBlock.isNotBlank()) {
                val sources = "\"([^\"]+)\"".toRegex().findAll(pinnedBlock).map { it.groupValues[1] }.toSet()
                SettingsStorage.setStringSet("pinned_source_ids", sources)
            }

            val username = extractString(json, "username")
            val bio = extractString(json, "bio")
            val avatar = extractString(json, "avatarId").ifBlank { "avatar_1" }
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
            message = "Full Overwrite Restored: $restoredLib library items, $restoredHist history entries, $restoredStats stats",
            libraryCount = restoredLib,
            historyCount = restoredHist,
            statsCount = restoredStats
        )
    }

    private fun extractArray(json: String, arrayKey: String): String {
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
                    if (depth == 0) return json.substring(bracketStart + 1, i)
                }
            }
        }
        return ""
    }

    private fun extractObjects(arrayContent: String): List<String> {
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

    private fun extractString(json: String, key: String): String {
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
            if (c == '"') break
            sb.append(c)
        }
        return sb.toString()
    }

    private fun extractLong(json: String, key: String, default: Long = 0L): Long {
        val search = "\"$key\":"
        val start = json.indexOf(search)
        if (start == -1) return default
        var numStart = start + search.length
        while (numStart < json.length && json[numStart].isWhitespace()) numStart++
        var numEnd = numStart
        while (numEnd < json.length && (json[numEnd].isDigit() || json[numEnd] == '-')) numEnd++
        return json.substring(numStart, numEnd).toLongOrNull() ?: default
    }

    private fun extractInt(json: String, key: String, default: Int = 0): Int {
        val search = "\"$key\":"
        val start = json.indexOf(search)
        if (start == -1) return default
        var numStart = start + search.length
        while (numStart < json.length && json[numStart].isWhitespace()) numStart++
        var numEnd = numStart
        while (numEnd < json.length && (json[numEnd].isDigit() || json[numEnd] == '-')) numEnd++
        return json.substring(numStart, numEnd).toIntOrNull() ?: default
    }
}
