package com.abht.manga_dt.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.abht.manga_dt.models.Manga
import com.abht.manga_dt.ui.models.LayoutMode

data class MangaSource(
    val id: String,
    val name: String,
    val iconUrl: String? = null,
    val locale: String? = null,
    val contentType: String? = null,
    val isBroken: Boolean = false
)

object MangaDataCache {
    var cachedSources: List<MangaSource>? = null
    val cachedPopularManga = mutableMapOf<String, List<Manga>>()
    val cachedMangaDetails = mutableMapOf<String, Manga>()
    val cachedPages = mutableMapOf<String, List<com.abht.manga_dt.models.ReaderPage>>()
}

expect class MangaSourceManager() {
    suspend fun getAvailableSources(): List<MangaSource>
    suspend fun searchManga(sourceId: String, query: String): List<Manga>
    suspend fun getPopularManga(sourceId: String, page: Int = 1): List<Manga>
    suspend fun getMangaDetails(sourceId: String, mangaUrl: String): Manga?
    suspend fun getPages(sourceId: String, chapterUrl: String): List<com.abht.manga_dt.models.ReaderPage>
    suspend fun testSource(sourceId: String): String?
    fun getSourceBaseUrl(sourceId: String): String
}

expect fun currentTimeMillis(): Long

expect object SettingsStorage {
    fun getString(key: String, defaultValue: String = ""): String
    fun setString(key: String, value: String)
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    fun setBoolean(key: String, value: Boolean)
    fun getInt(key: String, defaultValue: Int = 0): Int
    fun setInt(key: String, value: Int)
    fun getStringSet(key: String): Set<String>
    fun setStringSet(key: String, values: Set<String>)
}

enum class ThemePreset(val title: String, val primaryColor: Long, val accentDotColor: Long) {
    TOTORO("Totoro", 0xFF386A20, 0xFF70A352),
    DYNAMIC("Dynamic", 0xFF6750A4, 0xFF9E86E0),
    EXPRESSIVE("Expressive", 0xFF65558F, 0xFFB5A4E3),
    MIKU("Miku", 0xFF00687A, 0xFF4BD6EC),
    ASUKA("Asuka", 0xFF9C4146, 0xFFE2868A),
    MIO("Mio", 0xFF7E5700, 0xFFFFBA27)
}

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT;

    companion object {
        fun fromName(name: String): ThemeMode {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: SYSTEM
        }
    }
}

object AppSettings {
    var browseLayoutMode: com.abht.manga_dt.ui.models.LayoutMode by androidx.compose.runtime.mutableStateOf(
        runCatching { 
            com.abht.manga_dt.ui.models.LayoutMode.valueOf(SettingsStorage.getString("browse_layout_mode", com.abht.manga_dt.ui.models.LayoutMode.LIST.name))
        }.getOrDefault(com.abht.manga_dt.ui.models.LayoutMode.LIST)
    )

    var browseSortAscending: Boolean by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getBoolean("browse_sort_asc", true)
    )

    var browseSelectedLanguage: String by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getString("browse_selected_lang", "ALL")
    )

    var browseSelectedContentType: String by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getString("browse_selected_type", "ALL")
    )

    var pinnedSourceIds: Set<String> by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getStringSet("pinned_source_ids")
    )

    var libraryLayoutMode: com.abht.manga_dt.ui.models.LayoutMode by androidx.compose.runtime.mutableStateOf(
        runCatching {
            com.abht.manga_dt.ui.models.LayoutMode.valueOf(SettingsStorage.getString("library_layout_mode", com.abht.manga_dt.ui.models.LayoutMode.COMFORTABLE.name))
        }.getOrDefault(com.abht.manga_dt.ui.models.LayoutMode.COMFORTABLE)
    )

    var isNsfwAllowed: Boolean by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getBoolean("is_nsfw_allowed", false)
    )
        private set

    var themeMode: ThemeMode by androidx.compose.runtime.mutableStateOf(
        ThemeMode.fromName(SettingsStorage.getString("theme_mode", ThemeMode.SYSTEM.name))
    )
        private set

    var darkTheme: Boolean by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getBoolean("dark_theme", true)
    )
        private set

    var themePreset: ThemePreset by androidx.compose.runtime.mutableStateOf(
        runCatching {
            ThemePreset.valueOf(SettingsStorage.getString("theme_preset", ThemePreset.DYNAMIC.name))
        }.getOrDefault(ThemePreset.DYNAMIC)
    )
        private set

    var amoledBlack: Boolean by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getBoolean("amoled_black", false)
    )
        private set

    var appLanguage: AppLanguage by androidx.compose.runtime.mutableStateOf(
        AppLanguage.fromCode(SettingsStorage.getString("app_language", AppLanguage.SYSTEM.code))
    )
        private set

    fun updateThemePreset(preset: ThemePreset) {
        themePreset = preset
        SettingsStorage.setString("theme_preset", preset.name)
    }

    fun setAmoled(enabled: Boolean) {
        amoledBlack = enabled
        SettingsStorage.setBoolean("amoled_black", enabled)
    }

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        SettingsStorage.setString("theme_mode", mode.name)
    }

    fun setDarkThemeMode(enabled: Boolean) {
        darkTheme = enabled
        themeMode = if (enabled) ThemeMode.DARK else ThemeMode.LIGHT
        SettingsStorage.setBoolean("dark_theme", enabled)
        SettingsStorage.setString("theme_mode", themeMode.name)
    }

    fun setLanguage(lang: AppLanguage) {
        appLanguage = lang
        SettingsStorage.setString("app_language", lang.code)
    }

    fun setBrowseLayout(mode: com.abht.manga_dt.ui.models.LayoutMode) {
        browseLayoutMode = mode
        SettingsStorage.setString("browse_layout_mode", mode.name)
    }

    fun setBrowseSort(ascending: Boolean) {
        browseSortAscending = ascending
        SettingsStorage.setBoolean("browse_sort_asc", ascending)
    }

    fun setBrowseLanguage(lang: String) {
        browseSelectedLanguage = lang
        SettingsStorage.setString("browse_selected_lang", lang)
    }

    fun setBrowseContentType(type: String) {
        browseSelectedContentType = type
        SettingsStorage.setString("browse_selected_type", type)
    }

    fun togglePinnedSource(sourceId: String) {
        val newSet = if (pinnedSourceIds.contains(sourceId)) {
            pinnedSourceIds - sourceId
        } else {
            pinnedSourceIds + sourceId
        }
        pinnedSourceIds = newSet
        SettingsStorage.setStringSet("pinned_source_ids", newSet)
    }

    fun setLibraryLayout(mode: com.abht.manga_dt.ui.models.LayoutMode) {
        libraryLayoutMode = mode
        SettingsStorage.setString("library_layout_mode", mode.name)
    }

    fun setNsfw(allowed: Boolean) {
        isNsfwAllowed = allowed
        SettingsStorage.setBoolean("is_nsfw_allowed", allowed)
        MangaDataCache.cachedSources = null
        MangaDataCache.cachedPopularManga.clear()
    }

    var readingMode: com.abht.manga_dt.ui.models.ReadingMode by androidx.compose.runtime.mutableStateOf(
        runCatching {
            com.abht.manga_dt.ui.models.ReadingMode.valueOf(SettingsStorage.getString("reader_mode", com.abht.manga_dt.ui.models.ReadingMode.WEBTOON.name))
        }.getOrDefault(com.abht.manga_dt.ui.models.ReadingMode.WEBTOON)
    )
        private set

    var readerBackground: com.abht.manga_dt.ui.models.ReaderBackground by androidx.compose.runtime.mutableStateOf(
        runCatching {
            com.abht.manga_dt.ui.models.ReaderBackground.valueOf(SettingsStorage.getString("reader_bg", com.abht.manga_dt.ui.models.ReaderBackground.BLACK.name))
        }.getOrDefault(com.abht.manga_dt.ui.models.ReaderBackground.BLACK)
    )
        private set

    var readerScaleMode: com.abht.manga_dt.ui.models.ReaderScaleMode by androidx.compose.runtime.mutableStateOf(
        runCatching {
            com.abht.manga_dt.ui.models.ReaderScaleMode.valueOf(SettingsStorage.getString("reader_scale", com.abht.manga_dt.ui.models.ReaderScaleMode.FIT_WIDTH.name))
        }.getOrDefault(com.abht.manga_dt.ui.models.ReaderScaleMode.FIT_WIDTH)
    )
        private set

    var showPageNumberPill: Boolean by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getBoolean("reader_page_pill", true)
    )
        private set

    fun setReaderMode(mode: com.abht.manga_dt.ui.models.ReadingMode) = updateReadingMode(mode)

    fun updateReadingMode(mode: com.abht.manga_dt.ui.models.ReadingMode) {
        readingMode = mode
        SettingsStorage.setString("reader_mode", mode.name)
    }

    fun updateReaderBackground(bg: com.abht.manga_dt.ui.models.ReaderBackground) {
        readerBackground = bg
        SettingsStorage.setString("reader_bg", bg.name)
    }

    fun updateReaderScaleMode(scale: com.abht.manga_dt.ui.models.ReaderScaleMode) {
        readerScaleMode = scale
        SettingsStorage.setString("reader_scale", scale.name)
    }

    fun updateShowPageNumberPill(show: Boolean) {
        showPageNumberPill = show
        SettingsStorage.setBoolean("reader_page_pill", show)
    }

    fun reload() {
        browseLayoutMode = runCatching { 
            com.abht.manga_dt.ui.models.LayoutMode.valueOf(SettingsStorage.getString("browse_layout_mode", com.abht.manga_dt.ui.models.LayoutMode.LIST.name))
        }.getOrDefault(com.abht.manga_dt.ui.models.LayoutMode.LIST)
        browseSortAscending = SettingsStorage.getBoolean("browse_sort_asc", true)
        browseSelectedLanguage = SettingsStorage.getString("browse_selected_lang", "ALL")
        browseSelectedContentType = SettingsStorage.getString("browse_selected_type", "ALL")
        pinnedSourceIds = SettingsStorage.getStringSet("pinned_source_ids")
        libraryLayoutMode = runCatching {
            com.abht.manga_dt.ui.models.LayoutMode.valueOf(SettingsStorage.getString("library_layout_mode", com.abht.manga_dt.ui.models.LayoutMode.COMFORTABLE.name))
        }.getOrDefault(com.abht.manga_dt.ui.models.LayoutMode.COMFORTABLE)
        isNsfwAllowed = SettingsStorage.getBoolean("is_nsfw_allowed", false)
        darkTheme = SettingsStorage.getBoolean("dark_theme", true)
        readingMode = runCatching {
            com.abht.manga_dt.ui.models.ReadingMode.valueOf(SettingsStorage.getString("reader_mode", com.abht.manga_dt.ui.models.ReadingMode.WEBTOON.name))
        }.getOrDefault(com.abht.manga_dt.ui.models.ReadingMode.WEBTOON)
        readerBackground = runCatching {
            com.abht.manga_dt.ui.models.ReaderBackground.valueOf(SettingsStorage.getString("reader_bg", com.abht.manga_dt.ui.models.ReaderBackground.BLACK.name))
        }.getOrDefault(com.abht.manga_dt.ui.models.ReaderBackground.BLACK)
        readerScaleMode = runCatching {
            com.abht.manga_dt.ui.models.ReaderScaleMode.valueOf(SettingsStorage.getString("reader_scale", com.abht.manga_dt.ui.models.ReaderScaleMode.FIT_WIDTH.name))
        }.getOrDefault(com.abht.manga_dt.ui.models.ReaderScaleMode.FIT_WIDTH)
        showPageNumberPill = SettingsStorage.getBoolean("reader_page_pill", true)
    }
}

object HistoryManager {
    var historyEntries: List<com.abht.manga_dt.models.HistoryEntry> by androidx.compose.runtime.mutableStateOf(loadHistory())
        private set

    private fun loadHistory(): List<com.abht.manga_dt.models.HistoryEntry> {
        val raw = SettingsStorage.getString("reading_history_json", "")
        if (raw.isBlank()) return emptyList()
        return raw.split(";;;").mapNotNull { entryStr ->
            val parts = entryStr.split("|||")
            if (parts.size >= 7) {
                com.abht.manga_dt.models.HistoryEntry(
                    mangaTitle = parts[0],
                    mangaCover = parts[1],
                    sourceId = parts[2],
                    chapterTitle = parts[3],
                    chapterUrl = parts[4],
                    mangaUrl = parts[5],
                    timestamp = parts[6].toLongOrNull() ?: 0L,
                    lastPage = parts.getOrNull(7)?.toIntOrNull() ?: 1,
                    totalPages = parts.getOrNull(8)?.toIntOrNull() ?: 1,
                    scrollOffset = parts.getOrNull(9)?.toIntOrNull() ?: 0
                )
            } else null
        }
    }

    private fun saveHistory(list: List<com.abht.manga_dt.models.HistoryEntry>) {
        val raw = list.joinToString(";;;") {
            "${it.mangaTitle}|||${it.mangaCover}|||${it.sourceId}|||${it.chapterTitle}|||${it.chapterUrl}|||${it.mangaUrl}|||${it.timestamp}|||${it.lastPage}|||${it.totalPages}|||${it.scrollOffset}"
        }
        SettingsStorage.setString("reading_history_json", raw)
        historyEntries = list
    }

    fun addOrUpdateHistory(entry: com.abht.manga_dt.models.HistoryEntry) {
        val existing = historyEntries.firstOrNull { 
            (entry.mangaUrl.isNotBlank() && it.mangaUrl == entry.mangaUrl) || it.mangaTitle.equals(entry.mangaTitle, ignoreCase = true) 
        }
        val preservedPage = if (existing != null && existing.chapterUrl == entry.chapterUrl && existing.lastPage > 1 && entry.lastPage <= 1) {
            existing.lastPage
        } else {
            entry.lastPage
        }
        val preservedTotal = if (existing != null && existing.chapterUrl == entry.chapterUrl && existing.totalPages > 1 && entry.totalPages <= 1) {
            existing.totalPages
        } else {
            entry.totalPages
        }
        val preservedOffset = if (existing != null && existing.chapterUrl == entry.chapterUrl && existing.scrollOffset > 0 && entry.scrollOffset <= 0) {
            existing.scrollOffset
        } else {
            entry.scrollOffset
        }
        val filtered = historyEntries.filterNot { 
            (entry.mangaUrl.isNotBlank() && it.mangaUrl == entry.mangaUrl) || it.mangaTitle.equals(entry.mangaTitle, ignoreCase = true) 
        }
        val updated = listOf(entry.copy(lastPage = preservedPage, totalPages = preservedTotal, scrollOffset = preservedOffset)) + filtered
        saveHistory(updated.take(100))
    }

    fun updatePageProgress(mangaTitle: String, chapterUrl: String, page: Int, totalPages: Int, scrollOffset: Int = 0) {
        if (page <= 0) return
        val updated = historyEntries.map {
            if ((chapterUrl.isNotBlank() && it.chapterUrl == chapterUrl) || it.mangaTitle.equals(mangaTitle, ignoreCase = true)) {
                it.copy(
                    lastPage = page,
                    totalPages = totalPages.coerceAtLeast(1),
                    scrollOffset = scrollOffset,
                    timestamp = currentTimeMillis(),
                    chapterUrl = if (chapterUrl.isNotBlank()) chapterUrl else it.chapterUrl
                )
            } else it
        }
        saveHistory(updated)
    }

    fun deleteHistoryEntry(entry: com.abht.manga_dt.models.HistoryEntry) {
        val updated = historyEntries.filterNot { it.chapterUrl == entry.chapterUrl && it.mangaTitle == entry.mangaTitle }
        saveHistory(updated)
    }

    fun clearAllHistory() {
        saveHistory(emptyList())
    }

    fun reload() {
        historyEntries = loadHistory()
    }
}

object LibraryManager {
    var libraryItems: List<com.abht.manga_dt.models.LibraryManga> by androidx.compose.runtime.mutableStateOf(loadLibrary())
        private set

    var categories: List<String> by androidx.compose.runtime.mutableStateOf(loadCategories())
        private set

    var layoutMode: com.abht.manga_dt.ui.models.LayoutMode by androidx.compose.runtime.mutableStateOf(
        runCatching {
            com.abht.manga_dt.ui.models.LayoutMode.valueOf(SettingsStorage.getString("library_layout_mode", com.abht.manga_dt.ui.models.LayoutMode.COMFORTABLE.name))
        }.getOrDefault(com.abht.manga_dt.ui.models.LayoutMode.COMFORTABLE)
    )
        private set

    var sortOrder: com.abht.manga_dt.models.LibrarySortOrder by androidx.compose.runtime.mutableStateOf(
        runCatching {
            com.abht.manga_dt.models.LibrarySortOrder.valueOf(SettingsStorage.getString("library_sort_order", com.abht.manga_dt.models.LibrarySortOrder.LAST_READ.name))
        }.getOrDefault(com.abht.manga_dt.models.LibrarySortOrder.LAST_READ)
    )
        private set

    fun updateLayoutMode(mode: com.abht.manga_dt.ui.models.LayoutMode) {
        layoutMode = mode
        SettingsStorage.setString("library_layout_mode", mode.name)
    }

    fun updateSortOrder(order: com.abht.manga_dt.models.LibrarySortOrder) {
        sortOrder = order
        SettingsStorage.setString("library_sort_order", order.name)
    }

    fun reload() {
        libraryItems = loadLibrary()
        categories = loadCategories()
        layoutMode = runCatching {
            com.abht.manga_dt.ui.models.LayoutMode.valueOf(SettingsStorage.getString("library_layout_mode", com.abht.manga_dt.ui.models.LayoutMode.COMFORTABLE.name))
        }.getOrDefault(com.abht.manga_dt.ui.models.LayoutMode.COMFORTABLE)
        sortOrder = runCatching {
            com.abht.manga_dt.models.LibrarySortOrder.valueOf(SettingsStorage.getString("library_sort_order", com.abht.manga_dt.models.LibrarySortOrder.LAST_READ.name))
        }.getOrDefault(com.abht.manga_dt.models.LibrarySortOrder.LAST_READ)
    }

    private fun loadCategories(): List<String> {
        val raw = SettingsStorage.getString("library_categories", "Reading|||Favorites|||Plan to Read|||Completed")
        val list = raw.split("|||").map { it.trim() }.filter { it.isNotEmpty() }
        return if (list.isEmpty()) listOf("Reading", "Favorites", "Plan to Read", "Completed") else list
    }

    fun addCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank() && !categories.contains(trimmed)) {
            val updated = categories + trimmed
            categories = updated
            SettingsStorage.setString("library_categories", updated.joinToString("|||"))
        }
    }

    fun deleteCategory(name: String) {
        val updated = categories.filterNot { it == name }
        categories = updated
        SettingsStorage.setString("library_categories", updated.joinToString("|||"))
        val updatedItems = libraryItems.map { if (it.category == name) it.copy(category = "Reading") else it }
        saveLibrary(updatedItems)
    }

    private fun loadLibrary(): List<com.abht.manga_dt.models.LibraryManga> {
        val raw = SettingsStorage.getString("library_items_json", "")
        if (raw.isBlank()) return emptyList()
        return raw.split(";;;").mapNotNull { itemStr ->
            val parts = itemStr.split("|||")
            if (parts.size >= 6) {
                com.abht.manga_dt.models.LibraryManga(
                    id = parts[0],
                    title = parts[1],
                    thumbnailUrl = parts[2],
                    sourceId = parts[3],
                    mangaUrl = parts[4],
                    category = parts[5],
                    addedAt = parts.getOrNull(6)?.toLongOrNull() ?: 0L,
                    lastReadAt = parts.getOrNull(7)?.toLongOrNull() ?: 0L,
                    lastReadChapterTitle = parts.getOrNull(8) ?: "",
                    lastReadChapterUrl = parts.getOrNull(9) ?: "",
                    totalChapters = parts.getOrNull(10)?.toIntOrNull() ?: 0,
                    unreadCount = parts.getOrNull(11)?.toIntOrNull() ?: 0,
                    author = parts.getOrNull(12),
                    status = runCatching { com.abht.manga_dt.models.MangaStatus.valueOf(parts.getOrNull(13) ?: "") }.getOrDefault(com.abht.manga_dt.models.MangaStatus.UNKNOWN)
                )
            } else null
        }
    }

    private fun saveLibrary(list: List<com.abht.manga_dt.models.LibraryManga>) {
        val raw = list.joinToString(";;;") {
            "${it.id}|||${it.title}|||${it.thumbnailUrl}|||${it.sourceId}|||${it.mangaUrl}|||${it.category}|||${it.addedAt}|||${it.lastReadAt}|||${it.lastReadChapterTitle}|||${it.lastReadChapterUrl}|||${it.totalChapters}|||${it.unreadCount}|||${it.author ?: ""}|||${it.status.name}"
        }
        SettingsStorage.setString("library_items_json", raw)
        libraryItems = list
    }

    fun isMangaInLibrary(mangaUrl: String, title: String): Boolean {
        return libraryItems.any { (mangaUrl.isNotBlank() && it.mangaUrl == mangaUrl) || it.title.equals(title, ignoreCase = true) }
    }

    fun getLibraryManga(mangaUrl: String, title: String): com.abht.manga_dt.models.LibraryManga? {
        return libraryItems.firstOrNull { (mangaUrl.isNotBlank() && it.mangaUrl == mangaUrl) || it.title.equals(title, ignoreCase = true) }
    }

    fun addToLibrary(item: com.abht.manga_dt.models.LibraryManga) {
        val filtered = libraryItems.filterNot { (item.mangaUrl.isNotBlank() && it.mangaUrl == item.mangaUrl) || it.title.equals(item.title, ignoreCase = true) }
        val updated = listOf(item) + filtered
        saveLibrary(updated)
    }

    fun removeFromLibrary(mangaUrl: String, title: String) {
        val updated = libraryItems.filterNot { (mangaUrl.isNotBlank() && it.mangaUrl == mangaUrl) || it.title.equals(title, ignoreCase = true) }
        saveLibrary(updated)
    }

    fun updateCategory(mangaUrl: String, title: String, newCategory: String) {
        val updated = libraryItems.map {
            if ((mangaUrl.isNotBlank() && it.mangaUrl == mangaUrl) || it.title.equals(title, ignoreCase = true)) {
                it.copy(category = newCategory)
            } else it
        }
        saveLibrary(updated)
    }

    fun updateProgress(mangaUrl: String, title: String, chapterTitle: String, chapterUrl: String) {
        val updated = libraryItems.map {
            if ((mangaUrl.isNotBlank() && it.mangaUrl == mangaUrl) || it.title.equals(title, ignoreCase = true)) {
                it.copy(
                    lastReadAt = currentTimeMillis(),
                    lastReadChapterTitle = chapterTitle,
                    lastReadChapterUrl = chapterUrl
                )
            } else it
        }
        saveLibrary(updated)
    }
}



