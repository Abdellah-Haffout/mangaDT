package com.abht.manga_dt.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    SYSTEM("system", "Follow System", "تلقائي (حسب لغة الجهاز)"),
    ARABIC("ar", "Arabic", "العربية"),
    ENGLISH("en", "English", "English");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: SYSTEM
        }
    }
}

expect fun isSystemLanguageArabic(): Boolean

object Strings {
    val current: AppStrings
        @Composable
        get() = when (AppSettings.appLanguage) {
            AppLanguage.ARABIC -> ArabicStrings
            AppLanguage.ENGLISH -> EnglishStrings
            AppLanguage.SYSTEM -> if (isSystemLanguageArabic()) ArabicStrings else EnglishStrings
        }
}

interface AppStrings {
    val appName: String
    val home: String
    val library: String
    val updates: String
    val browse: String
    val settings: String
    val favorites: String
    val history: String
    val popular: String
    val latest: String
    val searchManga: String
    val searchLocal: String
    val searchSources: String
    val onlineMangaMode: String
    val localLibraryMode: String
    val sourcesMode: String
    val sources: String
    val genre: String
    val language: String
    val all: String
    val continueReading: String
    val startReading: String
    val resumeReading: String
    val inLibrary: String
    val addToLibrary: String
    val add: String
    val chapters: String
    val noMangaFound: String
    val retry: String
    val statusOngoing: String
    val statusCompleted: String
    val statusOnHold: String
    val statusDropped: String
    val appearance: String
    val colorScheme: String
    val theme: String
    val themeDark: String
    val themeLight: String
    val themeSystem: String
    val themeSystemDesc: String
    val blackAmoled: String
    val blackAmoledDesc: String
    val selectThemeTitle: String
    val darkTheme: String
    val nsfwContent: String
    val nsfwContentDesc: String
    val appLanguage: String
    val selectLanguageTitle: String
    val clearCache: String
    val clearCacheSuccess: String
    val expandSidebar: String
    val collapseSidebar: String
    val selectSourceTitle: String
    val selectGenreTitle: String
    val close: String
    val clear: String
    val ok: String
    val cancel: String
    val notificationsTitle: String
    val noNotifications: String
    val notificationsSubtitle: String
    val userDefaultName: String
    val userSubtitle: String
    val typeToSearchLocal: String
    val typeToSearchOnline: String
    val loadingManga: String
    val defaultBrowseLayout: String
    val layoutList: String
    val layoutComfortable: String
    val layoutCompact: String
    val selectLayoutTitle: String
    val gridSizeAndLayout: String
    val gridColumnsCount: String
    val gridColumnsAuto: String
    fun gridColumnsCountLabel(count: Int): String
    val offlineAvailable: String
    val savedOffline: String
    val defaultReadingMode: String
    val readingModeWebtoon: String
    val readingModeVerticalPaged: String
    val readingModeLTR: String
    val readingModeRTL: String
    val selectReadingModeTitle: String
    val readerSettings: String
    val readerMode: String
    val readerBackground: String
    val readerScale: String
    val readerScaleFitWidth: String
    val readerScaleFitHeight: String
    val readerScaleFitScreen: String
    val readerScaleOriginal: String
    val readerBgBlack: String
    val readerBgDarkGray: String
    val readerBgWhite: String
    val previousChapter: String
    val nextChapter: String
    val chapterList: String
    val searchChapters: String
    val endOfChapter: String
    val finishedChapter: String
    val goToNextChapter: String
    val goToPreviousChapter: String
    val noMoreChapters: String
    val goToPage: String
    val pageIndicator: String
    val pageIndicatorDesc: String
    val pageFailedToLoad: String
    val tapToRetry: String
    val keepScreenOn: String
    val keepScreenOnDesc: String
    val cropBorders: String
    val cropBordersDesc: String
    val readerDoubleTapZoom: String
    val readerDoubleTapZoomDesc: String
    val readerPanSensitivity: String
    val readerPanSensitivityDesc: String
    val pinnedSourcesTitle: String
    val noPinnedSources: String
    val clearAllPinned: String
    val versionText: String
    val autoConfigured: String
    val storageUsageDesc: String
    val captchaRequired: String
    val solveCaptcha: String
    val captchaSolved: String
    val captchaHelpText: String
    val iHaveSolvedIt: String

    // Profile & Statistics strings
    val profile: String
    val statistics: String
    val profileAndStats: String
    val totalReadingTime: String
    val chaptersRead: String
    val read: String
    val pagesRead: String
    val mangaRead: String
    val readingStreak: String
    val streakDays: String
    val bestStreak: String
    val dailyAverage: String
    val averageTimePerChapter: String
    val rank: String
    val rankNovice: String
    val rankReader: String
    val rankEnthusiast: String
    val rankOtaku: String
    val rankLegend: String
    val topGenres: String
    val weeklyActivity: String
    val mangaStatisticsTitle: String
    val timeSpent: String
    val lastRead: String
    val completedChapters: String
    val editProfile: String
    val username: String
    val bio: String
    val save: String
    val privacyLocalNotice: String
    val noStatsYet: String
    val clearStats: String
    val clearStatsConfirm: String

    // Profile Experience strings
    val overviewAndActivity: String
    val detailedAnalytics: String
    val currentlyReading: String
    val myFavorites: String
    val planToRead: String
    val tasteDna: String
    val readingVibe: String
    val noFavoritesYet: String
    val noPlanToReadYet: String
    val noCurrentReading: String

    // Local Network Sync strings
    val localSync: String
    val localSyncDesc: String
    val myDevice: String
    val deviceName: String
    val serverStatus: String
    val serverRunning: String
    val serverStopped: String
    val localIp: String
    val scanDevices: String
    val scanning: String
    val noDevicesFound: String
    val discoveredDevices: String
    val syncNow: String
    val manualConnect: String
    val enterTargetIp: String
    val connectAndSync: String
    val syncOptions: String
    val syncLibrary: String
    val syncHistory: String
    val syncStats: String
    val syncSettings: String
    val syncDownloads: String
    val syncDownloadsDesc: String
    val syncingDownloadsProgress: (current: Int, total: Int, chapterName: String) -> String
    val syncSuccess: String
    val syncFailed: String
    val syncing: String
    val lastSync: String

    // Backup & Restore strings
    val createBackup: String
    val createBackupDesc: String
    val restoreBackup: String
    val restoreBackupDesc: String
    val backupCreatedSuccess: String
    val backupRestoredSuccess: String
    val backupSavedAt: String
    val restoreModeTitle: String
    val restoreModeMerge: String
    val restoreModeMergeDesc: String
    val restoreModeOverwrite: String
    val restoreModeOverwriteDesc: String
    val backupPreviewTitle: String
    val backupDate: String
    val backupLibraryItems: String
    val backupHistoryEntries: String
    val backupStatsEntries: String
    val localBackupsList: String
    val noLocalBackupsFound: String
    val selectFileFromDevice: String
    val selectFileFromDeviceDesc: String
    val pasteJsonBackup: String
    val pasteJsonPlaceholder: String
    val invalidBackupFormat: String
    val deleteBackupConfirm: String

    // Deep Analytics Strings
    val deepAnalyticsTitle: String
    val deepAnalyticsSubtitle: String
    val readingPace: String
    val secondsPerPage: String
    val minutesPerChapter: String
    val longestSession: String
    val peakReadingTime: String
    val morningReader: String
    val afternoonReader: String
    val nightReader: String
    val allMangaDetailed: String
    val sortByTime: String
    val sortByChapters: String
    val sortByPages: String
    val sortByLastRead: String
    val openDeepAnalytics: String

    // Home Customization Strings
    val homeCustomizationTitle: String
    val homeCustomizationDesc: String
    val homeDefaultSectionTitle: String
    val homeDefaultSectionDesc: String
    val homeCardsLayoutTitle: String
    val homeCardsLayoutDesc: String
    val homeContinueReadingBarTitle: String
    val homeContinueReadingBarDesc: String
    val homeContinueReadingLimitTitle: String
    val homeQuickFilterPillsTitle: String
    val homeQuickFilterPillsDesc: String
    val homeShowFavoritesTabTitle: String
    val homeShowFavoritesTabDesc: String
    val homeCardCornersTitle: String
    val homeCardCornersDesc: String
    val homeShowSourceBadgeTitle: String
    val homeShowSourceBadgeDesc: String
    val homeShowRatingBadgeTitle: String
    val homeShowRatingBadgeDesc: String
    val homeCornerSmooth: String
    val homeCornerMedium: String
    val homeCornerSharp: String

    // Kotatsu Exact Appearance & Main Screen Settings
    val defaultTabTitle: String
    val defaultTabLastUsed: String
    val mainScreenSectionHeader: String
    val searchSuggestionsTitle: String
    val searchSuggestionsDesc: String
    val mainScreenSectionsTitle: String
    val mainScreenSectionsDesc: String
    val showFloatingContinueBtnTitle: String
    val showFloatingContinueBtnDesc: String
    val showLabelsInNavBarTitle: String
    val floatingNavBarTitle: String
    val floatingNavBarDesc: String
    val pinNavigationUiTitle: String
    val pinNavigationUiDesc: String
    val exitConfirmationTitle: String
    val exitConfirmationDesc: String
    val pressBackAgainToExit: String
    val exploreMoreManga: String
    val browseAllManga: String
    val exploreSources: String

    // Manga Details Statistics & Recommendations
    val readingProgressTitle: String
    val similarMangaTitle: String
    val loadingGenreManga: String
    val noMangaInGenre: String
    val readCountLabel: String

    // Download Manager Strings
    val downloads: String
    val downloadChapter: String
    val downloaded: String
    val downloading: String
    val downloadQueued: String
    val pauseAll: String
    val resumeAll: String
    val cancelAll: String
    val clearCompleted: String
    val deleteDownload: String
    val deleteDownloadConfirm: String
    val downloadNextChapters: String
    val downloadAllUnread: String
    val downloadAllChapters: String
    val storageUsed: String
    val noActiveDownloads: String
    val noDownloadedManga: String
    val offlineReadingAvailable: String
    val downloadQueueTab: String
    val downloadedMangaTab: String
    val batchDownloadTitle: String

    // Kotatsu Downloads Settings Strings
    val localMangaDirectoriesTitle: String
    val localMangaDirectoriesDesc: String
    val downloadsFolderTitle: String
    val internalSharedStorage: String
    val preferredDownloadFormatTitle: String
    val downloadFormatAuto: String
    val downloadFormatCbz: String
    val downloadFormatFolder: String
    val downloadFormatZip: String
    val downloadFormatPdf: String
    val downloadingOverCellularTitle: String
    val cellularAllowAlways: String
    val cellularWifiOnly: String
    val cellularAskEveryTime: String
    val downloadSlowdownInfo: String
    val disableBatteryOptimizationTitle: String
    val disableBatteryOptimizationDesc: String
    val savingPagesHeader: String
    val defaultPageSaveDirTitle: String
    val notSet: String
    val askDestinationDirEveryTimeTitle: String
    val autoDeleteReadChaptersTitle: String
    val autoDeleteReadChaptersDesc: String
    val deleteReadChaptersNowTitle: String
    val deleteReadChaptersNowDesc: String
    val deleteReadChaptersForManga: String
    fun deleteReadChaptersConfirmMessage(count: Int, sizeStr: String): String
    fun deleteReadChaptersSuccessMessage(count: Int, sizeStr: String): String
    val noReadChaptersToDelete: String

    // NSFW & Sensitive Content Privacy strings
    val nsfwPrivacyCategoryTitle: String
    val nsfwPrivacyCategoryDesc: String
    val nsfwIncognitoModeTitle: String
    val nsfwIncognitoModeDesc: String
    val nsfwExcludeStatsTitle: String
    val nsfwExcludeStatsDesc: String
    val nsfwBlurCoversTitle: String
    val nsfwBlurCoversDesc: String
    val nsfwSeparateCategoryTitle: String
    val nsfwSeparateCategoryDesc: String
    val nsfwClearHistoryAndStats: String
    val nsfwClearHistoryAndStatsDesc: String
    val nsfwClearSuccess: String
    val nsfwIncognitoReaderBanner: String
    val nsfwBadgeText: String
    val nsfwTapToReveal: String
    val nsfwAgeConfirmTitle: String
    val nsfwAgeConfirmMessage: String
    val nsfwAgeConfirmButton: String
    val readIncognito: String
    val readFirstChapter: String
    val readLatestChapter: String
    val removeFromHistory: String
    val removedFromHistory: String
    val removeFromLibrary: String
    val changeCategory: String
    val markAllAsRead: String
    val markAllAsUnread: String

    // Manga Providers & Engines
    val mangaProvidersTitle: String
    val mangaProvidersDesc: String
    val providerKotatsuTitle: String
    val providerKotatsuDesc: String
    val providerMangaSourceTitle: String
    val providerMangaSourceDesc: String
    val providerAtLeastOneRequired: String

    fun percentCompleted(percent: Int): String
    fun timeSpentOnManga(time: String): String
    fun chaptersReadCount(read: Int, total: Int): String
    fun pagesReadCount(count: Int): String
    fun mangaWithGenreTitle(genre: String): String
    fun searchingInSource(source: String): String
    fun noResultsFor(query: String): String
    fun libraryResultsCount(count: Int): String
    fun historyResultsCount(count: Int): String
    fun chapterFormat(title: String): String
    fun pageFormat(page: Int): String
    fun categoryFormat(cat: String): String
    fun languageFormat(lang: String): String
    fun downloadNextCount(count: Int): String
}

object ArabicStrings : AppStrings {
    override val appName = "مانـغـا DT"
    override val home = "الرئيسية"
    override val library = "المكتبة"
    override val updates = "التحديثات"
    override val browse = "المصادر والتصفح"
    override val settings = "الإعدادات"
    override val favorites = "المفضلة"
    override val history = "سجل القراءة"
    override val popular = "الأكثر شعبية"
    override val latest = "أحدث الفصول"
    override val searchManga = "البحث عن مانجا أونلاين..."
    override val searchLocal = "البحث في المكتبة وسجل القراءة..."
    override val searchSources = "البحث عن مصدر مانجا..."
    override val onlineMangaMode = "مانجا أونلاين"
    override val localLibraryMode = "المكتبة والسجل"
    override val sourcesMode = "المصادر"
    override val sources = "المصدر"
    override val genre = "التصنيف"
    override val language = "اللغة"
    override val all = "الكل"
    override val continueReading = "متابعة القراءة"
    override val startReading = "بدء القراءة"
    override val resumeReading = "استئناف"
    override val inLibrary = "في المكتبة"
    override val addToLibrary = "إضافة للمكتبة"
    override val add = "إضافة"
    override val chapters = "الفصول"
    override val noMangaFound = "لا توجد مانجا متوفرة في هذا القسم"
    override val retry = "إعادة المحاولة"
    override val statusOngoing = "مستمرة"
    override val statusCompleted = "مكتملة"
    override val statusOnHold = "متوقفة"
    override val statusDropped = "ملغية"
    override val appearance = "المظهر والتصميم"
    override val colorScheme = "نظام الألوان"
    override val theme = "السمة"
    override val themeDark = "داكن"
    override val themeLight = "فاتح"
    override val themeSystem = "اتباع سمة النظام"
    override val themeSystemDesc = "التبديل بين الفاتح والداكن تلقائياً وفق إعدادات جهازك"
    override val blackAmoled = "الأسود الداكن"
    override val blackAmoledDesc = "يوفر استهلاك الطاقة لشاشات AMOLED"
    override val selectThemeTitle = "اختر السمة"
    override val darkTheme = "الوضع الداكن"
    override val nsfwContent = "عرض محتوى 18+ (NSFW)"
    override val nsfwContentDesc = "السماح بعرض والبحث في المصادر الخاصة بالبالغين"
    override val appLanguage = "لغة التطبيق"
    override val selectLanguageTitle = "اختر لغة التطبيق"
    override val clearCache = "مسح الذاكرة المؤقتة"
    override val clearCacheSuccess = "تم مسح الذاكرة المؤقتة بنجاح ✓"
    override val expandSidebar = "توسيع القائمة"
    override val collapseSidebar = "طي القائمة"
    override val selectSourceTitle = "اختر المصدر المعروض"
    override val selectGenreTitle = "اختر التصنيف"
    override val close = "إغلاق"
    override val clear = "مسح"
    override val ok = "حسناً"
    override val cancel = "إلغاء"
    override val notificationsTitle = "الإشعارات والتحديثات"
    override val noNotifications = "لا توجد إشعارات جديدة في الوقت الحالي."
    override val notificationsSubtitle = "ستظهر هنا تحديثات الفصول الجديدة لمانجا مكتبتك فور صدورها."
    override val userDefaultName = "مستخدم Manga DT"
    override val userSubtitle = "reader@mangadt.app"
    override val typeToSearchLocal = "اكتب للبحث داخل المكتبة وسجل القراءة"
    override val typeToSearchOnline = "اكتب اسم المانجا للبحث أونلاين"
    override val loadingManga = "جاري تحميل أحدث فصول وقوائم المانجا..."
    override val defaultBrowseLayout = "نمط عرض المصادر"
    override val layoutList = "عرض القائمة"
    override val layoutComfortable = "شبكة مريحة"
    override val layoutCompact = "شبكة مدمجة"
    override val selectLayoutTitle = "اختر نمط العرض"
    override val gridSizeAndLayout = "حجم الشبكة والتخطيط"
    override val gridColumnsCount = "عدد أعمدة الشبكة"
    override val gridColumnsAuto = "تلقائي ذكي (حسب الشاشة)"
    override fun gridColumnsCountLabel(count: Int) = if (count <= 0) "تلقائي ذكي" else "$count أعمدة"
    override val offlineAvailable = "متاح بدون إنترنت"
    override val savedOffline = "محفوظ للأوفلاين"
    override val defaultReadingMode = "نمط القراءة الافتراضي"
    override val readingModeWebtoon = "ويب تون (طولي مستمر)"
    override val readingModeVerticalPaged = "صفحات عمودية"
    override val readingModeLTR = "من اليسار إلى اليمين"
    override val readingModeRTL = "من اليمين إلى اليسار"
    override val selectReadingModeTitle = "اختر نمط القراءة"
    override val readerSettings = "إعدادات القارئ"
    override val readerMode = "نمط القراءة"
    override val readerBackground = "لون الخلفية"
    override val readerScale = "ملاءمة وحجم الصور"
    override val readerScaleFitWidth = "ملاءمة العرض"
    override val readerScaleFitHeight = "ملاءمة الارتفاع"
    override val readerScaleFitScreen = "ملاءمة الشاشة"
    override val readerScaleOriginal = "الحجم الأصلي"
    override val readerBgBlack = "أسود AMOLED"
    override val readerBgDarkGray = "رمادي داكن"
    override val readerBgWhite = "أبيض"
    override val previousChapter = "الفصل السابق"
    override val nextChapter = "الفصل التالي"
    override val chapterList = "قائمة الفصول"
    override val searchChapters = "البحث في الفصول..."
    override val endOfChapter = "نهاية الفصل"
    override val finishedChapter = "أتممت قراءة هذا الفصل بنجاح"
    override val goToNextChapter = "الانتقال إلى الفصل التالي"
    override val goToPreviousChapter = "العودة إلى الفصل السابق"
    override val noMoreChapters = "وصلت إلى أحدث فصل متوفر"
    override val goToPage = "الانتقال إلى صفحة"
    override val pageIndicator = "مؤشر رقم الصفحة"
    override val pageIndicatorDesc = "إظهار فقاعة رقم الصفحة والوقت أثناء القراءة"
    override val pageFailedToLoad = "فشل تحميل هذه الصفحة"
    override val tapToRetry = "اضغط لإعادة المحاولة"
    override val keepScreenOn = "إبقاء الشاشة قيد التشغيل"
    override val keepScreenOnDesc = "منع إيقاف تشغيل الشاشة تلقائياً أثناء القراءة"
    override val cropBorders = "قص الحواف والهوامش البيضاء"
    override val cropBordersDesc = "إزالة الهوامش الفارغة المحيطة بالصفحات تلقائياً"
    override val readerDoubleTapZoom = "نسبة التكبير بالنقر المزدوج"
    override val readerDoubleTapZoomDesc = "تحديد نسبة التكبير عند النقر مرتين على الشاشة (الافتراضي 1.5x)"
    override val readerPanSensitivity = "تسارع وسرعة التحريك بإصبع واحد"
    override val readerPanSensitivityDesc = "زيادة حساسية وتسارع تحريك الصفحة عند سحبها بإصبع واحد"
    override val pinnedSourcesTitle = "المصادر المثبتة"
    override val noPinnedSources = "لا توجد مصادر مثبتة حالياً"
    override val clearAllPinned = "إلغاء تثبيت الكل"
    override val versionText = "الإصدار 1.0.0 • محرك كوتاتسو"
    override val autoConfigured = "يتم ضبط هذه الإعدادات تلقائياً."
    override val storageUsageDesc = "الذاكرة المؤقتة لصور الأغلفة والفصول"
    override val captchaRequired = "يتطلب هذا المصدر إكمال التحقق البشري (Captcha / Cloudflare) للمتابعة"
    override val solveCaptcha = "حل التحقق في المتصفح المدمج (WebView)"
    override val captchaSolved = "تم التحقق بنجاح ✓"
    override val captchaHelpText = "يرجى حل اختبار التحقق البشري الظاهر في الصفحة ثم الضغط على \"تم اجتياز التحقق\""
    override val iHaveSolvedIt = "تم اجتياز التحقق"

    // Profile & Statistics (Arabic)
    override val profile = "الملف الشخصي"
    override val statistics = "الإحصائيات"
    override val profileAndStats = "الملف الشخصي والنشاط"
    override val totalReadingTime = "وقت القراءة"
    override val chaptersRead = "الفصول المقروءة"
    override val read = "مقروء"
    override val pagesRead = "الصفحات"
    override val mangaRead = "الأعمال"
    override val readingStreak = "سلسلة القراءة"
    override val streakDays = "أيام متتالية"
    override val bestStreak = "أطول سلسلة قراءة"
    override val dailyAverage = "المعدل اليومي"
    override val averageTimePerChapter = "متوسط وقت الفصل"
    override val rank = "الرتبة القرائية"
    override val rankNovice = "مبتدئ"
    override val rankReader = "قارئ نشط"
    override val rankEnthusiast = "عاشق للمانغا"
    override val rankOtaku = "خبير أوتاكو"
    override val rankLegend = "أسطورة المانغا"
    override val topGenres = "أكثر التصنيفات قراءة"
    override val weeklyActivity = "نشاط القراءة الأسبوعي"
    override val mangaStatisticsTitle = "إحصائيات كل مانغا بالتفصيل"
    override val timeSpent = "الوقت المستغرق"
    override val lastRead = "آخر قراءة"
    override val completedChapters = "الفصول المكتملة"
    override val editProfile = "تعديل الملف الشخصي"
    override val username = "اسم المستخدم"
    override val bio = "النبذة التعريفية"
    override val save = "حفظ التغييرات"
    override val privacyLocalNotice = "بياناتك ونشاطك مخزن محلياً بالكامل 100% على جهازك بترخيص مفتوح المصدر (GPL) دون أي اتصال بخوادم خارجية."
    override val noStatsYet = "لا توجد إحصائيات مسجلة بعد. ابدأ بقراءة أي مانغا ليتم تجميع نشاطك تلقائياً!"
    override val clearStats = "إعادة ضبط الإحصائيات"
    override val clearStatsConfirm = "هل أنت متأكد من رغبتك في حذف جميع إحصائيات القراءة المخزنة محلياً؟"

    // Profile Experience (Arabic)
    override val overviewAndActivity = "نظرة عامة وشخصيتي"
    override val detailedAnalytics = "التحليلات المفصلة"
    override val currentlyReading = "أقرأ حالياً"
    override val myFavorites = "أعمالي المفضلة"
    override val planToRead = "أنوي قراءتها"
    override val tasteDna = "البصمة القرائية والتصنيفات"
    override val readingVibe = "نمط ومزاج القراءة"
    override val noFavoritesYet = "لم تضف أعمالاً إلى المفضلة بعد. اضغط على أيقونة القلب في أي مانغا لإبرازها هنا!"
    override val noPlanToReadYet = "لا توجد مانجا في قائمة الانتظار. أضف أعمالاً تخطط لقراءتها لاحقاً!"
    override val noCurrentReading = "لا توجد مانجا قيد القراءة حالياً. استكشف المكتبة وابدأ مغامرتك القادمة!"

    // Local Network Sync (Arabic)
    override val localSync = "المزامنة عبر الشبكة المحلية"
    override val localSyncDesc = "مزامنة المكتبة وسجل القراءة والإحصائيات مباشرة بين الهاتف والحاسوب على نفس شبكة Wi-Fi دون إنترنت أو خوادم خارجية"
    override val myDevice = "جهازي الحالي"
    override val deviceName = "اسم الجهاز"
    override val serverStatus = "خادم المزامنة المحلي"
    override val serverRunning = "الخادم نشط وجاهز للمزامنة 🟢"
    override val serverStopped = "الخادم متوقف 🔴"
    override val localIp = "عنوان IP المحلي"
    override val scanDevices = "البحث عن أجهزة على نفس الشبكة"
    override val scanning = "جاري البحث عن أجهزة..."
    override val noDevicesFound = "لم يتم العثور على أجهزة أخرى. تأكد من تشغيل خادم المزامنة على الجهاز الآخر واتصال كلا الجهازين بنفس شبكة Wi-Fi أو نقطة الاتصال."
    override val discoveredDevices = "الأجهزة المكتشفة على الشبكة"
    override val syncNow = "مزامنة الآن"
    override val manualConnect = "الاتصال المباشر بعنوان IP"
    override val enterTargetIp = "أدخل عنوان IP للجهاز الآخر (مثال: 192.168.1.50)"
    override val connectAndSync = "اتصال ومزامنة فورية"
    override val syncOptions = "البيانات المراد مزامنتها"
    override val syncLibrary = "المكتبة، التصنيفات، والمفضلة"
    override val syncHistory = "سجل القراءة، الفصول، ورقم الصفحة"
    override val syncStats = "إحصائيات القراءة وسلاسل الأيام"
    override val syncSettings = "المصادر المثبتة وتفضيلات القراءة"
    override val syncDownloads = "نقل الفصول المحملة"
    override val syncDownloadsDesc = "نقل ملفات وصفحات الفصول المحملة مباشرة بين الأجهزة لتجنب إعادة تحميلها"
    override val syncingDownloadsProgress: (Int, Int, String) -> String = { curr, total, ch -> "جاري نقل الفصول: ($curr/$total) $ch..." }
    override val syncSuccess = "تمت المزامنة بنجاح ✓"
    override val syncFailed = "فشلت المزامنة. تأكد من صحة عنوان IP وتشغيل خادم المزامنة."
    override val syncing = "جاري تبادل ومزامنة البيانات..."
    override val lastSync = "آخر مزامنة"

    // Backup & Restore (Arabic)
    override val createBackup = "إنشاء نسخة احتياطية"
    override val createBackupDesc = "تصدير المكتبة وسجل القراءة والإحصائيات والإعدادات إلى ملف محلي"
    override val restoreBackup = "استعادة نسخة احتياطية"
    override val restoreBackupDesc = "استيراد البيانات من ملف نسخة احتياطية سابقة أو عبر نص JSON"
    override val backupCreatedSuccess = "تم إنشاء النسخة الاحتياطية بنجاح ✓"
    override val backupRestoredSuccess = "تمت استعادة النسخة الاحتياطية بنجاح ✓"
    override val backupSavedAt = "تم الحفظ في المسار:"
    override val restoreModeTitle = "اختر أسلوب الاستعادة"
    override val restoreModeMerge = "⚡ دمج ذكي مع البيانات الحالية (مستحسن)"
    override val restoreModeMergeDesc = "إضافة المانجا والفصول وساعات القراءة دون مسح ما قرأته محلياً"
    override val restoreModeOverwrite = "🔄 استبدال كامل (تصفير واستعادة مطابقة)"
    override val restoreModeOverwriteDesc = "استبدال البيانات الحالية بالكامل بمحتويات النسخة الاحتياطية"
    override val backupPreviewTitle = "معاينة النسخة الاحتياطية"
    override val backupDate = "تاريخ النسخة"
    override val backupLibraryItems = "عناصر المكتبة"
    override val backupHistoryEntries = "سجل القراءة"
    override val backupStatsEntries = "إحصائيات القراءة"
    override val localBackupsList = "النسخ الاحتياطية المحفوظة محلياً"
    override val noLocalBackupsFound = "لا توجد نسخ احتياطية محفوظة حالياً."
    override val selectFileFromDevice = "اختيار ملف من ذاكرة الجهاز"
    override val selectFileFromDeviceDesc = "تصفح الذاكرة واختيار ملف النسخة الاحتياطية (JSON) مباشرة"
    override val pasteJsonBackup = "استعادة عبر لصق نص JSON"
    override val pasteJsonPlaceholder = "الصق محتوى النسخة الاحتياطية بتنسيق JSON هنا..."
    override val invalidBackupFormat = "صيغة النسخة الاحتياطية غير صالحة. يرجى التأكد من صحة الملف."
    override val deleteBackupConfirm = "هل أنت متأكد من رغبتك في حذف هذا الملف نهائياً؟"

    // Deep Analytics (Arabic)
    override val deepAnalyticsTitle = "التحليلات والإحصائيات الدقيقة"
    override val deepAnalyticsSubtitle = "تحليل مفصل لسلوك وقراءة المانجا، السرعة، ومخطط الأوقات"
    override val readingPace = "سرعة ومعدل القراءة"
    override val secondsPerPage = "ثانية / صفحة"
    override val minutesPerChapter = "دقيقة / فصل"
    override val longestSession = "أطول جلسة قراءة"
    override val peakReadingTime = "فترة القراءة المفضلة"
    override val morningReader = "قارئ صباحي ☀️"
    override val afternoonReader = "قارئ نهاري 🌤️"
    override val nightReader = "قارئ ليلي 🌙"
    override val allMangaDetailed = "إحصائيات كل عمل بالتفصيل"
    override val sortByTime = "حسب الوقت المستغرق"
    override val sortByChapters = "حسب عدد الفصول"
    override val sortByPages = "حسب عدد الصفحات"
    override val sortByLastRead = "حسب أحدث قراءة"
    override val openDeepAnalytics = "عرض الإحصائيات والتحليلات الدقيقة ⭢"

    // Home Customization Strings (Arabic)
    override val homeCustomizationTitle = "الشاشة الرئيسية"
    override val homeCustomizationDesc = "ترتيب الأقسام، القسم الافتراضي، مظهر البطاقات، وشريط المتابعة"
    override val homeDefaultSectionTitle = "القسم الافتراضي عند الفتح"
    override val homeDefaultSectionDesc = "تحديد القسم الذي يفتح تلقائياً عند تشغيل التطبيق"
    override val homeCardsLayoutTitle = "نمط عرض البطاقات في الرئيسية"
    override val homeCardsLayoutDesc = "شبكة مريحة، شبكة مدمجة، أو قائمة مفصلة للأعمال"
    override val homeContinueReadingBarTitle = "شريط متابعة القراءة السريع"
    override val homeContinueReadingBarDesc = "إظهار شريط المانغا المقروءة مؤخراً أعلى الشاشة الرئيسية"
    override val homeContinueReadingLimitTitle = "عدد عناصر متابعة القراءة"
    override val homeQuickFilterPillsTitle = "كبسولات الفلترة السريعة"
    override val homeQuickFilterPillsDesc = "إظهار أزرار فلترة المصدر والتصنيف واللغة"
    override val homeShowFavoritesTabTitle = "تبويب المفضلة في الرئيسية"
    override val homeShowFavoritesTabDesc = "إدراج تبويب المفضلة ضمن شريط تصنيفات الرئيسية"
    override val homeCardCornersTitle = "استدارة زوايا البطاقات"
    override val homeCardCornersDesc = "التحكم في درجة انحناء حواف بطاقات المانغا"
    override val homeShowSourceBadgeTitle = "إظهار شارة اسم المصدر"
    override val homeShowSourceBadgeDesc = "عرض شارة اسم المصدر على ملصق المانغا"
    override val homeShowRatingBadgeTitle = "إظهار تقييم النجوم"
    override val homeShowRatingBadgeDesc = "عرض تقييم العمل الفعلي على البطاقة إن توفر"
    override val homeCornerSmooth = "دائري ناعم (16dp)"
    override val homeCornerMedium = "متوسط قياسي (10dp)"
    override val homeCornerSharp = "كلاسيكي حاد (4dp)"

    // Kotatsu Exact Appearance & Main Screen Settings (Arabic)
    override val defaultTabTitle = "التبويب الافتراضي"
    override val defaultTabLastUsed = "آخر استخدام"
    override val mainScreenSectionHeader = "الشاشة الرئيسية"
    override val searchSuggestionsTitle = "اقتراحات البحث"
    override val searchSuggestionsDesc = "مصادر المانجا، التصنيفات، عمليات البحث السابقة، المؤلفون، المصادر الأخيرة، المانجا"
    override val mainScreenSectionsTitle = "أقسام الشاشة الرئيسية"
    override val mainScreenSectionsDesc = "السجل، المفضلة، الاستكشاف، المستجدات، الاقتراحات، على الجهاز"
    override val showFloatingContinueBtnTitle = "إظهار زر المتابعة العائم"
    override val showFloatingContinueBtnDesc = "يتيح متابعة القراءة بنقرة واحدة. لن يظهر هذا الزر في الوضع المتخفي أو عندما يكون السجل فارغاً"
    override val showLabelsInNavBarTitle = "إظهار النصوص في شريط التنقل"
    override val floatingNavBarTitle = "شريط تنقل عائم"
    override val floatingNavBarDesc = "استخدام تصميم شريط تنقل دائري عائم"
    override val pinNavigationUiTitle = "تثبيت واجهة التنقل"
    override val pinNavigationUiDesc = "عدم إخفاء شريط التنقل ومربع البحث أثناء التمرير"
    override val exitConfirmationTitle = "تأكيد الخروج"
    override val exitConfirmationDesc = "اضغط زر العودة مرتين للخروج من التطبيق"
    override val pressBackAgainToExit = "اضغط مرة أخرى للخروج من التطبيق"
    override val exploreMoreManga = "تصفح المزيد من المانغا"
    override val browseAllManga = "عرض كل المانغا من هذا المصدر"
    override val exploreSources = "تصفح جميع المصادر"

    // Manga Details Statistics & Recommendations (Arabic)
    override val readingProgressTitle = "إحصائيات وتقدم القراءة"
    override val similarMangaTitle = "أعمال مشابهة قد تعجبك"
    override val loadingGenreManga = "جاري البحث عن أعمال مشابهة..."
    override val noMangaInGenre = "لم يتم العثور على أعمال أخرى بهذا التصنيف"
    override val readCountLabel = "تمت قراءة"

    // Download Manager Strings (Arabic)
    override val downloads = "التنزيلات"
    override val downloadChapter = "تنزيل الفصل"
    override val downloaded = "تم التنزيل"
    override val downloading = "جاري التنزيل..."
    override val downloadQueued = "في قائمة الانتظار"
    override val pauseAll = "إيقاف الكل مؤقتاً"
    override val resumeAll = "استئناف الكل"
    override val cancelAll = "إلغاء الكل"
    override val clearCompleted = "مسح المكتملة"
    override val deleteDownload = "حذف التنزيل"
    override val deleteDownloadConfirm = "هل تريد بالتأكيد حذف ملفات هذا التنزيل من جهازك؟"
    override val downloadNextChapters = "تنزيل الفصول التالية"
    override val downloadAllUnread = "تنزيل الفصول غير المقروءة"
    override val downloadAllChapters = "تنزيل جميع الفصول"
    override val storageUsed = "المساحة المستخدمة"
    override val noActiveDownloads = "لا توجد عمليات تنزيل نشطة حالياً"
    override val noDownloadedManga = "لم تقم بتنزيل أي مانجا بعد للقراءة دون اتصال"
    override val offlineReadingAvailable = "متاح للقراءة دون اتصال"
    override val downloadQueueTab = "قائمة الانتظار"
    override val downloadedMangaTab = "المانجا المحملة"
    override val batchDownloadTitle = "تنزيل متعدد للفصول"

    // Kotatsu Downloads Settings Strings (Arabic)
    override val localMangaDirectoriesTitle = "مجلدات المانغا المحلية"
    override val localMangaDirectoriesDesc = "إدارة مجلدات التخزين المحلية لقراءة المانجا بدون إنترنت"
    override val downloadsFolderTitle = "مجلد التنزيلات"
    override val internalSharedStorage = "التخزين الداخلي المشترك"
    override val preferredDownloadFormatTitle = "صيغة التنزيل المفضلة"
    override val downloadFormatAuto = "تلقائي"
    override val downloadFormatCbz = "أرشيف CBZ"
    override val downloadFormatFolder = "مجلد صور"
    override val downloadFormatZip = "أرشيف ZIP"
    override val downloadFormatPdf = "مستند PDF"
    override val downloadingOverCellularTitle = "التنزيل عبر شبكة الهاتف"
    override val cellularAllowAlways = "السماح دائماً"
    override val cellularWifiOnly = "عبر Wi-Fi فقط"
    override val cellularAskEveryTime = "السؤال في كل مرة"
    override val downloadSlowdownInfo = "يمكنك تفعيل إبطاء سرعة التنزيل لكل مصدر مانغا بشكل منفرد في إعدادات المصادر لتجنب الحظر من الخادم"
    override val disableBatteryOptimizationTitle = "تعطيل تحسين استهلاك البطارية"
    override val disableBatteryOptimizationDesc = "يساعد في استمرار التنزيل في الخلفية دون توقف العمليات بواسطة النظام"
    override val savingPagesHeader = "حفظ الصفحات"
    override val defaultPageSaveDirTitle = "مجلد حفظ الصفحات الافتراضي"
    override val notSet = "غير محدد"
    override val askDestinationDirEveryTimeTitle = "طلب تحديد المجلد في كل مرة"
    override val autoDeleteReadChaptersTitle = "حذف الفصول المقروءة تلقائياً"
    override val autoDeleteReadChaptersDesc = "حذف ملفات الفصل المحمل من الجهاز فور الانتهاء من قراءته لتوفير المساحة"
    override val deleteReadChaptersNowTitle = "حذف الفصول المقروءة الآن"
    override val deleteReadChaptersNowDesc = "فحص وحذف جميع فصول المانجا المحملة التي تمت قراءتها يدوياً"
    override val deleteReadChaptersForManga = "حذف الفصول المقروءة"
    override fun deleteReadChaptersConfirmMessage(count: Int, sizeStr: String) = "هل أنت متأكد من حذف $count فصول مقروءة محملة؟ (المساحة: $sizeStr)"
    override fun deleteReadChaptersSuccessMessage(count: Int, sizeStr: String) = "تم حذف $count فصول مقروءة بنجاح وتوفير $sizeStr"
    override val noReadChaptersToDelete = "لا توجد فصول مقروءة محملة لحذفها حالياً."

    // NSFW & Sensitive Content Privacy strings (Arabic)
    override val nsfwPrivacyCategoryTitle = "الخصوصية والمحتوى الحساس"
    override val nsfwPrivacyCategoryDesc = "التصفح المتخفي، استبعاد الإحصائيات، حجب الأغلفة"
    override val nsfwIncognitoModeTitle = "التصفح المتخفي للمحتوى الحساس"
    override val nsfwIncognitoModeDesc = "عدم حفظ المانجا الحساسة في سجل القراءة أو شريط المتابعة إطلاقاً"
    override val nsfwExcludeStatsTitle = "استبعاد من إحصائيات القراءة"
    override val nsfwExcludeStatsDesc = "عدم احتساب وقت القراءة أو التصنيفات الحساسة ضمن ملف الإنجازات والإحصائيات"
    override val nsfwBlurCoversTitle = "تمويه وحجب الأغلفة الحساسة"
    override val nsfwBlurCoversDesc = "حجب وتمويه صور وأغلفة المانجا الحساسة في القوائم والبحث والمكتبة"
    override val nsfwSeparateCategoryTitle = "عزل في قسم خاص للخصوصية"
    override val nsfwSeparateCategoryDesc = "فصل المانجا الحساسة في تصنيف مكتبة خاص لتجنب الظهور في القوائم العامة"
    override val nsfwClearHistoryAndStats = "مسح فوري لسجلات المحتوى الحساس"
    override val nsfwClearHistoryAndStatsDesc = "حذف أي سجلات أو إحصائيات سابقة متعلقة بالمانجا الحساسة بنقرة واحدة"
    override val nsfwClearSuccess = "تم مسح جميع سجلات وإحصائيات المحتوى الحساس بنجاح"
    override val nsfwIncognitoReaderBanner = "وضع التصفح المتخفي نشط (لن يتم حفظ السجل أو الإحصائيات)"
    override val nsfwBadgeText = "18+ حساس"
    override val nsfwTapToReveal = "انقر لإظهار الغلاف"
    override val nsfwAgeConfirmTitle = "تأكيد العمر للمحتوى الحساس"
    override val nsfwAgeConfirmMessage = "يتضمن هذا القسم أعمالاً موجهة للبالغين فقط (+18). هل تؤكد أن عمرك يتجاوز 18 عاماً وترغب في تمكين هذا المحتوى؟"
    override val nsfwAgeConfirmButton = "أؤكد، أنا أكبر من 18 عاماً"
    override val readIncognito = "قراءة متخفية (بدون حفظ السجل)"
    override val readFirstChapter = "قراءة من الفصل الأول"
    override val readLatestChapter = "قراءة أحدث فصل"
    override val removeFromHistory = "إزالة من سجل القراءة"
    override val removedFromHistory = "تمت الإزالة من السجل"
    override val removeFromLibrary = "إزالة من المفضلة / المكتبة"
    override val changeCategory = "تغيير تصنيف المكتبة"
    override val markAllAsRead = "تحديد جميع الفصول كمقروءة"
    override val markAllAsUnread = "تحديد جميع الفصول كغير مقروءة"

    override val mangaProvidersTitle = "مزودات ومحركات المانجا"
    override val mangaProvidersDesc = "إدارة وتفعيل محركات ومصادر المانجا في التطبيق"
    override val providerKotatsuTitle = "محرك كوتاتسو (Kotatsu Parsers Redo)"
    override val providerKotatsuDesc = "مكتبة كوتاتسو الشاملة التي تضم أكثر من 1000 مصدر مانجا عالمي"
    override val providerMangaSourceTitle = "محرك مانجا سورس (Manga-Source Engine)"
    override val providerMangaSourceDesc = "محرك فائق السرعة مع مصادر عربية وعالمية مخصصة (العاشق، مانجا ليك، مانجا ليكو، مانجابيل، لايك مانجا...)"
    override val providerAtLeastOneRequired = "يجب إبقاء مزود واحد على الأقل مفعلاً لتمكين تصفح المانجا"

    override fun percentCompleted(percent: Int) = "$percent% مكتمل"
    override fun timeSpentOnManga(time: String) = "الوقت: $time"
    override fun chaptersReadCount(read: Int, total: Int) = "$read من أصل $total فصول"
    override fun pagesReadCount(count: Int) = "$count صفحة"
    override fun mangaWithGenreTitle(genre: String) = "مانجا بتصنيف \"$genre\""
    override fun searchingInSource(source: String) = "جاري البحث في $source..."
    override fun noResultsFor(query: String) = "لا توجد نتائج مطابقة لـ \"$query\""
    override fun libraryResultsCount(count: Int) = "المكتبة ($count)"
    override fun historyResultsCount(count: Int) = "سجل القراءة ($count)"
    override fun chapterFormat(title: String) = "فصل $title"
    override fun pageFormat(page: Int) = "صفحة $page"
    override fun categoryFormat(cat: String) = "التصنيف: $cat"
    override fun languageFormat(lang: String) = "اللغة: $lang"
    override fun downloadNextCount(count: Int) = "تنزيل $count فصول تالية"
}

object EnglishStrings : AppStrings {
    override val appName = "Manga DT"
    override val home = "Home"
    override val library = "Library"
    override val updates = "Updates"
    override val browse = "Browse"
    override val settings = "Settings"
    override val favorites = "Favorites"
    override val history = "History"
    override val popular = "Popular"
    override val latest = "Latest"
    override val searchManga = "Search manga online..."
    override val searchLocal = "Search library & history..."
    override val searchSources = "Search sources..."
    override val onlineMangaMode = "Online Manga"
    override val localLibraryMode = "Library & History"
    override val sourcesMode = "Sources"
    override val sources = "Source"
    override val genre = "Genre"
    override val language = "Language"
    override val all = "All"
    override val continueReading = "Continue Reading"
    override val startReading = "Start Reading"
    override val resumeReading = "Resume"
    override val inLibrary = "In Library"
    override val addToLibrary = "Add to Library"
    override val add = "Add"
    override val chapters = "Chapters"
    override val noMangaFound = "No manga available in this section"
    override val retry = "Retry"
    override val statusOngoing = "Ongoing"
    override val statusCompleted = "Completed"
    override val statusOnHold = "On Hold"
    override val statusDropped = "Dropped"
    override val appearance = "Appearance"
    override val colorScheme = "Color scheme"
    override val theme = "Theme"
    override val themeDark = "Dark"
    override val themeLight = "Light"
    override val themeSystem = "Follow System Theme"
    override val themeSystemDesc = "Switch between light and dark based on your device settings"
    override val blackAmoled = "Black"
    override val blackAmoledDesc = "Uses less power on AMOLED screens"
    override val selectThemeTitle = "Select Theme"
    override val darkTheme = "Dark Theme"
    override val nsfwContent = "Show 18+ (NSFW) Content"
    override val nsfwContentDesc = "Allow viewing and searching adult sources"
    override val appLanguage = "App Language"
    override val selectLanguageTitle = "Select App Language"
    override val clearCache = "Clear Cache"
    override val clearCacheSuccess = "Cache Cleared Successfully ✓"
    override val expandSidebar = "Expand Sidebar"
    override val collapseSidebar = "Collapse Sidebar"
    override val selectSourceTitle = "Select Displayed Source"
    override val selectGenreTitle = "Select Genre"
    override val close = "Close"
    override val clear = "Clear"
    override val ok = "OK"
    override val cancel = "Cancel"
    override val notificationsTitle = "Notifications & Updates"
    override val noNotifications = "No new notifications at this time."
    override val notificationsSubtitle = "New chapter updates for your library manga will appear here."
    override val userDefaultName = "Manga DT User"
    override val userSubtitle = "reader@mangadt.app"
    override val typeToSearchLocal = "Type to search inside library & history"
    override val typeToSearchOnline = "Type manga name to search online"
    override val loadingManga = "Loading latest manga and chapters..."
    override val defaultBrowseLayout = "Default Browse Layout"
    override val layoutList = "List View"
    override val layoutComfortable = "Comfortable Grid"
    override val layoutCompact = "Compact Grid"
    override val selectLayoutTitle = "Select Layout Mode"
    override val gridSizeAndLayout = "Grid Size & Layout"
    override val gridColumnsCount = "Grid Columns"
    override val gridColumnsAuto = "Smart Auto"
    override fun gridColumnsCountLabel(count: Int) = if (count <= 0) "Smart Auto" else "$count Columns"
    override val offlineAvailable = "Available Offline"
    override val savedOffline = "Saved for Offline"
    override val defaultReadingMode = "Default Reading Mode"
    override val readingModeWebtoon = "Webtoon"
    override val readingModeVerticalPaged = "Vertical Paged"
    override val readingModeLTR = "Left to Right"
    override val readingModeRTL = "Right to Left"
    override val selectReadingModeTitle = "Select Reading Mode"
    override val readerSettings = "Reader Settings"
    override val readerMode = "Reading Mode"
    override val readerBackground = "Background Color"
    override val readerScale = "Image Scaling"
    override val readerScaleFitWidth = "Fit Width"
    override val readerScaleFitHeight = "Fit Height"
    override val readerScaleFitScreen = "Fit Screen"
    override val readerScaleOriginal = "Original Size"
    override val readerBgBlack = "AMOLED Black"
    override val readerBgDarkGray = "Dark Gray"
    override val readerBgWhite = "White"
    override val previousChapter = "Previous Chapter"
    override val nextChapter = "Next Chapter"
    override val chapterList = "Chapter List"
    override val searchChapters = "Search chapters..."
    override val endOfChapter = "End of Chapter"
    override val finishedChapter = "You have completed this chapter"
    override val goToNextChapter = "Go to Next Chapter"
    override val goToPreviousChapter = "Go to Previous Chapter"
    override val noMoreChapters = "You are on the latest chapter"
    override val goToPage = "Go to Page"
    override val pageIndicator = "Page Number Indicator"
    override val pageIndicatorDesc = "Show floating page bubble and clock while reading"
    override val pageFailedToLoad = "Page failed to load"
    override val tapToRetry = "Tap to retry"
    override val keepScreenOn = "Keep Screen On"
    override val keepScreenOnDesc = "Prevent device screen from sleeping while reading"
    override val cropBorders = "Crop Image Borders"
    override val cropBordersDesc = "Automatically trim empty white borders around pages"
    override val readerDoubleTapZoom = "Double-Tap Zoom Scale"
    override val readerDoubleTapZoomDesc = "Zoom level when double-tapping the screen (default 1.5x)"
    override val readerPanSensitivity = "1-Finger Pan Sensitivity & Acceleration"
    override val readerPanSensitivityDesc = "Increase panning speed and velocity acceleration when dragging with one finger"
    override val pinnedSourcesTitle = "Pinned Sources"
    override val noPinnedSources = "No sources pinned yet"
    override val clearAllPinned = "Clear All Pinned"
    override val versionText = "Version 1.0.0 • Kotatsu Multiplatform"
    override val autoConfigured = "These settings are configured automatically."
    override val storageUsageDesc = "Covers and chapters disk cache"
    override val captchaRequired = "This source requires human verification (Captcha / Cloudflare) to proceed"
    override val solveCaptcha = "Solve in Embedded WebView"
    override val captchaSolved = "Verification Solved Successfully ✓"
    override val captchaHelpText = "Please solve the human verification test on the page, then click \"I have solved it\""
    override val iHaveSolvedIt = "I have solved it"

    // Profile & Statistics (English)
    override val profile = "Profile"
    override val statistics = "Statistics"
    override val profileAndStats = "Profile & Activity"
    override val totalReadingTime = "Reading Time"
    override val chaptersRead = "Chapters Read"
    override val read = "Read"
    override val pagesRead = "Pages"
    override val mangaRead = "Titles"
    override val readingStreak = "Reading Streak"
    override val streakDays = "Days Streak"
    override val bestStreak = "Best Streak"
    override val dailyAverage = "Daily Average"
    override val averageTimePerChapter = "Avg Time / Chapter"
    override val rank = "Reader Rank"
    override val rankNovice = "Novice"
    override val rankReader = "Active Reader"
    override val rankEnthusiast = "Manga Enthusiast"
    override val rankOtaku = "Otaku Master"
    override val rankLegend = "Manga Legend"
    override val topGenres = "Top Genres"
    override val weeklyActivity = "Weekly Activity"
    override val mangaStatisticsTitle = "Per-Manga Statistics"
    override val timeSpent = "Time Spent"
    override val lastRead = "Last Read"
    override val completedChapters = "Completed Chapters"
    override val editProfile = "Edit Profile"
    override val username = "Username"
    override val bio = "Bio"
    override val save = "Save Changes"
    override val privacyLocalNotice = "100% Offline & Local. Your activity and stats are kept strictly on your device under GPL open-source license with zero external servers."
    override val noStatsYet = "No reading data recorded yet. Start reading any manga to automatically track your activity!"
    override val clearStats = "Reset Statistics"
    override val clearStatsConfirm = "Are you sure you want to delete all locally stored reading statistics?"

    // Profile Experience (English)
    override val overviewAndActivity = "Overview & Taste"
    override val detailedAnalytics = "Deep Analytics"
    override val currentlyReading = "Currently Reading"
    override val myFavorites = "My Favorites"
    override val planToRead = "Plan to Read"
    override val tasteDna = "Taste DNA & Genres"
    override val readingVibe = "Reading Vibe"
    override val noFavoritesYet = "No favorites added yet. Tap the heart on any manga to showcase it here!"
    override val noPlanToReadYet = "No manga in your reading queue. Add titles you plan to read later!"
    override val noCurrentReading = "No manga currently being read. Discover titles and start your next adventure!"

    // Local Network Sync (English)
    override val localSync = "Local Network Sync"
    override val localSyncDesc = "Sync library, history and stats directly between phone and PC on the same Wi-Fi with zero external servers"
    override val myDevice = "This Device"
    override val deviceName = "Device Name"
    override val serverStatus = "Local Sync Server"
    override val serverRunning = "Server Running & Ready 🟢"
    override val serverStopped = "Server Stopped 🔴"
    override val localIp = "Local IP Address"
    override val scanDevices = "Scan for Nearby Devices"
    override val scanning = "Scanning for devices..."
    override val noDevicesFound = "No other devices found. Ensure sync server is running on the other device and both are on the same Wi-Fi or hotspot."
    override val discoveredDevices = "Discovered Devices on LAN"
    override val syncNow = "Sync Now"
    override val manualConnect = "Direct IP Connection"
    override val enterTargetIp = "Enter target device IP (e.g. 192.168.1.50)"
    override val connectAndSync = "Connect & Sync Now"
    override val syncOptions = "Items to Synchronize"
    override val syncLibrary = "Library, Categories & Favorites"
    override val syncHistory = "Reading History & Chapter Progress"
    override val syncStats = "Reading Stats & Streaks"
    override val syncSettings = "Pinned Sources & Preferences"
    override val syncDownloads = "Transfer Downloaded Chapters"
    override val syncDownloadsDesc = "Directly transfer downloaded chapter files and pages between devices to avoid re-downloading"
    override val syncingDownloadsProgress: (Int, Int, String) -> String = { curr, total, ch -> "Transferring chapters: ($curr/$total) $ch..." }
    override val syncSuccess = "Sync Completed Successfully ✓"
    override val syncFailed = "Sync failed. Check target IP and ensure sync server is enabled."
    override val syncing = "Transferring and merging data..."
    override val lastSync = "Last Synced"

    // Backup & Restore (English)
    override val createBackup = "Create Backup"
    override val createBackupDesc = "Export library, reading history, statistics, and settings to a local file"
    override val restoreBackup = "Restore Backup"
    override val restoreBackupDesc = "Import data from a previous backup file or via JSON text"
    override val backupCreatedSuccess = "Backup Created Successfully ✓"
    override val backupRestoredSuccess = "Backup Restored Successfully ✓"
    override val backupSavedAt = "Saved at:"
    override val restoreModeTitle = "Select Restore Mode"
    override val restoreModeMerge = "⚡ Smart Merge with Current Data (Recommended)"
    override val restoreModeMergeDesc = "Add new manga, chapters, and stats without deleting local progress"
    override val restoreModeOverwrite = "🔄 Full Overwrite (Replace All)"
    override val restoreModeOverwriteDesc = "Completely replace current local data with the backup contents"
    override val backupPreviewTitle = "Backup Preview"
    override val backupDate = "Backup Date"
    override val backupLibraryItems = "Library Items"
    override val backupHistoryEntries = "Reading History"
    override val backupStatsEntries = "Reading Statistics"
    override val localBackupsList = "Locally Saved Backups"
    override val noLocalBackupsFound = "No backup files saved yet."
    override val selectFileFromDevice = "Select Backup File from Device"
    override val selectFileFromDeviceDesc = "Browse device storage and select a JSON backup file directly"
    override val pasteJsonBackup = "Restore via Paste JSON Text"
    override val pasteJsonPlaceholder = "Paste backup JSON content here..."
    override val invalidBackupFormat = "Invalid backup format. Please verify the file content."
    override val deleteBackupConfirm = "Are you sure you want to delete this backup file?"

    // Deep Analytics (English)
    override val deepAnalyticsTitle = "Deep Analytics"
    override val deepAnalyticsSubtitle = "Comprehensive breakdown of reading speed, completed chapters, and hours"
    override val readingPace = "Reading Pace"
    override val secondsPerPage = "sec / page"
    override val minutesPerChapter = "min / chapter"
    override val longestSession = "Longest Reading Session"
    override val peakReadingTime = "Peak Reading Time"
    override val morningReader = "Morning Reader ☀️"
    override val afternoonReader = "Afternoon Reader 🌤️"
    override val nightReader = "Night Owl 🌙"
    override val allMangaDetailed = "All Manga Detailed Stats"
    override val sortByTime = "By Time Spent"
    override val sortByChapters = "By Chapters"
    override val sortByPages = "By Pages"
    override val sortByLastRead = "By Last Read"
    override val openDeepAnalytics = "View Deep Analytics ⭢"

    // Home Customization Strings (English)
    override val homeCustomizationTitle = "Home Screen"
    override val homeCustomizationDesc = "Sections priority, default category, card style & continue reading"
    override val homeDefaultSectionTitle = "Default Section on Launch"
    override val homeDefaultSectionDesc = "Select which category opens automatically when opening the app"
    override val homeCardsLayoutTitle = "Home Cards Layout"
    override val homeCardsLayoutDesc = "Comfortable Grid, Compact Grid, or Detailed List"
    override val homeContinueReadingBarTitle = "Continue Reading Shortcuts"
    override val homeContinueReadingBarDesc = "Show recently read manga shortcut carousel at the top of Home"
    override val homeContinueReadingLimitTitle = "Continue Reading Item Limit"
    override val homeQuickFilterPillsTitle = "Quick Filter Pills"
    override val homeQuickFilterPillsDesc = "Show Source, Genre, and Language filter buttons"
    override val homeShowFavoritesTabTitle = "Favorites Tab in Home"
    override val homeShowFavoritesTabDesc = "Include Favorites in the top category bar"
    override val homeCardCornersTitle = "Card Corner Curvature"
    override val homeCardCornersDesc = "Control roundness of manga card corners"
    override val homeShowSourceBadgeTitle = "Show Source Badge"
    override val homeShowSourceBadgeDesc = "Display source name chip on manga posters"
    override val homeShowRatingBadgeTitle = "Show Rating Stars"
    override val homeShowRatingBadgeDesc = "Display star rating chip on cards if available"
    override val homeCornerSmooth = "Smooth Rounded (16dp)"
    override val homeCornerMedium = "Standard (10dp)"
    override val homeCornerSharp = "Classic Sharp (4dp)"

    // Kotatsu Exact Appearance & Main Screen Settings (English)
    override val defaultTabTitle = "Default tab"
    override val defaultTabLastUsed = "Last used"
    override val mainScreenSectionHeader = "Main screen"
    override val searchSuggestionsTitle = "Search suggestions"
    override val searchSuggestionsDesc = "Manga sources, Genres, Recent queries, Authors, Suggested queries, Recent sources, Manga"
    override val mainScreenSectionsTitle = "Main screen sections"
    override val mainScreenSectionsDesc = "History, Favourites, Explore, Feed, Suggestions, On device"
    override val showFloatingContinueBtnTitle = "Show floating Continue button"
    override val showFloatingContinueBtnDesc = "Allows to continue reading in a one click. This button will not appear in incognito mode or when the history is empty"
    override val showLabelsInNavBarTitle = "Show labels in navigation bar"
    override val floatingNavBarTitle = "Floating navigation bar"
    override val floatingNavBarDesc = "Use a rounded floating navigation bar style"
    override val pinNavigationUiTitle = "Pin navigation UI"
    override val pinNavigationUiDesc = "Do not hide navigation bar and search view on scroll"
    override val exitConfirmationTitle = "Exit confirmation"
    override val exitConfirmationDesc = "Press Back twice to exit the app"
    override val pressBackAgainToExit = "Press back again to exit"
    override val exploreMoreManga = "Browse More Manga"
    override val browseAllManga = "View All Manga from this Source"
    override val exploreSources = "Browse All Sources"

    // Manga Details Statistics & Recommendations (English)
    override val readingProgressTitle = "Reading Progress"
    override val similarMangaTitle = "Similar Manga & Recommendations"
    override val loadingGenreManga = "Finding related manga..."
    override val noMangaInGenre = "No other manga found with this tag"
    override val readCountLabel = "Read"

    // Download Manager Strings (English)
    override val downloads = "Downloads"
    override val downloadChapter = "Download Chapter"
    override val downloaded = "Downloaded"
    override val downloading = "Downloading..."
    override val downloadQueued = "Queued"
    override val pauseAll = "Pause All"
    override val resumeAll = "Resume All"
    override val cancelAll = "Cancel All"
    override val clearCompleted = "Clear Completed"
    override val deleteDownload = "Delete Download"
    override val deleteDownloadConfirm = "Are you sure you want to delete these download files from your device?"
    override val downloadNextChapters = "Download Next Chapters"
    override val downloadAllUnread = "Download All Unread"
    override val downloadAllChapters = "Download All Chapters"
    override val storageUsed = "Storage Used"
    override val noActiveDownloads = "No active downloads currently"
    override val noDownloadedManga = "You haven't downloaded any manga for offline reading yet"
    override val offlineReadingAvailable = "Available for offline reading"
    override val downloadQueueTab = "Queue"
    override val downloadedMangaTab = "Downloaded Manga"
    override val batchDownloadTitle = "Batch Download Chapters"

    // Kotatsu Downloads Settings Strings (English)
    override val localMangaDirectoriesTitle = "Local manga directories"
    override val localMangaDirectoriesDesc = "Manage local folders to read manga offline"
    override val downloadsFolderTitle = "Downloads folder"
    override val internalSharedStorage = "Internal shared storage"
    override val preferredDownloadFormatTitle = "Preferred download format"
    override val downloadFormatAuto = "Automatic"
    override val downloadFormatCbz = "CBZ Archive"
    override val downloadFormatFolder = "Folder with Images"
    override val downloadFormatZip = "ZIP Archive"
    override val downloadFormatPdf = "PDF Document"
    override val downloadingOverCellularTitle = "Downloading over cellular network"
    override val cellularAllowAlways = "Allow always"
    override val cellularWifiOnly = "Wi-Fi only"
    override val cellularAskEveryTime = "Ask every time"
    override val downloadSlowdownInfo = "You can enable download slowdown for each manga source individually in the source settings if you are having problems with server-side blocking"
    override val disableBatteryOptimizationTitle = "Disable battery optimization"
    override val disableBatteryOptimizationDesc = "Might help with getting the download started if you have any issues with it"
    override val savingPagesHeader = "Saving pages"
    override val defaultPageSaveDirTitle = "Default page save directory"
    override val notSet = "Not set"
    override val askDestinationDirEveryTimeTitle = "Ask for the destination dir every time"
    override val autoDeleteReadChaptersTitle = "Auto-Delete Read Chapters"
    override val autoDeleteReadChaptersDesc = "Automatically delete downloaded chapter files upon completion to save disk space"
    override val deleteReadChaptersNowTitle = "Delete Read Chapters Now"
    override val deleteReadChaptersNowDesc = "Scan and manually purge all downloaded manga chapters that have been read"
    override val deleteReadChaptersForManga = "Delete Read Chapters"
    override fun deleteReadChaptersConfirmMessage(count: Int, sizeStr: String) = "Are you sure you want to delete $count read downloaded chapters? (Size: $sizeStr)"
    override fun deleteReadChaptersSuccessMessage(count: Int, sizeStr: String) = "Successfully deleted $count read chapters and freed $sizeStr"
    override val noReadChaptersToDelete = "No read downloaded chapters found to delete."

    // NSFW & Sensitive Content Privacy strings (English)
    override val nsfwPrivacyCategoryTitle = "Privacy & 18+ Content"
    override val nsfwPrivacyCategoryDesc = "Incognito mode, Exclude from stats, Blur covers"
    override val nsfwIncognitoModeTitle = "Incognito Mode for 18+ Content"
    override val nsfwIncognitoModeDesc = "Never save sensitive manga or chapters to reading history or quick continue bar"
    override val nsfwExcludeStatsTitle = "Exclude from Reading Statistics"
    override val nsfwExcludeStatsDesc = "Do not track reading duration, chapters, or sensitive genres in profile stats"
    override val nsfwBlurCoversTitle = "Blur & Shield Sensitive Covers"
    override val nsfwBlurCoversDesc = "Censor and blur NSFW covers across browse, search, and library"
    override val nsfwSeparateCategoryTitle = "Isolate in Private Library Category"
    override val nsfwSeparateCategoryDesc = "Separate adult manga into a distinct private category away from main tabs"
    override val nsfwClearHistoryAndStats = "Clear All 18+ History & Stats"
    override val nsfwClearHistoryAndStatsDesc = "Instantly purge any past traces of adult manga from history and analytics"
    override val nsfwClearSuccess = "All sensitive history and statistics have been cleared"
    override val nsfwIncognitoReaderBanner = "Incognito Mode Active (History & Stats tracking disabled)"
    override val nsfwBadgeText = "18+ NSFW"
    override val nsfwTapToReveal = "Tap to reveal cover"
    override val nsfwAgeConfirmTitle = "Age Verification"
    override val nsfwAgeConfirmMessage = "This section contains content intended for adults (+18). Do you confirm you are over 18 years of age?"
    override val nsfwAgeConfirmButton = "I confirm, I am 18+"
    override val readIncognito = "Incognito Reading (No history)"
    override val readFirstChapter = "Read from First Chapter"
    override val readLatestChapter = "Read Latest Chapter"
    override val removeFromHistory = "Remove from History"
    override val removedFromHistory = "Removed from History"
    override val removeFromLibrary = "Remove from Library"
    override val changeCategory = "Change Library Category"
    override val markAllAsRead = "Mark All Chapters as Read"
    override val markAllAsUnread = "Mark All Chapters as Unread"

    override val mangaProvidersTitle = "Manga Providers & Engines"
    override val mangaProvidersDesc = "Manage enabled manga catalog sources and parsing engines"
    override val providerKotatsuTitle = "Kotatsu Parsers (Redo)"
    override val providerKotatsuDesc = "Comprehensive global catalog featuring 1000+ manga sources"
    override val providerMangaSourceTitle = "Manga-Source Engine"
    override val providerMangaSourceDesc = "Fast engine with specialized Arabic & English sources (3asq, Manga-Lek, MangaLeko, Mangapill, LikeManga...)"
    override val providerAtLeastOneRequired = "At least one provider must remain enabled to browse manga"

    override fun percentCompleted(percent: Int) = "$percent% completed"
    override fun timeSpentOnManga(time: String) = "Time: $time"
    override fun chaptersReadCount(read: Int, total: Int) = "$read of $total chapters"
    override fun pagesReadCount(count: Int) = "$count pages"
    override fun mangaWithGenreTitle(genre: String) = "Manga in \"$genre\""
    override fun searchingInSource(source: String) = "Searching in $source..."
    override fun noResultsFor(query: String) = "No results found for \"$query\""
    override fun libraryResultsCount(count: Int) = "Library ($count)"
    override fun historyResultsCount(count: Int) = "History ($count)"
    override fun chapterFormat(title: String) = "Ch. $title"
    override fun pageFormat(page: Int) = "Page $page"
    override fun categoryFormat(cat: String) = "Category: $cat"
    override fun languageFormat(lang: String) = "Language: $lang"
    override fun downloadNextCount(count: Int) = "Download Next $count Chapters"
}
