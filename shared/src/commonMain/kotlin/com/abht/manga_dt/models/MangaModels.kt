package com.abht.manga_dt.models

data class Manga(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val author: String? = null,
    val description: String? = null,
    val status: MangaStatus = MangaStatus.UNKNOWN,
    val source: String,
    val url: String = "",
    val chapters: List<Chapter> = emptyList(),
    val altTitle: String? = null,
    val rating: Float = 0f,
    val isNsfw: Boolean = false,
    val tags: List<String> = emptyList(),
    val publicUrl: String = "",
    val latestChapter: String? = null,
    val updatedAt: String? = null,
    val views: String? = null,
    val releaseYear: String? = null,
    val mangaType: String? = null,
    val ratingCount: Int? = null
)

enum class MangaStatus {
    ONGOING, COMPLETED, DROPPED, ON_HOLD, UNKNOWN
}

data class Chapter(
    val id: String,
    val mangaId: String,
    val title: String,
    val chapterNumber: Float,
    val uploadDate: Long? = null,
    val scanlator: String? = null,
    val url: String = ""
)

data class ReaderPage(
    val url: String,
    val pageNumber: Int
)

data class DownloadTask(
    val id: String,
    val title: String,
    val subtitle: String,
    val progress: Float,
    val status: DownloadStatus,
    val thumbnailUrl: String,
    val sourceId: String = "",
    val mangaUrl: String = "",
    val chapterNumber: Float = 0f
)

enum class DownloadStatus {
    QUEUED, DOWNLOADING, PAUSED, COMPLETED, ERROR
}

data class HistoryEntry(
    val mangaTitle: String,
    val mangaCover: String,
    val sourceId: String,
    val chapterTitle: String,
    val chapterUrl: String,
    val mangaUrl: String = "",
    val timestamp: Long = 0L,
    val lastPage: Int = 1,
    val totalPages: Int = 1,
    val scrollOffset: Int = 0
)

data class LibraryManga(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val sourceId: String,
    val mangaUrl: String = "",
    val category: String = "Default",
    val addedAt: Long = 0L,
    val lastReadAt: Long = 0L,
    val lastReadChapterTitle: String = "",
    val lastReadChapterUrl: String = "",
    val totalChapters: Int = 0,
    val unreadCount: Int = 0,
    val author: String? = null,
    val status: MangaStatus = MangaStatus.UNKNOWN
)

enum class LibrarySortOrder(val title: String) {
    LAST_READ("Last Read"),
    ALPHABETICAL("Alphabetical (A-Z)"),
    DATE_ADDED("Date Added"),
    UNREAD_COUNT("Unread Count")
}

data class MangaReadingStats(
    val mangaKey: String,
    val mangaTitle: String,
    val mangaCover: String = "",
    val sourceId: String = "",
    val mangaUrl: String = "",
    val totalTimeSeconds: Long = 0L,
    val chaptersRead: Set<String> = emptySet(),
    val totalPagesRead: Int = 0,
    val lastReadTimestamp: Long = 0L,
    val firstReadTimestamp: Long = 0L,
    val sessionCount: Int = 0,
    val tags: List<String> = emptyList()
)

data class DailyReadingRecord(
    val dateKey: String,
    val timeSeconds: Long = 0L,
    val pagesRead: Int = 0,
    val chaptersCompleted: Int = 0
)

data class UserProfileData(
    val username: String = "قارئ المانغا",
    val avatarId: String = "avatar_1",
    val bio: String = "استكشاف عوالم المانغا والويب تون الرائعة 📖✨"
)

data class DayActivity(
    val dateKey: String,
    val dayNameAr: String,
    val dayNameEn: String,
    val minutes: Long,
    val pages: Int,
    val isToday: Boolean
)
