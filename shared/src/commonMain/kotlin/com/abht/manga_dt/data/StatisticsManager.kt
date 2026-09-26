package com.abht.manga_dt.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.abht.manga_dt.models.DailyReadingRecord
import com.abht.manga_dt.models.DayActivity
import com.abht.manga_dt.models.MangaReadingStats
import com.abht.manga_dt.models.UserProfileData

object StatisticsManager {

    var mangaStatsMap: Map<String, MangaReadingStats> by mutableStateOf(loadMangaStats())
        private set

    var dailyRecordsMap: Map<String, DailyReadingRecord> by mutableStateOf(loadDailyRecords())
        private set

    var userProfile: UserProfileData by mutableStateOf(loadUserProfile())
        private set

    // Simple custom date generator (YYYY-MM-DD) from timestamp
    private fun getTodayDateKey(timestamp: Long = currentTimeMillis()): String {
        // Approximate calculation without external heavy date libraries
        val totalDays = timestamp / (1000L * 60 * 60 * 24)
        // 1970-01-01 epoch calculation
        var year = 1970
        var days = totalDays
        while (true) {
            val leap = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
            val daysInYear = if (leap) 366 else 365
            if (days >= daysInYear) {
                days -= daysInYear
                year++
            } else {
                break
            }
        }
        val leap = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
        val monthDays = intArrayOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var month = 1
        for (mDays in monthDays) {
            if (days >= mDays) {
                days -= mDays
                month++
            } else {
                break
            }
        }
        val day = days + 1
        return "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    }

    private fun loadMangaStats(): Map<String, MangaReadingStats> {
        val raw = SettingsStorage.getString("mangadt_stats_manga_data", "")
        if (raw.isBlank()) return emptyMap()
        val result = mutableMapOf<String, MangaReadingStats>()
        raw.split("§§§").forEach { itemStr ->
            val parts = itemStr.split("|||")
            if (parts.size >= 10) {
                val key = parts[0]
                val chapters = if (parts[6].isNotBlank()) parts[6].split(",,,").toSet() else emptySet()
                val tags = if (parts.getOrNull(10)?.isNotBlank() == true) parts[10].split(",,,") else emptyList()
                result[key] = MangaReadingStats(
                    mangaKey = key,
                    mangaTitle = parts[1],
                    mangaCover = parts[2],
                    sourceId = parts[3],
                    mangaUrl = parts[4],
                    totalTimeSeconds = parts[5].toLongOrNull() ?: 0L,
                    chaptersRead = chapters,
                    totalPagesRead = parts[7].toIntOrNull() ?: 0,
                    lastReadTimestamp = parts[8].toLongOrNull() ?: 0L,
                    firstReadTimestamp = parts[9].toLongOrNull() ?: 0L,
                    sessionCount = parts.getOrNull(11)?.toIntOrNull() ?: 1,
                    tags = tags
                )
            }
        }
        return result
    }

    private fun saveMangaStats(map: Map<String, MangaReadingStats>) {
        val raw = map.values.joinToString("§§§") {
            val chaptersStr = it.chaptersRead.joinToString(",,,")
            val tagsStr = it.tags.joinToString(",,,")
            "${it.mangaKey}|||${it.mangaTitle}|||${it.mangaCover}|||${it.sourceId}|||${it.mangaUrl}|||${it.totalTimeSeconds}|||$chaptersStr|||${it.totalPagesRead}|||${it.lastReadTimestamp}|||${it.firstReadTimestamp}|||$tagsStr|||${it.sessionCount}"
        }
        SettingsStorage.setString("mangadt_stats_manga_data", raw)
        mangaStatsMap = map
    }

    private fun loadDailyRecords(): Map<String, DailyReadingRecord> {
        val raw = SettingsStorage.getString("mangadt_stats_daily_data", "")
        if (raw.isBlank()) return emptyMap()
        val result = mutableMapOf<String, DailyReadingRecord>()
        raw.split("§§§").forEach { itemStr ->
            val parts = itemStr.split("|||")
            if (parts.size >= 4) {
                val dateKey = parts[0]
                result[dateKey] = DailyReadingRecord(
                    dateKey = dateKey,
                    timeSeconds = parts[1].toLongOrNull() ?: 0L,
                    pagesRead = parts[2].toIntOrNull() ?: 0,
                    chaptersCompleted = parts[3].toIntOrNull() ?: 0
                )
            }
        }
        return result
    }

    private fun saveDailyRecords(map: Map<String, DailyReadingRecord>) {
        val raw = map.values.joinToString("§§§") {
            "${it.dateKey}|||${it.timeSeconds}|||${it.pagesRead}|||${it.chaptersCompleted}"
        }
        SettingsStorage.setString("mangadt_stats_daily_data", raw)
        dailyRecordsMap = map
    }

    private fun loadUserProfile(): UserProfileData {
        val name = SettingsStorage.getString("mangadt_user_name", "قارئ المانغا")
        val avatar = SettingsStorage.getString("mangadt_user_avatar", "avatar_1")
        val bio = SettingsStorage.getString("mangadt_user_bio", "استكشاف عوالم المانغا والويب تون الرائعة 📖✨")
        return UserProfileData(username = name, avatarId = avatar, bio = bio)
    }

    fun updateUserProfile(username: String, bio: String, avatarId: String) {
        SettingsStorage.setString("mangadt_user_name", username)
        SettingsStorage.setString("mangadt_user_avatar", avatarId)
        SettingsStorage.setString("mangadt_user_bio", bio)
        userProfile = UserProfileData(username = username, avatarId = avatarId, bio = bio)
    }

    fun getMangaStats(title: String, url: String, sourceId: String = ""): MangaReadingStats? {
        val keyWithSource = if (url.isNotBlank() && sourceId.isNotBlank()) "$sourceId::$url" else ""
        return (if (keyWithSource.isNotBlank()) mangaStatsMap[keyWithSource] else null)
            ?: mangaStatsMap[url]
            ?: mangaStatsMap[title.trim().lowercase()]
            ?: mangaStatsMap.values.firstOrNull {
                (url.isNotBlank() && it.mangaUrl == url) || it.mangaTitle.equals(title, ignoreCase = true)
            }
    }

    fun recordReadingSession(
        mangaTitle: String,
        mangaCover: String,
        sourceId: String,
        mangaUrl: String,
        durationSeconds: Long,
        pagesTurned: Int,
        completedChapterUrl: String? = null,
        tags: List<String> = emptyList(),
        isNsfw: Boolean = false
    ) {
        if (AppSettings.nsfwExcludeFromStats && (isNsfw || AppSettings.isMangaNsfw(isNsfw, tags))) {
            return
        }
        if (durationSeconds <= 0 && pagesTurned <= 0 && completedChapterUrl == null) return
        val now = currentTimeMillis()
        val key = if (mangaUrl.isNotBlank()) "$sourceId::$mangaUrl" else mangaTitle.trim().lowercase()

        // 1. Update Manga Stats
        val existing = mangaStatsMap[key]
        val updatedChapters = (existing?.chaptersRead ?: emptySet()).toMutableSet()
        if (!completedChapterUrl.isNullOrBlank()) {
            updatedChapters.add(completedChapterUrl)
        }

        val updatedMangaStats = MangaReadingStats(
            mangaKey = key,
            mangaTitle = if (mangaTitle.isNotBlank()) mangaTitle else existing?.mangaTitle ?: "Unknown",
            mangaCover = if (mangaCover.isNotBlank()) mangaCover else existing?.mangaCover ?: "",
            sourceId = if (sourceId.isNotBlank()) sourceId else existing?.sourceId ?: "",
            mangaUrl = if (mangaUrl.isNotBlank()) mangaUrl else existing?.mangaUrl ?: "",
            totalTimeSeconds = (existing?.totalTimeSeconds ?: 0L) + durationSeconds.coerceAtLeast(0L),
            chaptersRead = updatedChapters,
            totalPagesRead = (existing?.totalPagesRead ?: 0) + pagesTurned.coerceAtLeast(0),
            lastReadTimestamp = now,
            firstReadTimestamp = if (existing == null || existing.firstReadTimestamp <= 0L) now else existing.firstReadTimestamp,
            sessionCount = (existing?.sessionCount ?: 0) + 1,
            tags = if (tags.isNotEmpty()) tags else existing?.tags ?: emptyList()
        )

        val newMangaMap = mangaStatsMap.toMutableMap()
        newMangaMap[key] = updatedMangaStats
        saveMangaStats(newMangaMap)

        // 2. Update Daily Records
        val todayKey = getTodayDateKey(now)
        val existingDaily = dailyRecordsMap[todayKey]
        val updatedDaily = DailyReadingRecord(
            dateKey = todayKey,
            timeSeconds = (existingDaily?.timeSeconds ?: 0L) + durationSeconds.coerceAtLeast(0L),
            pagesRead = (existingDaily?.pagesRead ?: 0) + pagesTurned.coerceAtLeast(0),
            chaptersCompleted = (existingDaily?.chaptersCompleted ?: 0) + (if (!completedChapterUrl.isNullOrBlank()) 1 else 0)
        )

        val newDailyMap = dailyRecordsMap.toMutableMap()
        newDailyMap[todayKey] = updatedDaily
        saveDailyRecords(newDailyMap)
    }

    fun batchMergeStats(incomingList: List<MangaReadingStats>): Int {
        if (incomingList.isEmpty()) return 0
        val newMangaMap = mangaStatsMap.toMutableMap()
        var mergedCount = 0

        for (stat in incomingList) {
            val key = stat.mangaKey.ifBlank {
                if (stat.mangaUrl.isNotBlank()) "${stat.sourceId}::${stat.mangaUrl}" else stat.mangaTitle.trim().lowercase()
            }
            if (key.isBlank()) continue

            val existing = newMangaMap[key]
            if (existing == null) {
                newMangaMap[key] = stat.copy(mangaKey = key)
                mergedCount++
            } else {
                val unionChapters = existing.chaptersRead + stat.chaptersRead
                val maxTime = maxOf(existing.totalTimeSeconds, stat.totalTimeSeconds)
                val maxPages = maxOf(existing.totalPagesRead, stat.totalPagesRead)
                val latestRead = maxOf(existing.lastReadTimestamp, stat.lastReadTimestamp)
                val earliestFirst = if (existing.firstReadTimestamp > 0 && stat.firstReadTimestamp > 0) {
                    minOf(existing.firstReadTimestamp, stat.firstReadTimestamp)
                } else maxOf(existing.firstReadTimestamp, stat.firstReadTimestamp)
                val maxSessions = maxOf(existing.sessionCount, stat.sessionCount)
                val combinedTags = (existing.tags + stat.tags).distinct()

                val merged = existing.copy(
                    totalTimeSeconds = maxTime,
                    chaptersRead = unionChapters,
                    totalPagesRead = maxPages,
                    lastReadTimestamp = latestRead,
                    firstReadTimestamp = earliestFirst,
                    sessionCount = maxSessions,
                    tags = combinedTags
                )
                newMangaMap[key] = merged
                mergedCount++
            }
        }

        saveMangaStats(newMangaMap)
        return mergedCount
    }

    fun getTotalReadingTimeSeconds(): Long {
        return mangaStatsMap.values.sumOf { it.totalTimeSeconds }
    }

    fun getTotalChaptersReadCount(): Int {
        return mangaStatsMap.values.sumOf { it.chaptersRead.size }
    }

    fun getTotalPagesReadCount(): Int {
        return mangaStatsMap.values.sumOf { it.totalPagesRead }
    }

    fun getTotalMangaCount(): Int {
        return mangaStatsMap.size
    }

    fun getCurrentStreakDays(): Int {
        if (dailyRecordsMap.isEmpty()) return 0
        var streak = 0
        var checkTimestamp = currentTimeMillis()
        var todayChecked = false

        for (i in 0 until 365) {
            val key = getTodayDateKey(checkTimestamp)
            val record = dailyRecordsMap[key]
            if (record != null && (record.timeSeconds > 30 || record.pagesRead > 0)) {
                streak++
                todayChecked = true
            } else {
                if (i == 0 && !todayChecked) {
                    // Today might not have reading yet, check yesterday
                } else {
                    break
                }
            }
            checkTimestamp -= 24 * 60 * 60 * 1000L
        }
        return streak
    }

    fun getWeeklyActivityDetails(): List<DayActivity> {
        val list = mutableListOf<DayActivity>()
        val now = currentTimeMillis()
        val todayKey = getTodayDateKey(now)
        val arDays = listOf("أحد", "إثن", "ثلا", "أرب", "خمي", "جمع", "سبت")
        val enDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        var checkTimestamp = now - 6 * 24 * 60 * 60 * 1000L
        for (i in 0 until 7) {
            val key = getTodayDateKey(checkTimestamp)
            val totalDays = checkTimestamp / (1000L * 60 * 60 * 24)
            val dayOfWeek = (((totalDays + 4) % 7 + 7) % 7).toInt()
            val record = dailyRecordsMap[key]

            val seconds = record?.timeSeconds ?: 0L
            val pages = record?.pagesRead ?: 0
            val minutes = when {
                seconds > 0 -> ((seconds + 30) / 60).coerceAtLeast(if (seconds >= 10) 1L else 0L)
                pages > 0 -> (pages / 2L).coerceAtLeast(1L)
                else -> 0L
            }

            list.add(
                DayActivity(
                    dateKey = key,
                    dayNameAr = arDays[dayOfWeek],
                    dayNameEn = enDays[dayOfWeek],
                    minutes = minutes,
                    pages = pages,
                    isToday = (key == todayKey)
                )
            )
            checkTimestamp += 24 * 60 * 60 * 1000L
        }
        return list
    }

    fun getWeeklyActivityList(): List<Pair<String, Long>> {
        return getWeeklyActivityDetails().map { it.dateKey to it.minutes }
    }

    fun getTopGenres(): List<Pair<String, Int>> {
        val genreCounts = mutableMapOf<String, Int>()
        mangaStatsMap.values.forEach { manga ->
            manga.tags.forEach { tag ->
                val clean = tag.trim()
                if (clean.isNotBlank()) {
                    genreCounts[clean] = (genreCounts[clean] ?: 0) + (manga.chaptersRead.size.coerceAtLeast(1))
                }
            }
        }
        return genreCounts.entries
            .sortedByDescending { it.value }
            .take(6)
            .map { it.key to it.value }
    }

    fun formatDuration(totalSeconds: Long, isArabic: Boolean): String {
        if (totalSeconds < 60) {
            return if (isArabic) "$totalSeconds ث" else "${totalSeconds}s"
        }
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return when {
            hours > 0 && minutes > 0 -> {
                if (isArabic) "$hours س و $minutes د" else "${hours}h ${minutes}m"
            }
            hours > 0 -> {
                if (isArabic) "$hours ساعة" else "${hours}h"
            }
            else -> {
                if (isArabic) "$minutes دقيقة" else "${minutes}m"
            }
        }
    }

    fun getReaderRank(totalSeconds: Long, isArabic: Boolean): Pair<String, Float> {
        val hours = totalSeconds / 3600f
        return when {
            hours < 2f -> (if (isArabic) "مبتدئ" else "Novice") to (hours / 2f).coerceIn(0f, 1f)
            hours < 10f -> (if (isArabic) "قارئ نشط" else "Active Reader") to ((hours - 2f) / 8f).coerceIn(0f, 1f)
            hours < 30f -> (if (isArabic) "عاشق للمانغا" else "Manga Enthusiast") to ((hours - 10f) / 20f).coerceIn(0f, 1f)
            hours < 100f -> (if (isArabic) "خبير أوتاكو" else "Otaku Master") to ((hours - 30f) / 70f).coerceIn(0f, 1f)
            else -> (if (isArabic) "أسطورة المانغا" else "Manga Legend") to 1f
        }
    }

    fun clearAllStats() {
        SettingsStorage.setString("mangadt_stats_manga_data", "")
        SettingsStorage.setString("mangadt_stats_daily_data", "")
        mangaStatsMap = emptyMap()
        dailyRecordsMap = emptyMap()
    }

    fun clearNsfwStats() {
        val newMangaMap = mangaStatsMap.filterNot { (_, stat) ->
            AppSettings.isMangaNsfw(false, stat.tags)
        }
        saveMangaStats(newMangaMap)
    }

    fun reload() {
        mangaStatsMap = loadMangaStats()
        dailyRecordsMap = loadDailyRecords()
        userProfile = loadUserProfile()
    }
}
