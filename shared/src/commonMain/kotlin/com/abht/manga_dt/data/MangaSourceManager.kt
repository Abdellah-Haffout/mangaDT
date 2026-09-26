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
    val cachedArtworkSeeds = mutableMapOf<String, androidx.compose.ui.graphics.Color>()
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

enum class ThemePreset(
    val title: String,
    val titleAr: String,
    val primaryColor: Long,
    val accentDotColor: Long
) {
    NOGUCHI_GOLD("Noguchi Gold", "ذهب نوغوتشي", 0xFFFCCD4A, 0xFFD33333),
    CYBER_VOLT("Cyber Volt", "سايبر فولت", 0xFFD0FF00, 0xFF8116E0),
    NEON_ROSE("Neon Rose", "وردي نيوني", 0xFFFF096C, 0xFF4F6172),
    MIDNIGHT_SOLAR("Midnight Solar", "أزرق ليلي شمسي", 0xFFFFD72A, 0xFFC52A5D),
    DYNAMIC("Dynamic", "تلقائي ديناميكي", 0xFF6750A4, 0xFF9E86E0),
    TOTORO("Totoro", "توتورو", 0xFF386A20, 0xFF70A352),
    EXPRESSIVE("Expressive", "تعبيري", 0xFF65558F, 0xFFB5A4E3),
    MIKU("Miku", "ميكو", 0xFF00687A, 0xFF4BD6EC),
    ASUKA("Asuka", "أسوكا", 0xFF9C4146, 0xFFE2868A),
    MIO("Mio", "ميو", 0xFF7E5700, 0xFFFFBA27);

    fun getDisplayName(isArabic: Boolean): String = if (isArabic) titleAr else title
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

    var enableKotatsuSources: Boolean by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getBoolean("enable_kotatsu_sources", true)
    )
        private set

    var enableMangaSources: Boolean by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getBoolean("enable_manga_sources", true)
    )
        private set

    var nsfwIncognitoMode: Boolean by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getBoolean("nsfw_incognito_mode", true)
    )
        private set

    var nsfwExcludeFromStats: Boolean by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getBoolean("nsfw_exclude_stats", true)
    )
        private set

    var nsfwBlurCovers: Boolean by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getBoolean("nsfw_blur_covers", true)
    )
        private set

    var nsfwSeparateCategory: Boolean by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getBoolean("nsfw_separate_cat", true)
    )
        private set

    fun isMangaNsfw(isNsfw: Boolean, tags: List<String> = emptyList(), contentType: String? = null): Boolean {
        if (isNsfw) return true
        if (contentType?.contains("HENTAI", ignoreCase = true) == true || contentType?.contains("ADULT", ignoreCase = true) == true) return true
        val keywords = setOf("nsfw", "18+", "adult", "hentai", "ecchi", "erotica", "smut", "gore", "sexual", "yaoi", "yuri", "mature", "r18", "r-18", "doujinshi", "pornhwa", "webtoon 18+", "uncensored", "manhwa 18+")
        return tags.any { tag ->
            val clean = tag.trim().lowercase()
            keywords.any { k -> clean == k || clean.contains(k) }
        }
    }

    fun updateNsfwIncognito(enabled: Boolean) {
        nsfwIncognitoMode = enabled
        SettingsStorage.setBoolean("nsfw_incognito_mode", enabled)
        if (enabled) {
            HistoryManager.clearNsfwHistory()
            StatisticsManager.clearNsfwStats()
        }
    }

    fun updateNsfwExcludeStats(enabled: Boolean) {
        nsfwExcludeFromStats = enabled
        SettingsStorage.setBoolean("nsfw_exclude_stats", enabled)
    }

    fun updateNsfwBlurCovers(enabled: Boolean) {
        nsfwBlurCovers = enabled
        SettingsStorage.setBoolean("nsfw_blur_covers", enabled)
    }

    fun updateNsfwSeparateCategory(enabled: Boolean) {
        nsfwSeparateCategory = enabled
        SettingsStorage.setBoolean("nsfw_separate_cat", enabled)
    }

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

    fun updateEnableKotatsuSources(enabled: Boolean) {
        if (!enabled && !enableMangaSources) {
            enableMangaSources = true
            SettingsStorage.setBoolean("enable_manga_sources", true)
        }
        enableKotatsuSources = enabled
        SettingsStorage.setBoolean("enable_kotatsu_sources", enabled)
        MangaDataCache.cachedSources = null
        MangaDataCache.cachedPopularManga.clear()
    }

    fun updateEnableMangaSources(enabled: Boolean) {
        if (!enabled && !enableKotatsuSources) {
            enableKotatsuSources = true
            SettingsStorage.setBoolean("enable_kotatsu_sources", true)
        }
        enableMangaSources = enabled
        SettingsStorage.setBoolean("enable_manga_sources", enabled)
        MangaDataCache.cachedSources = null
        MangaDataCache.cachedPopularManga.clear()
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

    var readerKeepScreenOn: Boolean by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getBoolean("reader_keep_screen_on", true)
    )
        private set

    var readerCropBorders: Boolean by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getBoolean("reader_crop_borders", false)
    )
        private set

    var readerVolumeKeysNavigation: Boolean by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getBoolean("reader_volume_keys", false)
    )
        private set

    var readerDoubleTapZoom: Float by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getString("reader_double_tap_zoom", "1.5").toFloatOrNull() ?: 1.5f
    )
        private set

    var readerPanSensitivity: Float by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getString("reader_pan_sensitivity", "1.5").toFloatOrNull() ?: 1.5f
    )
        private set

    // --- Home Screen Customization Settings ---
    var homeDefaultCategory: String by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getString("home_default_category", "POPULAR")
    )
        private set

    var homeLayoutMode: com.abht.manga_dt.ui.models.LayoutMode by androidx.compose.runtime.mutableStateOf(
        runCatching {
            com.abht.manga_dt.ui.models.LayoutMode.valueOf(
                SettingsStorage.getString("home_layout_mode", com.abht.manga_dt.ui.models.LayoutMode.COMFORTABLE.name)
            )
        }.getOrDefault(com.abht.manga_dt.ui.models.LayoutMode.COMFORTABLE)
    )
        private set

    var homeGridColumns: Int by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getInt("home_grid_columns", 0)
    )
        private set

    var homeShowContinueReading: Boolean by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getBoolean("home_show_continue_reading", true)
    )
        private set

    var homeContinueReadingCount: Int by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getInt("home_continue_reading_count", 8)
    )
        private set

    var homeShowFilterPills: Boolean by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getBoolean("home_show_filter_pills", true)
    )
        private set

    var homeShowFavoritesTab: Boolean by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getBoolean("home_show_favorites_tab", true)
    )
        private set

    var homeCardCornersDp: Int by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getInt("home_card_corners_dp", 16)
    )
        private set

    var homeShowSourceBadge: Boolean by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getBoolean("home_show_source_badge", true)
    )
        private set

    var homeShowRatingBadge: Boolean by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getBoolean("home_show_rating_badge", true)
    )
        private set

    // --- Kotatsu Appearance & Main Screen Settings ---
    var defaultAppTab: String by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getString("default_app_tab", "LAST_USED")
    )
        private set

    var searchSuggestionsList: Set<String> by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getStringSet("search_suggestions_list").ifEmpty {
            setOf("SOURCES", "GENRES", "RECENT_QUERIES", "AUTHORS", "SUGGESTIONS", "RECENT_SOURCES", "MANGA")
        }
    )
        private set

    var mainScreenSections: Set<String> by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getStringSet("main_screen_sections").ifEmpty {
            setOf("HISTORY", "FAVORITES", "EXPLORE", "FEED", "SUGGESTIONS", "ON_DEVICE")
        }
    )
        private set

    var showFloatingContinueButton: Boolean by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getBoolean("show_floating_continue_btn", true)
    )
        private set

    var showNavBarLabels: Boolean by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getBoolean("show_nav_bar_labels", true)
    )
        private set

    var floatingNavBar: Boolean by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getBoolean("floating_nav_bar", false)
    )
        private set

    var pinNavigationUi: Boolean by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getBoolean("pin_navigation_ui", true)
    )
        private set

    var exitConfirmation: Boolean by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getBoolean("exit_confirmation", true)
    )
        private set

    fun updateDefaultAppTab(tab: String) {
        defaultAppTab = tab
        SettingsStorage.setString("default_app_tab", tab)
    }

    fun updateSearchSuggestionsList(list: Set<String>) {
        searchSuggestionsList = list
        SettingsStorage.setStringSet("search_suggestions_list", list)
    }

    fun updateMainScreenSections(sections: Set<String>) {
        mainScreenSections = sections
        SettingsStorage.setStringSet("main_screen_sections", sections)
    }

    fun updateShowFloatingContinueButton(show: Boolean) {
        showFloatingContinueButton = show
        SettingsStorage.setBoolean("show_floating_continue_btn", show)
    }

    fun updateShowNavBarLabels(show: Boolean) {
        showNavBarLabels = show
        SettingsStorage.setBoolean("show_nav_bar_labels", show)
    }

    fun updateFloatingNavBar(floating: Boolean) {
        floatingNavBar = floating
        SettingsStorage.setBoolean("floating_nav_bar", floating)
    }

    fun updatePinNavigationUi(pin: Boolean) {
        pinNavigationUi = pin
        SettingsStorage.setBoolean("pin_navigation_ui", pin)
    }

    fun updateExitConfirmation(exit: Boolean) {
        exitConfirmation = exit
        SettingsStorage.setBoolean("exit_confirmation", exit)
    }

    fun updateHomeDefaultCategory(category: String) {
        homeDefaultCategory = category
        SettingsStorage.setString("home_default_category", category)
    }

    fun updateHomeLayoutMode(mode: com.abht.manga_dt.ui.models.LayoutMode) {
        homeLayoutMode = mode
        SettingsStorage.setString("home_layout_mode", mode.name)
    }

    fun updateHomeGridColumns(columns: Int) {
        homeGridColumns = columns.coerceIn(0, 8)
        SettingsStorage.setInt("home_grid_columns", homeGridColumns)
    }

    fun updateHomeShowContinueReading(show: Boolean) {
        homeShowContinueReading = show
        SettingsStorage.setBoolean("home_show_continue_reading", show)
    }

    fun updateHomeContinueReadingCount(count: Int) {
        homeContinueReadingCount = count
        SettingsStorage.setInt("home_continue_reading_count", count)
    }

    fun updateHomeShowFilterPills(show: Boolean) {
        homeShowFilterPills = show
        SettingsStorage.setBoolean("home_show_filter_pills", show)
    }

    fun updateHomeShowFavoritesTab(show: Boolean) {
        homeShowFavoritesTab = show
        SettingsStorage.setBoolean("home_show_favorites_tab", show)
    }

    fun updateHomeCardCornersDp(dp: Int) {
        homeCardCornersDp = dp
        SettingsStorage.setInt("home_card_corners_dp", dp)
    }

    fun updateHomeShowSourceBadge(show: Boolean) {
        homeShowSourceBadge = show
        SettingsStorage.setBoolean("home_show_source_badge", show)
    }

    fun updateHomeShowRatingBadge(show: Boolean) {
        homeShowRatingBadge = show
        SettingsStorage.setBoolean("home_show_rating_badge", show)
    }

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

    fun updateReaderKeepScreenOn(keep: Boolean) {
        readerKeepScreenOn = keep
        SettingsStorage.setBoolean("reader_keep_screen_on", keep)
    }

    fun updateReaderCropBorders(crop: Boolean) {
        readerCropBorders = crop
        SettingsStorage.setBoolean("reader_crop_borders", crop)
    }

    fun updateReaderVolumeKeysNavigation(enabled: Boolean) {
        readerVolumeKeysNavigation = enabled
        SettingsStorage.setBoolean("reader_volume_keys", enabled)
    }

    fun updateReaderDoubleTapZoom(zoom: Float) {
        readerDoubleTapZoom = zoom
        SettingsStorage.setString("reader_double_tap_zoom", zoom.toString())
    }

    fun updateReaderPanSensitivity(sensitivity: Float) {
        readerPanSensitivity = sensitivity
        SettingsStorage.setString("reader_pan_sensitivity", sensitivity.toString())
    }

    var localMangaDirectories: Set<String> by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getStringSet("local_manga_dirs").ifEmpty {
            setOf("Internal storage/Manga", "SD card/Manga")
        }
    )

    var downloadsFolder: String by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getString("downloads_folder", "Internal shared storage")
    )

    var preferredDownloadFormat: DownloadFormat by androidx.compose.runtime.mutableStateOf(
        runCatching {
            DownloadFormat.valueOf(SettingsStorage.getString("preferred_download_format", DownloadFormat.AUTOMATIC.name))
        }.getOrDefault(DownloadFormat.AUTOMATIC)
    )

    var cellularDownloadPolicy: CellularDownloadPolicy by androidx.compose.runtime.mutableStateOf(
        runCatching {
            CellularDownloadPolicy.valueOf(SettingsStorage.getString("cellular_download_policy", CellularDownloadPolicy.ALLOW_ALWAYS.name))
        }.getOrDefault(CellularDownloadPolicy.ALLOW_ALWAYS)
    )

    var disableBatteryOptimization: Boolean by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getBoolean("disable_battery_optimization", false)
    )

    var defaultPageSaveDir: String by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getString("default_page_save_dir", "Not set")
    )

    var askPageSaveDirEveryTime: Boolean by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getBoolean("ask_page_save_dir_every_time", true)
    )

    var autoDeleteReadChapters: Boolean by androidx.compose.runtime.mutableStateOf(
        SettingsStorage.getBoolean("auto_delete_read_chapters", false)
    )

    fun updateAutoDeleteReadChapters(autoDelete: Boolean) {
        autoDeleteReadChapters = autoDelete
        SettingsStorage.setBoolean("auto_delete_read_chapters", autoDelete)
    }

    fun updateLocalMangaDirectories(dirs: Set<String>) {
        localMangaDirectories = dirs
        SettingsStorage.setStringSet("local_manga_dirs", dirs)
    }

    fun updateDownloadsFolder(folder: String) {
        downloadsFolder = folder
        SettingsStorage.setString("downloads_folder", folder)
    }

    fun updatePreferredDownloadFormat(format: DownloadFormat) {
        preferredDownloadFormat = format
        SettingsStorage.setString("preferred_download_format", format.name)
    }

    fun updateCellularDownloadPolicy(policy: CellularDownloadPolicy) {
        cellularDownloadPolicy = policy
        SettingsStorage.setString("cellular_download_policy", policy.name)
    }

    fun updateDisableBatteryOptimization(disable: Boolean) {
        disableBatteryOptimization = disable
        SettingsStorage.setBoolean("disable_battery_optimization", disable)
    }

    fun updateDefaultPageSaveDir(dir: String) {
        defaultPageSaveDir = dir
        SettingsStorage.setString("default_page_save_dir", dir)
    }

    fun updateAskPageSaveDirEveryTime(ask: Boolean) {
        askPageSaveDirEveryTime = ask
        SettingsStorage.setBoolean("ask_page_save_dir_every_time", ask)
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
        themePreset = runCatching {
            ThemePreset.valueOf(SettingsStorage.getString("theme_preset", ThemePreset.DYNAMIC.name))
        }.getOrDefault(ThemePreset.DYNAMIC)
        appLanguage = runCatching {
            AppLanguage.valueOf(SettingsStorage.getString("app_language", AppLanguage.ARABIC.name))
        }.getOrDefault(AppLanguage.ARABIC)
        themeMode = runCatching {
            ThemeMode.valueOf(SettingsStorage.getString("theme_mode", ThemeMode.SYSTEM.name))
        }.getOrDefault(ThemeMode.SYSTEM)
        amoledBlack = SettingsStorage.getBoolean("amoled_black", false)
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
        readerKeepScreenOn = SettingsStorage.getBoolean("reader_keep_screen_on", true)
        readerCropBorders = SettingsStorage.getBoolean("reader_crop_borders", false)
        readerVolumeKeysNavigation = SettingsStorage.getBoolean("reader_volume_keys", false)
        readerDoubleTapZoom = SettingsStorage.getString("reader_double_tap_zoom", "1.5").toFloatOrNull() ?: 1.5f
        readerPanSensitivity = SettingsStorage.getString("reader_pan_sensitivity", "1.5").toFloatOrNull() ?: 1.5f
        homeDefaultCategory = SettingsStorage.getString("home_default_category", "POPULAR")
        homeLayoutMode = runCatching {
            com.abht.manga_dt.ui.models.LayoutMode.valueOf(
                SettingsStorage.getString("home_layout_mode", com.abht.manga_dt.ui.models.LayoutMode.COMFORTABLE.name)
            )
        }.getOrDefault(com.abht.manga_dt.ui.models.LayoutMode.COMFORTABLE)
        homeGridColumns = SettingsStorage.getInt("home_grid_columns", 0)
        homeShowContinueReading = SettingsStorage.getBoolean("home_show_continue_reading", true)
        homeContinueReadingCount = SettingsStorage.getInt("home_continue_reading_count", 8)
        homeShowFilterPills = SettingsStorage.getBoolean("home_show_filter_pills", true)
        homeShowFavoritesTab = SettingsStorage.getBoolean("home_show_favorites_tab", true)
        homeCardCornersDp = SettingsStorage.getInt("home_card_corners_dp", 16)
        homeShowSourceBadge = SettingsStorage.getBoolean("home_show_source_badge", true)
        homeShowRatingBadge = SettingsStorage.getBoolean("home_show_rating_badge", true)
        defaultAppTab = SettingsStorage.getString("default_app_tab", "LAST_USED")
        searchSuggestionsList = SettingsStorage.getStringSet("search_suggestions_list").ifEmpty {
            setOf("SOURCES", "GENRES", "RECENT_QUERIES", "AUTHORS", "SUGGESTIONS", "RECENT_SOURCES", "MANGA")
        }
        mainScreenSections = SettingsStorage.getStringSet("main_screen_sections").ifEmpty {
            setOf("HISTORY", "FAVORITES", "EXPLORE", "FEED", "SUGGESTIONS", "ON_DEVICE")
        }
        showFloatingContinueButton = SettingsStorage.getBoolean("show_floating_continue_btn", true)
        showNavBarLabels = SettingsStorage.getBoolean("show_nav_bar_labels", true)
        floatingNavBar = SettingsStorage.getBoolean("floating_nav_bar", false)
        pinNavigationUi = SettingsStorage.getBoolean("pin_navigation_ui", true)
        isNsfwAllowed = SettingsStorage.getBoolean("is_nsfw_allowed", false)
        enableKotatsuSources = SettingsStorage.getBoolean("enable_kotatsu_sources", true)
        enableMangaSources = SettingsStorage.getBoolean("enable_manga_sources", true)
        if (!enableKotatsuSources && !enableMangaSources) {
            enableKotatsuSources = true
            enableMangaSources = true
        }
        nsfwIncognitoMode = SettingsStorage.getBoolean("nsfw_incognito_mode", true)
        nsfwExcludeFromStats = SettingsStorage.getBoolean("nsfw_exclude_stats", true)
        nsfwBlurCovers = SettingsStorage.getBoolean("nsfw_blur_covers", true)
        nsfwSeparateCategory = SettingsStorage.getBoolean("nsfw_separate_cat", true)
        localMangaDirectories = SettingsStorage.getStringSet("local_manga_dirs").ifEmpty {
            setOf("Internal storage/Manga", "SD card/Manga")
        }
        downloadsFolder = SettingsStorage.getString("downloads_folder", "Internal shared storage")
        preferredDownloadFormat = runCatching {
            DownloadFormat.valueOf(SettingsStorage.getString("preferred_download_format", DownloadFormat.AUTOMATIC.name))
        }.getOrDefault(DownloadFormat.AUTOMATIC)
        cellularDownloadPolicy = runCatching {
            CellularDownloadPolicy.valueOf(SettingsStorage.getString("cellular_download_policy", CellularDownloadPolicy.ALLOW_ALWAYS.name))
        }.getOrDefault(CellularDownloadPolicy.ALLOW_ALWAYS)
        disableBatteryOptimization = SettingsStorage.getBoolean("disable_battery_optimization", false)
        defaultPageSaveDir = SettingsStorage.getString("default_page_save_dir", "Not set")
        askPageSaveDirEveryTime = SettingsStorage.getBoolean("ask_page_save_dir_every_time", true)
        autoDeleteReadChapters = SettingsStorage.getBoolean("auto_delete_read_chapters", false)
    }
}

enum class DownloadFormat(val displayNameEn: String, val displayNameAr: String) {
    AUTOMATIC("Automatic", "تلقائي"),
    CBZ("CBZ Archive", "أرشيف CBZ"),
    FOLDER("Folder with Images", "مجلد صور"),
    ZIP("ZIP Archive", "أرشيف ZIP"),
    PDF("PDF Document", "مستند PDF");

    fun getDisplayName(isArabic: Boolean): String = if (isArabic) displayNameAr else displayNameEn
}

enum class CellularDownloadPolicy(val displayNameEn: String, val displayNameAr: String) {
    ALLOW_ALWAYS("Allow always", "السماح دائماً"),
    WIFI_ONLY("Wi-Fi only", "عبر Wi-Fi فقط"),
    ASK_EVERY_TIME("Ask every time", "السؤال في كل مرة");

    fun getDisplayName(isArabic: Boolean): String = if (isArabic) displayNameAr else displayNameEn
}

object HistoryManager {
    fun isEntryNsfw(
        mangaTitle: String,
        mangaUrl: String = "",
        sourceId: String = "",
        isNsfw: Boolean = false,
        tags: List<String> = emptyList()
    ): Boolean {
        if (isNsfw) return true
        if (AppSettings.isMangaNsfw(isNsfw, tags)) return true

        // 1. Check MangaDataCache for cached manga details
        val cached = MangaDataCache.cachedMangaDetails.values.firstOrNull { 
            (mangaUrl.isNotBlank() && it.url == mangaUrl) || it.title.equals(mangaTitle, ignoreCase = true) 
        }
        if (cached != null) {
            if (cached.isNsfw) return true
            if (AppSettings.isMangaNsfw(cached.isNsfw, cached.tags)) return true
        }

        // 2. Check MangaDataCache popular list
        MangaDataCache.cachedPopularManga.values.forEach { list ->
            val found = list.firstOrNull { 
                (mangaUrl.isNotBlank() && it.url == mangaUrl) || it.title.equals(mangaTitle, ignoreCase = true) 
            }
            if (found != null) {
                if (found.isNsfw) return true
                if (AppSettings.isMangaNsfw(found.isNsfw, found.tags)) return true
            }
        }

        // 3. Check MangaSourceManager cachedSources for adult content type
        val src = sourceId.trim().lowercase()
        if (src.isNotEmpty()) {
            val sourceObj = MangaDataCache.cachedSources?.firstOrNull { it.id.equals(src, ignoreCase = true) }
            if (sourceObj != null && sourceObj.contentType.equals("HENTAI", ignoreCase = true)) {
                return true
            }
            if (src.contains("hentai") || src.contains("adult") || src.contains("pornhwa") || src.contains("18+") || src.contains("nhentai") || src.contains("hitomi") || src.contains("asmhentai")) {
                return true
            }
        }

        // 4. Check Manga Title & URL for adult signals
        val title = mangaTitle.trim().lowercase()
        val url = mangaUrl.trim().lowercase()
        val nsfwKeywords = listOf("hentai", "18+", "nsfw", "erotica", "smut", "uncensored", "ecchi", "pornhwa", "r18", "r-18", "doujinshi")
        if (nsfwKeywords.any { title.contains(it) || url.contains(it) }) {
            return true
        }

        return false
    }

    var historyEntries: List<com.abht.manga_dt.models.HistoryEntry> by androidx.compose.runtime.mutableStateOf(loadHistory())
        private set

    private fun loadHistory(): List<com.abht.manga_dt.models.HistoryEntry> {
        val raw = SettingsStorage.getString("reading_history_json", "")
        if (raw.isBlank()) return emptyList()
        val allEntries = raw.split(";;;").mapNotNull { entryStr ->
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
        return if (AppSettings.nsfwIncognitoMode) {
            allEntries.filterNot { isEntryNsfw(it.mangaTitle, it.mangaUrl, it.sourceId) }
        } else {
            allEntries
        }
    }

    private fun saveHistory(list: List<com.abht.manga_dt.models.HistoryEntry>) {
        val raw = list.joinToString(";;;") {
            "${it.mangaTitle}|||${it.mangaCover}|||${it.sourceId}|||${it.chapterTitle}|||${it.chapterUrl}|||${it.mangaUrl}|||${it.timestamp}|||${it.lastPage}|||${it.totalPages}|||${it.scrollOffset}"
        }
        SettingsStorage.setString("reading_history_json", raw)
        historyEntries = list
    }

    var isIncognitoSessionActive: Boolean by androidx.compose.runtime.mutableStateOf(false)
    var incognitoSessionMangaUrl: String? by androidx.compose.runtime.mutableStateOf(null)

    fun startIncognitoSession(mangaUrl: String) {
        isIncognitoSessionActive = true
        incognitoSessionMangaUrl = mangaUrl
    }

    fun endIncognitoSession() {
        isIncognitoSessionActive = false
        incognitoSessionMangaUrl = null
    }

    fun removeHistoryForManga(mangaUrl: String, mangaTitle: String = "") {
        val updated = historyEntries.filterNot { 
            (mangaUrl.isNotBlank() && it.mangaUrl == mangaUrl) || 
            (mangaTitle.isNotBlank() && it.mangaTitle.equals(mangaTitle, ignoreCase = true)) 
        }
        saveHistory(updated)
    }

    fun addOrUpdateHistory(
        entry: com.abht.manga_dt.models.HistoryEntry,
        isNsfw: Boolean = false,
        tags: List<String> = emptyList()
    ) {
        if (isIncognitoSessionActive && (incognitoSessionMangaUrl.isNullOrBlank() || incognitoSessionMangaUrl == entry.mangaUrl)) {
            return
        }
        if (AppSettings.nsfwIncognitoMode && isEntryNsfw(entry.mangaTitle, entry.mangaUrl, entry.sourceId, isNsfw, tags)) {
            return
        }
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

        // Save manga details to offline cache if available
        val cached = MangaDataCache.cachedMangaDetails["${entry.sourceId}::${entry.mangaUrl}"]
            ?: MangaDataCache.cachedMangaDetails.values.firstOrNull { 
                (entry.mangaUrl.isNotBlank() && it.url == entry.mangaUrl) || it.title.equals(entry.mangaTitle, ignoreCase = true) 
            }
        if (cached != null) {
            OfflineMangaManager.saveManga(cached)
        }
    }

    fun updatePageProgress(
        mangaTitle: String,
        chapterUrl: String,
        page: Int,
        totalPages: Int,
        scrollOffset: Int = 0,
        isNsfw: Boolean = false,
        tags: List<String> = emptyList()
    ) {
        if (page <= 0) return
        if (isIncognitoSessionActive) {
            return
        }
        if (AppSettings.nsfwIncognitoMode && isEntryNsfw(mangaTitle, "", "", isNsfw, tags)) {
            return
        }
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

    fun clearNsfwHistory() {
        val raw = SettingsStorage.getString("reading_history_json", "")
        if (raw.isBlank()) {
            historyEntries = emptyList()
            return
        }
        val allEntries = raw.split(";;;").mapNotNull { entryStr ->
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
        val cleaned = allEntries.filterNot { isEntryNsfw(it.mangaTitle, it.mangaUrl, it.sourceId) }
        saveHistory(cleaned)
    }

    fun batchMergeHistory(entries: List<com.abht.manga_dt.models.HistoryEntry>): Int {
        if (entries.isEmpty()) return 0
        val map = LinkedHashMap<String, com.abht.manga_dt.models.HistoryEntry>()
        for (e in historyEntries) {
            val key = if (e.mangaUrl.isNotBlank()) e.mangaUrl.lowercase() else e.mangaTitle.trim().lowercase()
            map[key] = e
        }

        var mergedCount = 0
        for (newEntry in entries) {
            if (newEntry.mangaTitle.isBlank() || newEntry.chapterUrl.isBlank()) continue
            val key = if (newEntry.mangaUrl.isNotBlank()) newEntry.mangaUrl.lowercase() else newEntry.mangaTitle.trim().lowercase()
            val existing = map[key]
            if (existing == null) {
                map[key] = newEntry
                mergedCount++
            } else if (newEntry.timestamp >= existing.timestamp) {
                map[key] = newEntry
                mergedCount++
            }
        }

        val sortedList = map.values.sortedByDescending { it.timestamp }.take(500)
        saveHistory(sortedList)
        return mergedCount
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

    fun batchMergeLibrary(items: List<com.abht.manga_dt.models.LibraryManga>, incomingCategories: List<String> = emptyList()): Int {
        if (items.isEmpty() && incomingCategories.isEmpty()) return 0

        // 1. Merge categories
        if (incomingCategories.isNotEmpty()) {
            val currentCats = categories.toMutableList()
            var catsChanged = false
            for (cat in incomingCategories) {
                val trimmed = cat.trim()
                if (trimmed.isNotBlank() && !currentCats.contains(trimmed)) {
                    currentCats.add(trimmed)
                    catsChanged = true
                }
            }
            if (catsChanged) {
                categories = currentCats
                SettingsStorage.setString("library_categories", currentCats.joinToString("|||"))
            }
        }

        if (items.isEmpty()) return 0

        // 2. Merge items by Key
        val map = LinkedHashMap<String, com.abht.manga_dt.models.LibraryManga>()
        for (item in libraryItems) {
            val key = if (item.mangaUrl.isNotBlank()) item.mangaUrl.lowercase() else item.title.trim().lowercase()
            map[key] = item
        }

        var mergedCount = 0
        for (newItem in items) {
            if (newItem.title.isBlank()) continue
            val key = if (newItem.mangaUrl.isNotBlank()) newItem.mangaUrl.lowercase() else newItem.title.trim().lowercase()
            val existing = map[key]
            if (existing == null) {
                map[key] = newItem
                mergedCount++
            } else {
                val newerRead = newItem.lastReadAt > existing.lastReadAt && newItem.lastReadChapterUrl.isNotBlank()
                val bestCover = existing.thumbnailUrl.ifBlank { newItem.thumbnailUrl }
                val bestAuthor = existing.author ?: newItem.author
                val bestTotal = maxOf(existing.totalChapters, newItem.totalChapters)
                val mergedItem = existing.copy(
                    thumbnailUrl = bestCover,
                    author = bestAuthor,
                    totalChapters = bestTotal,
                    lastReadAt = if (newerRead) newItem.lastReadAt else existing.lastReadAt,
                    lastReadChapterTitle = if (newerRead) newItem.lastReadChapterTitle else existing.lastReadChapterTitle,
                    lastReadChapterUrl = if (newerRead) newItem.lastReadChapterUrl else existing.lastReadChapterUrl
                )
                if (newerRead) mergedCount++
                map[key] = mergedItem
            }
        }

        val mergedList = map.values.toList()
        saveLibrary(mergedList)
        return mergedCount
    }
}



